package algorithms.hybrid;

import algorithms.ISolver;
import com.google.common.collect.Iterables;
import common.Configuration;
import common.DLSyntax;
import common.Printer;
import fileLogger.FileLogger;
import models.Abducibles;
import models.Explanation;
import models.Literals;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.PelletReasonerFactory;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import reasoner.AxiomManager;
import reasoner.ILoader;
import reasoner.IReasonerManager;
import timer.ThreadTimes;
import org.semanticweb.owlapi.reasoner.Node;

import java.beans.Expression;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

/**
 * Base = knowledgeBase + negObservation
 * Literals = set of all literals / concepts with named individual except observation
 */

public class HybridSolver implements ISolver {

    private ILoader loader;
    private IReasonerManager reasonerManager;
    private OWLOntology ontology;

    private List<ModelNode> models;
    private List<ModelNode> negModels;
    private List<OWLAxiom> assertionsAxioms;
    private List<OWLAxiom> negAssertionsAxioms;
    private Literals literals;
    private Literals abd_literals;
    private List<Explanation> explanations;
    private List<Explanation> minimalExplanations;
    private List<OWLAxiom> path;
    private List<OWLAxiom> pathDuringCheckingMinimality;
    private Abducibles abducibles;

    List<OWLAxiom> lenghtOneExplanations = new ArrayList<>();

    private int lastUsableModelIndex;
    private OWLAxiom negObservation;

    private ThreadTimes threadTimes;
    private long currentTimeMillis;

    private boolean REUSE_OF_MODELS = true;
    private boolean GET_MODELS_BY_REASONER = false;
    private boolean CHECKING_MINIMALITY_BY_QXP = true;

    private Map<Integer, Double> level_times = new HashMap<>();

    private boolean checkingMinimalityWithQXP = false;

    public HybridSolver(ThreadTimes threadTimes, long currentTimeMillis) {
        this.threadTimes = threadTimes;
        this.currentTimeMillis = currentTimeMillis;
    }

    @Override
    public void solve(ILoader loader, IReasonerManager reasonerManager) throws OWLOntologyStorageException {
        this.loader = loader;
        this.reasonerManager = reasonerManager;
        this.ontology = this.loader.getOriginalOntology();
        negObservation = loader.getNegObservation().getOwlAxiom();
        this.abducibles = loader.getAbducibles();
        if (!reasonerManager.isOntologyConsistent())
            return;
        initialize();
        if (reasonerManager.isOntologyWithLiteralsConsistent(literals.getOwlAxioms(), ontology))
            return;
        startSolving();
        showExplanations();
    }

    private void initialize() {
        models = new ArrayList<>();
        negModels = new ArrayList<>();

        assertionsAxioms = new ArrayList<>();
        negAssertionsAxioms = new ArrayList<>();

        loader.getOntologyManager().addAxiom(ontology, loader.getNegObservation().getOwlAxiom());
        reasonerManager.addAxiomToOntology(loader.getNegObservation().getOwlAxiom());

        loader.getOntology().axioms(AxiomType.DECLARATION).forEach(axiom -> {
            /**SKONTROLOVAT CO ROBI PREMENNA preserveObservation**/
            List<OWLAxiom> classAssertionAxiom = AxiomManager.createClassAssertionAxiom(loader, axiom, true);
            List<OWLAxiom> objectPropertyAssertionAxiom = AxiomManager.createObjectPropertyAssertionAxiom(loader, axiom);

            for (int i = 0; i < classAssertionAxiom.size(); i++) {
                if (i % 2 == 0) {
                    assertionsAxioms.add(classAssertionAxiom.get(i));
                } else {
                    negAssertionsAxioms.add(classAssertionAxiom.get(i));
                }
            }
            for (int i = 0; i < objectPropertyAssertionAxiom.size(); i++) {
                if (i % 2 == 0) {
                    assertionsAxioms.add(objectPropertyAssertionAxiom.get(i));
                } else {
                    negAssertionsAxioms.add(objectPropertyAssertionAxiom.get(i));
                }
            }
        });

        Set<OWLAxiom> allLiterals = new HashSet<>();
        allLiterals.addAll(assertionsAxioms);
        allLiterals.addAll(negAssertionsAxioms);

        if (loader.isMultipleObservationOnInput()){
            allLiterals.remove(loader.getObservation().getOwlAxiom());
            for (int i = 0; i < loader.getMultipleObservations().size(); i++){
                allLiterals.remove(loader.getMultipleObservations().get(i));
            }
        } else {
            allLiterals.remove(loader.getObservation().getOwlAxiom());
        }

        literals = new Literals(allLiterals);
        Set<OWLAxiom> to_abd = new HashSet<>();
        if (Configuration.NEGATION_ALLOWED){
            for (OWLAxiom ax : allLiterals){
                if (abducibles.getIndividuals().containsAll(ax.getIndividualsInSignature()) &&
                        abducibles.getClasses().containsAll(ax.getClassesInSignature())){
                    to_abd.add(ax);
                }
            }
        }
        else {
            List<OWLAxiom> temp = new ArrayList<>(assertionsAxioms);
            if(loader.isMultipleObservationOnInput()){
                temp.removeAll(loader.getMultipleObservations());
            }

            //for (OWLAxiom ax : assertionsAxioms){
            for (OWLAxiom ax : temp){
                if (ax.equals(loader.getObservation().getOwlAxiom())){
                    continue;
                }
                if (abducibles.getIndividuals().containsAll(ax.getIndividualsInSignature()) &&
                        abducibles.getClasses().containsAll(ax.getClassesInSignature())){
                    to_abd.add(ax);
                }
            }
        }

        /**CO TO PRESNE JE???? preco ak je to prazdne tak sa tam naplnaju veci - AK NIE SU URCENE ABDUCIBLES????**/
        abd_literals = new Literals(to_abd);
        if (abd_literals.getOwlAxioms().isEmpty()){
            if (Configuration.NEGATION_ALLOWED){
                abd_literals = literals;
            }
            else{
                List<OWLAxiom> temp1 = new ArrayList<>(assertionsAxioms);
                if(loader.isMultipleObservationOnInput()){
                    temp1.removeAll(loader.getMultipleObservations());
                }
                //abd_literals.addLiterals(assertionsAxioms);
                abd_literals.addLiterals(temp1);
                abd_literals.removeLiteral(loader.getObservation().getOwlAxiom());
            }
        }
        System.out.println("abd literals");
        System.out.println(abd_literals.getOwlAxioms());
    }

    private void startSolving() throws OWLOntologyStorageException {
        explanations = new LinkedList<>();
        path = new ArrayList<>();
        ICheckRules checkRules = new CheckRules(loader, reasonerManager);
        Integer currentDepth = 0;

        //path = new ArrayList<>();

        Conflict conflict = getMergeConflict();
        for (Explanation e: conflict.getExplanations()){
            e.setDepth(e.getOwlAxioms().size());
        }
        explanations = conflict.getExplanations();

        ModelNode root = new ModelNode();
        if (usableModelInModels()){
            root.data = negModels.get(lastUsableModelIndex).data;
            root.label = new LinkedList<>();
            root.depth = 0;
        }
        if (root.data == null || root.data.isEmpty()){
            return;
        }

        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();

            if (node.depth > currentDepth){
                showExplanationsWithDepth(currentDepth, false);
                currentDepth++;
            }

            if (Configuration.TIMEOUT != null && threadTimes.getTotalUserTimeInSec() > Configuration.TIMEOUT) {
                System.out.println("timeout");
                showExplanationsWithDepth(currentDepth + 1, true);
                currentDepth = null;
                break;
            }

            if (ModelNode.class.isAssignableFrom(node.getClass())) {
                ModelNode model = (ModelNode) node;
                /*System.out.println("Model data");
                System.out.println(model.data);
                System.out.println("Model label");
                System.out.println(model.label);*/
                if (model.depth.equals(Configuration.DEPTH)) {
                    break;
                }

                for (OWLAxiom child : model.data){

                    if (Configuration.TIMEOUT != null && threadTimes.getTotalUserTimeInSec() > Configuration.TIMEOUT) {
                        break;
                    }

                    if (model.label.contains(AxiomManager.getComplementOfOWLAxiom2(loader, child)) ||
                            child.equals(loader.getObservation().getOwlAxiom())){
                        //System.out.println("CHILD: " + child);
                       // System.out.println("MODEL LABEL " + model.label);
                        //System.out.println("OBSERVATION: " + loader.getObservation().getOwlAxiom());
                        continue;
                    }

                    if (!abd_literals.contains(child)){
                        continue;
                    }

                    Explanation explanation = new Explanation();

                    explanation.addAxioms(model.label);
                    explanation.addAxiom(child);
                    explanation.setAcquireTime(threadTimes.getTotalUserTimeInSec());

                    path = new ArrayList<>(explanation.getOwlAxioms());
                    /*for(OWLAxiom a : ontology.getAxioms()){
                        reasonerManager.removeAxiomFromOntology(a);
                    }
                    reasonerManager.isOntologyWithLiteralsConsistent(explanation.getOwlAxioms(), ontology);*/

                    boolean isMinimal = checkRules.isMinimal(explanations, explanation);
                    if (!isMinimal){
                        path.clear();
                        continue;
                    }

                    if (checkRules.isExplanation(explanation)){ // zmenila som
                        explanation.setDepth(explanation.getOwlAxioms().size());
                        if(CHECKING_MINIMALITY_BY_QXP && isMinimalByCallingQXP(explanation)){
                            explanations.add(explanation);
                        } else {
                            explanations.add(explanation);
                        }
                        path.clear();
                        continue;
                    }

                    if (!REUSE_OF_MODELS || !usableModelInModels()) {
                        if (Configuration.TIMEOUT != null && threadTimes.getTotalUserTimeInSec() > Configuration.TIMEOUT) {
                            System.out.println("timeout");
                            showExplanationsWithDepth(currentDepth + 1, true);
                            currentDepth = null;
                            return;
                        }
                        if (!addNewExplanations()){
                            path.clear();
                            continue;
                        }
                    }
                    else{
                        lenghtOneExplanations = new ArrayList<>();
                    }

                    ModelNode modelNode = new ModelNode();
                    modelNode.label = explanation.getOwlAxioms();
                    if (usableModelInModels()){
                        modelNode.data = negModels.get(lastUsableModelIndex).data;
                        modelNode.data.removeAll(path);
                        if (modelNode.data.isEmpty()){
                            path.clear();
                            continue;
                        }
                    }
                    modelNode.depth = model.depth + 1;
                    modelNode.add_node_explanations(model);
                    modelNode.add_to_explanations(lenghtOneExplanations);       // TU JE CHYBA?
                    queue.add(modelNode);
                    path.clear();
                }
            }
        }
        path.clear();
    }


    private Conflict getMergeConflict() throws OWLOntologyStorageException {
        if (!reasonerManager.isOntologyConsistent()) {
            return new Conflict();
        }

        //ak by bola so vsetkymi konzistetna, tzn. nema v literaloch ziadne vysvetlenie
        if (reasonerManager.isOntologyWithLiteralsConsistent(literals.getOwlAxioms(), ontology)) {
            return new Conflict();
        }
        return findConflicts(abd_literals);
    }

    private List<Explanation> findExplanations(){
        abd_literals.removeLiterals(path);
        abd_literals.removeLiterals(lenghtOneExplanations);
        Conflict conflict = findConflicts(abd_literals);
        abd_literals.addLiterals(path);
        abd_literals.addLiterals(lenghtOneExplanations);
        return conflict.getExplanations();
    }

    private Conflict findConflicts(Literals literals) {
        path.remove(negObservation);
        reasonerManager.addAxiomsToOntology(path);
        if (isOntologyWithLiteralsConsistent(literals.getOwlAxioms())) {
            return new Conflict(literals, new LinkedList<>());
        }
        removeAxiomsFromOntology(path);
        if (literals.getOwlAxioms().size() == 1) {
            List<Explanation> explanations = new LinkedList<>();
            explanations.add(new Explanation(literals.getOwlAxioms(), literals.getOwlAxioms().size(), threadTimes.getTotalUserTimeInSec()));
            return new Conflict(new Literals(), explanations);
        }

        List<Literals> sets = divideIntoSets(literals);

        Conflict conflictC1 = findConflicts(sets.get(0));
        Conflict conflictC2 = findConflicts(sets.get(1));

        List<Explanation> explanations = new LinkedList<>();
        explanations.addAll(conflictC1.getExplanations());
        explanations.addAll(conflictC2.getExplanations());

        Literals conflictLiterals = new Literals();
        conflictLiterals.getOwlAxioms().addAll(conflictC1.getLiterals().getOwlAxioms());
        conflictLiterals.getOwlAxioms().addAll(conflictC2.getLiterals().getOwlAxioms());

        while (!isOntologyWithLiteralsConsistent(conflictLiterals.getOwlAxioms())) {
            path.addAll(conflictC2.getLiterals().getOwlAxioms());
            Explanation X = getConflict(conflictC2.getLiterals().getOwlAxioms(), conflictC1.getLiterals());
            path.removeAll(conflictC2.getLiterals().getOwlAxioms());

            path.addAll(X.getOwlAxioms());
            Explanation CS = getConflict(X.getOwlAxioms(), conflictC2.getLiterals());
            path.removeAll(X.getOwlAxioms());

            CS.getOwlAxioms().addAll(X.getOwlAxioms());

            conflictLiterals.getOwlAxioms().removeAll(conflictC1.getLiterals().getOwlAxioms());
            X.getOwlAxioms().stream().findFirst().ifPresent(axiom -> conflictC1.getLiterals().getOwlAxioms().remove(axiom));
            conflictLiterals.getOwlAxioms().addAll(conflictC1.getLiterals().getOwlAxioms());

            /**PRECO ROVNO BREAK???**/
            if (explanations.contains(CS)) {
                break;
            }
            if(CHECKING_MINIMALITY_BY_QXP && isMinimalByCallingQXP(CS)){
                explanations.add(CS);
            } else {
                explanations.add(CS);
            }
        }

        return new Conflict(conflictLiterals, explanations);
    }

    private Explanation getConflict(Collection<OWLAxiom> axioms, Literals literals) {
        if (!axioms.isEmpty() && !isOntologyConsistent()) {
            return new Explanation();
        }

        if (literals.getOwlAxioms().size() == 1) {
            return new Explanation(literals.getOwlAxioms(), 1, threadTimes.getTotalUserTimeInSec());
        }
        List<Literals> sets = divideIntoSets(literals);

        if(checkingMinimalityWithQXP){
            pathDuringCheckingMinimality.addAll(sets.get(0).getOwlAxioms());
        } else {
            path.addAll(sets.get(0).getOwlAxioms());
        }
        Explanation D2 = getConflict(sets.get(0).getOwlAxioms(), sets.get(1));
        if(checkingMinimalityWithQXP){
            pathDuringCheckingMinimality.removeAll(sets.get(0).getOwlAxioms());
        } else {
            path.removeAll(sets.get(0).getOwlAxioms());
        }

        if(checkingMinimalityWithQXP){
            pathDuringCheckingMinimality.addAll(D2.getOwlAxioms());
        } else {
            path.addAll(D2.getOwlAxioms());
        }
        Explanation D1 = getConflict(D2.getOwlAxioms(), sets.get(0));
        if(checkingMinimalityWithQXP){
            pathDuringCheckingMinimality.removeAll(D2.getOwlAxioms());
        } else {
            path.removeAll(D2.getOwlAxioms());
        }

        /*path.addAll(D2.getOwlAxioms());
        Explanation D1 = getConflict(D2.getOwlAxioms(), sets.get(0));
        path.removeAll(D2.getOwlAxioms());*/

        Set<OWLAxiom> conflicts = new HashSet<>();
        conflicts.addAll(D1.getOwlAxioms());
        conflicts.addAll(D2.getOwlAxioms());

        return new Explanation(conflicts, conflicts.size(), threadTimes.getTotalUserTimeInSec());
    }


    private List<Literals> divideIntoSets(Literals literals) {
        List<Literals> dividedLiterals = new ArrayList<>();

        dividedLiterals.add(new Literals());
        dividedLiterals.add(new Literals());

        int count = 0;

        for (OWLAxiom owlAxiom : literals.getOwlAxioms()) {
            dividedLiterals.get(count % 2).getOwlAxioms().add(owlAxiom);
            count++;
        }

        System.out.println("\n\nPRVA MNOZINA " + dividedLiterals.get(0).getOwlAxioms());
        System.out.println("DRUHA MNOZINA " + dividedLiterals.get(1).getOwlAxioms() + "\n\n");

        return dividedLiterals;
    }

    private String getClassName(OWLAxiom axiom) {
        return Printer.print(axiom).split("\\" + DLSyntax.LEFT_PARENTHESES)[0];
    }

    private boolean containsNegation(String name) {
        return name.contains(DLSyntax.DISPLAY_NEGATION);
    }

    private ModelNode getNegModelByReasoner() {
        ModelNode modelNode = new ModelNode();
        List<OWLAxiom> model = new LinkedList<>();

        if (path != null) {
            path.remove(negObservation);
            reasonerManager.addAxiomsToOntology(path);
            if (!reasonerManager.isOntologyConsistent()){
                removeAxiomsFromOntology(path);
                modelNode.data = model;
                return modelNode;
            }
        }

        for (int i = 0; i < assertionsAxioms.size(); i++) {
            OWLAxiom axiom = assertionsAxioms.get(i);
            OWLAxiom complementOfAxiom = negAssertionsAxioms.get(i);
            if (loader.getOntology().containsAxiom(axiom)){
                model.add(axiom);
            }
            else if (loader.getOntology().containsAxiom(complementOfAxiom)){
                model.add(complementOfAxiom);
            }
            else if (!model.contains(axiom) && !model.contains(complementOfAxiom)){
                reasonerManager.addAxiomToOntology(axiom);
                boolean isConsistent = reasonerManager.isOntologyConsistent();
                reasonerManager.removeAxiomFromOntology(axiom);

                reasonerManager.addAxiomToOntology(complementOfAxiom);
                boolean isComplementConsistent = reasonerManager.isOntologyConsistent();
                reasonerManager.removeAxiomFromOntology(complementOfAxiom);

                if (!isComplementConsistent && isConsistent) {
                    model.add(axiom);
                    reasonerManager.addAxiomToOntology(axiom);
                } else if (isComplementConsistent){
                    model.add(complementOfAxiom);
                    reasonerManager.addAxiomToOntology(complementOfAxiom);
                }
                else {
                    modelNode.data.clear();
                    removeAxiomsFromOntology(path);
                    return modelNode;
                }
            }
        }
        removeAxiomsFromOntology(path);
        modelNode.data = new LinkedList<>();
        for (OWLAxiom axiom: model){
            if (abducibles.getIndividuals().containsAll(axiom.individualsInSignature().collect(Collectors.toList())) &&
                    abducibles.getClasses().containsAll( axiom.classesInSignature().collect(Collectors.toList()))){
                modelNode.data.add(axiom);
            }
        }
        addModel(modelNode, getComplementOfModel(modelNode.data));
        return negModels.get(lastUsableModelIndex);
    }

    private ModelNode getComplementOfModel(List<OWLAxiom> model) {
        ModelNode negModelNode = new ModelNode();
        List<OWLAxiom> negModel = new LinkedList<>();
        for (OWLAxiom axiom : model) {
            OWLAxiom complement = AxiomManager.getComplementOfOWLAxiom(loader, axiom);
            negModel.add(complement);
        }
        negModelNode.data = negModel;
        return negModelNode;
    }

    private ModelNode getNegModelByOntology(){  // mrozek
        ModelNode negModelNode = new ModelNode();
        ModelNode modelNode = new ModelNode();
        modelNode.data = new LinkedList<>();

        if(checkingMinimalityWithQXP){
            if(loader.isMultipleObservationOnInput()){
                for(OWLAxiom axiom : loader.getMultipleObservations()){
                    pathDuringCheckingMinimality.remove(AxiomManager.getComplementOfOWLAxiom(loader, axiom));
                }
            } else {
                pathDuringCheckingMinimality.remove(negObservation);
            }

            reasonerManager.addAxiomsToOntology(pathDuringCheckingMinimality);
            if (!reasonerManager.isOntologyConsistent()){
                removeAxiomsFromOntology(pathDuringCheckingMinimality);
                return modelNode;
            }
        } else {
            if (path != null) {
                /**Je potrebne robit tieto remove veci, ak sa to tam realne uz nema ako dostat???**/
                if(loader.isMultipleObservationOnInput()){
                    for(OWLAxiom axiom : loader.getMultipleObservations()){
                        //axiom
                        path.remove(AxiomManager.getComplementOfOWLAxiom(loader, axiom));
                    }
                } else {
                    path.remove(negObservation);
                }
                //path.remove(negObservation);
                reasonerManager.addAxiomsToOntology(path);
                if (!reasonerManager.isOntologyConsistent()){
                    removeAxiomsFromOntology(path);
                    return modelNode;
                }
            }
        }


        System.out.println("PATH " + path);

        OWLDataFactory dfactory = OWLManager.createOWLOntologyManager().getOWLDataFactory();

        ArrayList<OWLNamedIndividual> individualArray = new ArrayList<>(ontology.getIndividualsInSignature());
        Set<OWLAxiom> negModelSet = new HashSet<>();
        Set<OWLAxiom> modelSet = new HashSet<>();

        for (OWLNamedIndividual ind : individualArray) {
            System.out.println("INDIVIDUAL " + ind);;
            if (!abducibles.getIndividuals().contains(ind)){
                continue;
            }

            Set<OWLClassExpression> ontologyTypes = EntitySearcher.getTypes(ind, ontology).collect(toSet());
            System.out.println("ONTOLOGY TYPES " + ontologyTypes);
            Set<OWLClassExpression> knownTypes = new HashSet<>();
            Set<OWLClassExpression> knownNotTypes = new HashSet<>();

            System.out.println();
            System.out.println("FOR CYKLUS CEZ VSETKY ONONTOLOGY TYPES");
            for (OWLClassExpression exp : ontologyTypes) {
                System.out.println("CLASS " + exp);
                System.out.println("EXPR TYPE " + exp.getClassExpressionType());
                assert (exp.isClassExpressionLiteral());
                if (exp.isOWLClass()) {
                    System.out.println("IS OWL CLASS");
                    knownTypes.add((exp));
                } else {
                    System.out.println("IS NOT OWL CLASS " + exp.getComplementNNF());
                    knownNotTypes.add(exp.getComplementNNF());
                }
            }

            System.out.println("KNOWN TYPES " + knownTypes);
            System.out.println("KNOWN NOT TYPES " + knownNotTypes);

            Set<OWLClassExpression> newNotTypes = classSet2classExpSet(ontology.classesInSignature().collect(toSet()));
            System.out.println("NEW NOT TYPES " + newNotTypes);

            newNotTypes.remove(dfactory.getOWLThing());
            newNotTypes.removeAll(knownNotTypes);
            System.out.println("NEW NOT TYPES AFTER REMOVING KNOWN NOT TYPES " + newNotTypes);

            Set<OWLClassExpression> foundTypes = nodeClassSet2classExpSet(loader.getReasoner().getTypes(ind, false).getNodes());
            System.out.println("FOUND TYPES " + foundTypes);

            newNotTypes.removeAll(foundTypes);
            System.out.println("NEW NOT TYPES AFTER REMOVING FOUND TYPES " + newNotTypes);

            foundTypes.removeAll(knownTypes);
            System.out.println("FOUND TYPES AFTER REMOVING KNOWN TYPES " + foundTypes);

            for (OWLClassExpression classExpression : foundTypes) {
                if (!abducibles.getClasses().contains(classExpression)){
                    continue;
                }
                OWLClassExpression negClassExp = classExpression.getComplementNNF();
                OWLAxiom axiom = dfactory.getOWLClassAssertionAxiom(negClassExp, ind);
                negModelSet.add(axiom);
                modelSet.add(dfactory.getOWLClassAssertionAxiom(classExpression, ind));
            }
            for (OWLClassExpression classExpression : newNotTypes) {
                if (!abducibles.getClasses().contains(classExpression)){
                    continue;
                }
                OWLClassExpression negClassExp = classExpression.getComplementNNF();
                OWLAxiom axiom = dfactory.getOWLClassAssertionAxiom(classExpression, ind);
                negModelSet.add(axiom);
                modelSet.add(dfactory.getOWLClassAssertionAxiom(negClassExp, ind));
            }
        }

        if(checkingMinimalityWithQXP){
            removeAxiomsFromOntology(pathDuringCheckingMinimality);
        } else {
            removeAxiomsFromOntology(path);
        }
        modelNode.data = new LinkedList<>(modelSet);
        negModelNode.data = new LinkedList<>(negModelSet);
        lastUsableModelIndex = models.indexOf(modelNode);
        if (!modelNode.data.isEmpty() && lastUsableModelIndex == -1) {
            lastUsableModelIndex = models.size();
            addModel(modelNode, negModelNode);
        }
        System.out.println("MODEL " + modelNode.data);
        System.out.println("NEG MODEL " + negModelNode.data);
        System.out.println();
        return negModelNode;
    }

    private void addModel(ModelNode model, ModelNode negModel){
        lastUsableModelIndex = models.indexOf(model);
        if (lastUsableModelIndex != -1 || model.data.isEmpty()){
            return;
        }
        lastUsableModelIndex = models.size();
        models.add(model);
        negModels.add(negModel);
    }

    public static Set<OWLClassExpression> nodeClassSet2classExpSet(Set<Node<OWLClass>> nodeList) {
        Set<OWLClassExpression> toReturn = new HashSet<>();
        for (Node<OWLClass> node : nodeList) {
            toReturn.addAll(node.getEntitiesMinusTop());
        }
        return toReturn;
    }

    public static Set<OWLClassExpression> classSet2classExpSet(Set<OWLClass> classSet) {
        Set<OWLClassExpression> toReturn = new HashSet<>();
        toReturn.addAll(classSet);
        return toReturn;
    }

    private boolean usableModelInModels(){
        for (int i = models.size()-1; i >= 0 ; i--){
            if (models.get(i).data.containsAll(path)){
                lastUsableModelIndex = i;
                return true;
            }
        }
        return false;
    }

    private boolean addNewExplanations(){
        List<Explanation> newExplanations = findExplanations();
        lenghtOneExplanations = new ArrayList<>();
        for (Explanation conflict : newExplanations){
            if (conflict.getOwlAxioms().size() == 1){
                lenghtOneExplanations.add(Iterables.get(conflict.getOwlAxioms(), 0));
            }
            conflict.addAxioms(path);
            if (isMinimal(explanations, conflict)){
                if(CHECKING_MINIMALITY_BY_QXP && isMinimalByCallingQXP(conflict)){
                    conflict.setDepth(conflict.getOwlAxioms().size());
                    explanations.add(conflict);

                } else {
                    conflict.setDepth(conflict.getOwlAxioms().size());
                    explanations.add(conflict);
                }
            }
        }
        /*System.out.println("\n \n EXPLANATIONS ");
        for(Explanation a: explanations){
            System.out.println("Ex " + a);
        }
        System.out.println("\n \n");*/
        if (newExplanations.size() == this.lenghtOneExplanations.size()){
            return false;
        }
        return !newExplanations.isEmpty();
    }

    private boolean isOntologyWithLiteralsConsistent(Collection<OWLAxiom> axioms){
        path.addAll(axioms);
        boolean isConsistent = isOntologyConsistent();
        path.removeAll(axioms);
        return isConsistent;
    }

    private boolean isOntologyConsistent(){
        if (GET_MODELS_BY_REASONER){
            return (!getNegModelByReasoner().data.isEmpty());
        }
        return (!getNegModelByOntology().data.isEmpty());
    }

    private void printAxioms(List<OWLAxiom> axioms){
        List<String> result = new ArrayList<>();
        for (OWLAxiom owlAxiom : axioms) {
            result.add(Printer.print(owlAxiom));
        }
        System.out.println("{" + StringUtils.join(result, ",") + "}");
    }

    public boolean isMinimal(List<Explanation> explanationList, Explanation explanation) {
        if (explanation == null || !(explanation.getOwlAxioms() instanceof List)) {
            return false;
        }

        for (Explanation minimalExplanation : explanationList) {
            if (explanation.getOwlAxioms().containsAll(minimalExplanation.getOwlAxioms())) {
                return false;
            }
        }

        return true;
    }

    public boolean isMinimalByCallingQXP(Explanation explanation){
        Set<OWLAxiom> temp = new HashSet<>();
        temp.addAll(explanation.getOwlAxioms());
        if(path != null){
            temp.addAll(path);
        }
        Literals potentialExplanations = new Literals(temp);

        checkingMinimalityWithQXP = true;
        pathDuringCheckingMinimality = new ArrayList<>();
        //reasonerManager.addAxiomToOntology(loader.getNegObservation().getOwlAxiom());
        Explanation newExplanation = getConflict(new ArrayList<>(), potentialExplanations);
        checkingMinimalityWithQXP = false;
        pathDuringCheckingMinimality = new ArrayList<>();

        if (newExplanation.getOwlAxioms().containsAll(explanation.getOwlAxioms())){
            return true;
        }
        return false;
    }

    private void removeAxiomsFromOntology(List<OWLAxiom> axioms){
        reasonerManager.resetOntology(loader.getOriginalOntology().axioms());
    }

    private void showExplanations() throws OWLOntologyStorageException {
        System.out.println("SHOWING EXPLANATIONS " + explanations);
        StringBuilder result = new StringBuilder();
        List<Explanation> filteredExplanations = filterExplanations();
        System.out.println("SHOWING EXPLANATIONS AFTER FILTER " + filteredExplanations);
        path.clear();
        minimalExplanations = new LinkedList<>();

        int depth = 1;
        while (filteredExplanations.size() > 0) {
            List<Explanation> currentExplanations = removeExplanationsWithDepth(filteredExplanations, depth);
            filterIfNotMinimal(currentExplanations);
            System.out.println("SHOWING EXPLANATIONS AFTER FILTER NON MINIMAL  " + currentExplanations);
            try {
                filterIfNotRelevant(currentExplanations);
                System.out.println("SHOWING EXPLANATIONS AFTER FILTER NON RELEVANT  " + currentExplanations);
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }
            if (currentExplanations.isEmpty()) {
                depth++;
                continue;
            }
            if (!level_times.containsKey(depth)){
                level_times.put(depth, find_level_time(currentExplanations));
            }
            minimalExplanations.addAll(currentExplanations);
            String currentExplanationsFormat = StringUtils.join(currentExplanations, ",");
            String line = String.format("%d;%d;%.2f;{%s}\n", depth, currentExplanations.size(), level_times.get(depth), currentExplanationsFormat);
            System.out.print(line);
            result.append(line);
            depth++;
        }
        String line = String.format("%.2f\n", threadTimes.getTotalUserTimeInSec());
        System.out.print(line);
        result.append(line);
        log_explanations_times(minimalExplanations);

        FileLogger.appendToFile(FileLogger.HYBRID_LOG_FILE__PREFIX, currentTimeMillis, result.toString());
    }

    private double find_level_time(List<Explanation> explanations){
        double time = 0;
        for (Explanation exp: explanations){
            if (exp.getAcquireTime() > time){
                time = exp.getAcquireTime();
            }
        }
        return time;
    }

    private void log_explanations_times(List<Explanation> explanations){
        StringBuilder result = new StringBuilder();
        for (Explanation exp: explanations){
            String line = String.format("%.2f;%s\n", exp.getAcquireTime(), exp);
            result.append(line);
        }
        FileLogger.appendToFile(FileLogger.HYBRID_EXP_TIMES_LOG_FILE__PREFIX, currentTimeMillis, result.toString());
    }

    private void filterIfNotMinimal(List<Explanation> explanations){
        List<Explanation> notMinimalExplanations = new LinkedList<>();
        for (Explanation e: explanations){
            for (Explanation m: minimalExplanations){
                if (e.getOwlAxioms().containsAll(m.getOwlAxioms())){
                    notMinimalExplanations.add(e);
                }
            }
        }
        explanations.removeAll(notMinimalExplanations);
    }

    private void filterIfWithS0(List<Explanation> explanations){
        List<Explanation> notMinimalExplanations = new LinkedList<>();
        for (Explanation e: explanations){
            for(OWLAxiom a : e.getOwlAxioms()){
                if(a.containsEntityInSignature(loader.getReductionIndividual())){
                    notMinimalExplanations.add(e);
                    break;
                }
            }
        }
        explanations.removeAll(notMinimalExplanations);
    }

    private void filterIfNotRelevant(List<Explanation> explanations) throws OWLOntologyCreationException {
        List<Explanation> notRelevantExplanations = new LinkedList<>();

        OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = ontologyManager.createOntology();

        OWLReasonerFactory reasonerFactory = new OpenlletReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);

        for (Explanation e : explanations){

            ontologyManager.addAxioms(ontology, e.getOwlAxioms());

            if(loader.isMultipleObservationOnInput()){
                for(OWLAxiom a : loader.getMultipleObservations()){
                    OWLClassAssertionAxiom res = (OWLClassAssertionAxiom) a;
                    OWLClassExpression classExpression = res.getClassExpression();
                    OWLClassExpression classNeg = classExpression.getComplementNNF();
                    OWLAxiom temp = loader.getDataFactory().getOWLClassAssertionAxiom(classNeg, res.getIndividual());
                    ontologyManager.addAxiom(ontology, temp);

                    reasoner.flush();
                    if(!reasoner.isConsistent()){
                        notRelevantExplanations.add(e);
                        reasoner.flush();
                        ontologyManager.removeAxiom(ontology, temp);
                        break;
                    }
                    ontologyManager.removeAxiom(ontology, temp);
                }
            } else {
                OWLClassAssertionAxiom res = (OWLClassAssertionAxiom) loader.getObservation().getOwlAxiom();
                OWLClassExpression classExpression = res.getClassExpression();
                OWLClassExpression classNeg = classExpression.getComplementNNF();
                OWLAxiom temp = loader.getDataFactory().getOWLClassAssertionAxiom(classNeg, res.getIndividual());
                ontologyManager.addAxiom(ontology, temp);

                reasoner.flush();
                if(!reasoner.isConsistent()){
                    notRelevantExplanations.add(e);
                    reasoner.flush();
                    ontologyManager.removeAxiom(ontology, temp);
                    break;
                }
                ontologyManager.removeAxiom(ontology, temp);
            }

            ontologyManager.removeAxioms(ontology, e.getOwlAxioms());
        }
        explanations.removeAll(notRelevantExplanations);
    }

    private List<Explanation> removeExplanationsWithDepth(List<Explanation> filteredExplanations, Integer depth) {
        List<Explanation> currentExplanations = filteredExplanations.stream().filter(explanation -> explanation.getDepth().equals(depth)).collect(Collectors.toList());
        filteredExplanations.removeAll(currentExplanations);
        return currentExplanations;
    }

    private void showExplanationsWithDepth(Integer depth, boolean timeout) {
        List<Explanation> currentExplanations = explanations.stream().filter(explanation -> explanation.getDepth().equals(depth)).collect(Collectors.toList());
        String currentExplanationsFormat = StringUtils.join(currentExplanations, ",");
        Double time = threadTimes.getTotalUserTimeInSec();
        level_times.put(depth, time);
        String line = String.format("%d;%d;%.2f%s;{%s}\n", depth, currentExplanations.size(), time, timeout ? "-TIMEOUT" : "", currentExplanationsFormat);
        System.out.print(line);
        FileLogger.appendToFile(FileLogger.HYBRID_PARTIAL_EXPLANATIONS_LOG_FILE__PREFIX, currentTimeMillis, line);
    }

    private List<Explanation> filterExplanations() throws OWLOntologyStorageException {
        loader.getOntologyManager().removeAxiom(ontology, loader.getNegObservation().getOwlAxiom());

        /*System.out.println("TU");
        Set<OWLAxiom> set1 = loader.getOntology().getAxioms();
        int i = 0;
        for(OWLAxiom a : set1){
            System.out.println(i + " : " + a);
            i++;
        }
        System.out.println(loader.getOntology());*/

        /*pridane kvoli tomu, ze vzdy PRVE vysvetlenie pri pouziti hermitu odignorovalo*/
        reasonerManager.resetOntology(ontology.axioms());

        /*System.out.println("TU");
        Set<OWLAxiom> set2 = loader.getOntology().getAxioms();
        int j = 0;
        for(OWLAxiom a : set2){
            System.out.println(j + " : " + a);
            j++;
        }
        System.out.println(loader.getOntology());*/

        List<Explanation> filteredExplanations = new ArrayList<>();
        for (Explanation explanation : explanations) {
            if (isExplanation(explanation)) {
                if (reasonerManager.isOntologyWithLiteralsConsistent(explanation.getOwlAxioms(), ontology)) {
                    filteredExplanations.add(explanation);
                }
            }
        }

        return filteredExplanations;
    }

    private boolean isExplanation(Explanation explanation) {
        if (explanation.getOwlAxioms().size() == 1) {
            return true;
        }

        for (OWLAxiom axiom1 : explanation.getOwlAxioms()) {
            String name1 = getClassName(axiom1);
            boolean negated1 = containsNegation(name1);
            if (negated1) {
                name1 = name1.substring(1);
            }

            for (OWLAxiom axiom2 : explanation.getOwlAxioms()) {
                if (!axiom1.equals(axiom2)) {
                    String name2 = getClassName(axiom2);

                    boolean negated2 = containsNegation(name2);
                    if (negated2) {
                        name2 = name2.substring(1);
                    }

                    if (name1.equals(name2) && ((!negated1 && negated2) || (negated1 && !negated2))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean isExplanation2(Explanation explanation) {
        if (explanation.getOwlAxioms().size() == 1) {
            return true;
        }

        for (OWLAxiom axiom1 : explanation.getOwlAxioms()) {
            String name1 = getClassName(axiom1);
            boolean negated1 = containsNegation(name1);
            if (negated1) {
                name1 = name1.substring(1);
            }

            for (OWLAxiom axiom2 : explanation.getOwlAxioms()) {
                if (!axiom1.equals(axiom2) && axiom1.getIndividualsInSignature().equals(axiom2.getIndividualsInSignature())) {
                    String name2 = getClassName(axiom2);

                    boolean negated2 = containsNegation(name2);
                    if (negated2) {
                        name2 = name2.substring(1);
                    }

                    if (name1.equals(name2) && ((!negated1 && negated2) || (negated1 && !negated2))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }


}