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
import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import reasoner.AxiomManager;
import reasoner.ILoader;
import reasoner.IReasonerManager;
import timer.ThreadTimes;
import org.semanticweb.owlapi.reasoner.Node;

import java.util.*;
import java.util.stream.Collectors;

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
    private Abducibles abducibles;

    List<OWLAxiom> lenghtOneExplanations = new ArrayList<>();

    private int lastUsableModelIndex;
    private OWLAxiom negObservation;

    private ThreadTimes threadTimes;
    private long currentTimeMillis;

    private boolean REUSE_OF_MODELS = true;
    private boolean GET_MODELS_BY_REASONER = false;

    private Map<Integer, Double> level_times = new HashMap<>();

    public HybridSolver(ThreadTimes threadTimes, long currentTimeMillis) {
        this.threadTimes = threadTimes;
        this.currentTimeMillis = currentTimeMillis;
    }

    @Override
    public void solve(ILoader loader, IReasonerManager reasonerManager) {
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
        allLiterals.remove(loader.getObservation().getOwlAxiom());
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
            for (OWLAxiom ax : assertionsAxioms){
                if (ax.equals(loader.getObservation().getOwlAxiom())){
                    continue;
                }
                if (abducibles.getIndividuals().containsAll(ax.getIndividualsInSignature()) &&
                        abducibles.getClasses().containsAll(ax.getClassesInSignature())){
                    to_abd.add(ax);
                }
            }
        }
        abd_literals = new Literals(to_abd);
        if (abd_literals.getOwlAxioms().isEmpty()){
            if (Configuration.NEGATION_ALLOWED){
                abd_literals = literals;        // ok??
            }
            else{
                abd_literals.addLiterals(assertionsAxioms);
                abd_literals.removeLiteral(loader.getObservation().getOwlAxiom());
            }
        }
    }

    private void startSolving() {
        explanations = new LinkedList<>();
        path = new ArrayList<>();
        ICheckRules checkRules = new CheckRules(loader, reasonerManager);
        Integer currentDepth = 0;

        path = new ArrayList<>();

        Conflict conflict = getMergeConflict();
        for (Explanation e: conflict.getExplanations()){
            e.setDepth(e.getOwlAxioms().size());
        }
        explanations = conflict.getExplanations();
//        showExplanationsWithDepth(1, false);

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
                if (model.depth.equals(Configuration.DEPTH)) {
                    break;
                }

                for (OWLAxiom child : model.data){

//                    if (Configuration.TIMEOUT != null && threadTimes.getTotalUserTimeInSec() > Configuration.TIMEOUT) {
//                        break;
//                    }
                    if (model.label.contains(AxiomManager.getComplementOfOWLAxiom(loader, child)) ||
                            child.equals(loader.getObservation().getOwlAxiom())){
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

                    boolean isMinimal = checkRules.isMinimal(explanations, explanation);
                    if (!isMinimal){
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
                        if (REUSE_OF_MODELS && checkRules.isExplanation(explanation)){
                            explanation.setDepth(explanation.getOwlAxioms().size());
                            explanations.add(explanation);
                            path.clear();
                            continue;
                        }
                        if (!addNewExplanations()){
                            path.clear();
                            continue;
                        }
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
                    modelNode.add_to_explanations(lenghtOneExplanations);
                    queue.add(modelNode);
                    path.clear();
                }
            }
        }
        path.clear();
    }


    private Conflict getMergeConflict() {
        if (!reasonerManager.isOntologyConsistent()) {
            return new Conflict();
        }

        if (reasonerManager.isOntologyWithLiteralsConsistent(literals.getOwlAxioms(), ontology)) {
            return new Conflict();
        }
//        abd_literals.removeLiterals(path); // nevraciam cestu naspat? mam ju vobec davat prec?
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
        int n = 0;
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

            if (explanations.contains(CS)) {
                break;      // continue?
            }
            explanations.add(CS);
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

        path.addAll(sets.get(0).getOwlAxioms());
        Explanation D2 = getConflict(sets.get(0).getOwlAxioms(), sets.get(1));
        path.removeAll(sets.get(0).getOwlAxioms());

        path.addAll(D2.getOwlAxioms());
        Explanation D1 = getConflict(D2.getOwlAxioms(), sets.get(0));
        path.removeAll(D2.getOwlAxioms());

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
//        if (!negModel.isEmpty() && lastUsableModelIndex == negModels.size()) {
//            addModel(negModelNode, true);
//        }
        return negModelNode;
    }

    private ModelNode getNegModelByOntology(){
        ModelNode negModelNode = new ModelNode();
        List<OWLAxiom> negModel = new LinkedList<>();
        ModelNode modelNode = new ModelNode();
        modelNode.data = new LinkedList<>();

        if (path != null) {
            path.remove(negObservation);
            reasonerManager.addAxiomsToOntology(path);
            if (!reasonerManager.isOntologyConsistent()){
                removeAxiomsFromOntology(path);
                return modelNode;
            }
        }

        OWLDataFactory dfactory = OWLManager.createOWLOntologyManager().getOWLDataFactory();

        ArrayList<OWLNamedIndividual> individualArray = new ArrayList<>(ontology.getIndividualsInSignature());
        Set<OWLAxiom> negModelSet = new HashSet<>();
        Set<OWLAxiom> modelSet = new HashSet<>();

        for (OWLNamedIndividual ind : individualArray) {
            /***********************
             class assertion axioms
             ***********************/
            if (!abducibles.getIndividuals().contains(ind)){
                continue;
            }

//            System.out.println(ind);

            Set<OWLClassExpression> ontologyTypes =  //type assertions mentioned in ontology
                    EntitySearcher.getTypes(ind, ontology).collect(toSet());
            Set<OWLClassExpression> knownTypes = new HashSet<>(); //individual is of these types, according to ontology
            Set<OWLClassExpression> knownNotTypes =  //individual is NOT of these types, according to ontology
                    new HashSet<>();
            for (OWLClassExpression exp : ontologyTypes) {
                assert (exp.isClassExpressionLiteral()); // in case this assumption is not correct, we can remove it
                //and uncomment the if (though it would mean we would be ignoring AND types
                //if (exp.isClassExpressionLiteral()){
                    /*
                     We are ignoring more complex class expressions
                     We should propably also consider AND
                     */
                if (exp.isOWLClass()) {
                    knownTypes.add((exp));	// ak to, co naslo, je OWLclass, tak to bolo typu jane:Mother
                } else {
                    knownNotTypes.add(exp.getComplementNNF());	// inak to bolo typu jane:-Mother, teda -Mother nie je priamo OWLclass
                }
                //}
            }
            Set<OWLClassExpression> newNotTypes = //negated types in model but not in ontology
                    classSet2classExpSet(ontology.classesInSignature().collect(toSet()));

            newNotTypes.remove(dfactory.getOWLThing()); // vrati Thing classu
            newNotTypes.removeAll(knownNotTypes);
            Set<OWLClassExpression> foundTypes = //non-negated types in model but not in ontology
                    nodeClassSet2classExpSet(loader.getReasoner().getTypes(ind, false).getNodes());

            newNotTypes.removeAll(foundTypes);
            foundTypes.removeAll(knownTypes);

            for (OWLClassExpression classExpression : foundTypes) {	// neguje ich, lebo hlada antimodel
                if (!abducibles.getClasses().contains(classExpression)){
                    continue;
                }
                OWLClassExpression negClassExp = classExpression.getComplementNNF();
                OWLAxiom axiom = dfactory.getOWLClassAssertionAxiom(negClassExp, ind);
//                System.out.println(axiom);
//                System.out.println(abd_literals);
                negModelSet.add(axiom);
                modelSet.add(dfactory.getOWLClassAssertionAxiom(classExpression, ind));
//                if (abd_literals.contains(axiom)) {
//                    System.out.println("found");
//                    System.out.println(ind);
//                    System.out.println(axiom);
//                }
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

            /**********************
             role assertion axioms
             **********************/
//            List<OWLObjectProperty> listOfRoles = //all roles mentioned in ontology
//                    ontology.objectPropertiesInSignature().collect(Collectors.toList());
//            for (OWLObjectProperty p : listOfRoles) {
//                Set<OWLIndividual> knownRelated = EntitySearcher.getObjectPropertyValues(ind, p, ontology)
//                        .collect(toSet());
//                Set<OWLIndividual> unknownRelated =
//                        loader.getReasoner().getObjectPropertyValues(ind, p).entities().collect(toSet());
//                Set<OWLIndividual> notRelated = ontology.individualsInSignature().collect(toSet());
//                notRelated.removeAll(unknownRelated);
//                unknownRelated.removeAll(knownRelated);
//                if (!Config.loopsAllowed) {
//                    unknownRelated.remove(ind);
//                    notRelated.remove(ind);
//                }
//                for (OWLIndividual relatedInd : unknownRelated) {
//                    toReturn.add(dfactory.getOWLNegativeObjectPropertyAssertionAxiom(p, ind, relatedInd));
//                }
//                for (OWLIndividual unrelatedInd : notRelated) {
//                    toReturn.add(dfactory.getOWLObjectPropertyAssertionAxiom(p, ind, unrelatedInd));
//                }
//            }
        }

        removeAxiomsFromOntology(path);
        modelNode.data = new LinkedList<>(modelSet);
        negModelNode.data = new LinkedList<>(negModelSet);
        lastUsableModelIndex = models.indexOf(modelNode);
        if (!modelNode.data.isEmpty() && lastUsableModelIndex == -1) {
            lastUsableModelIndex = models.size();
            addModel(modelNode, negModelNode);
        }
        return negModelNode;
    }

    private void addModel(ModelNode model, ModelNode negModel){
//        List<OWLAxiom> data = new ArrayList<>();
//        for (OWLAxiom axiom: model.data){
//            if (abd_literals.contains(axiom)){
//                data.add(axiom);
//            }
//        }
//        model.data = data;
        lastUsableModelIndex = models.indexOf(model);
        if (lastUsableModelIndex != -1 || model.data.isEmpty()){
            return;
        }
        lastUsableModelIndex = models.size();
        models.add(model);
        negModels.add(negModel);
    }

    public static Set<OWLClassExpression> nodeClassSet2classExpSet(Set<Node<OWLClass>> nodeList) {
        //node is returned from reasoner model
        Set<OWLClassExpression> toReturn = new HashSet<>();
        for (Node<OWLClass> node : nodeList) {
            //node
            toReturn.addAll(node.getEntitiesMinusTop());
        }
        return toReturn;
    }

    public static Set<OWLClassExpression> classSet2classExpSet(Set<OWLClass> classSet) {
        //transforms each class into (superclass) class expression
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
                conflict.setDepth(conflict.getOwlAxioms().size());
                explanations.add(conflict);
            }
        }
//        literals.removeLiterals(lenghtOneExplanations);
//        boolean allExplanationsFound = literals.getOwlAxioms().size() <= 1;
//        literals.addLiterals(lenghtOneExplanations);
        if (newExplanations.size() == this.lenghtOneExplanations.size()){
            return false;
        }
        return !newExplanations.isEmpty(); // && !allExplanationsFound;
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

    private void removeAxiomsFromOntology(List<OWLAxiom> axioms){
        reasonerManager.resetOntology(loader.getOriginalOntology().axioms());
    }

    private void showExplanations() {
        StringBuilder result = new StringBuilder();
        List<Explanation> filteredExplanations = filterExplanations();
        path.clear();
        minimalExplanations = new LinkedList<>();

        int depth = 1;
        while (filteredExplanations.size() > 0) {
            List<Explanation> currentExplanations = removeExplanationsWithDepth(filteredExplanations, depth);
            filterIfNotMinimal(currentExplanations);
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

    private List<Explanation> filterExplanations() {
        loader.getOntologyManager().removeAxiom(ontology, loader.getNegObservation().getOwlAxiom());
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

}

