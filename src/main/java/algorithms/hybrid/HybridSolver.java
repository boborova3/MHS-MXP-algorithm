package algorithms.hybrid;

import algorithms.ISolver;
import com.google.common.collect.Iterables;
import common.Configuration;
import common.Printer;
import models.Abducibles;
import models.Explanation;
import models.Literals;
import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.*;
import reasoner.AxiomManager;
import reasoner.ILoader;
import reasoner.IReasonerManager;
import timer.ThreadTimes;
import java.util.*;

/**
 * Base = knowledgeBase + negObservation
 * Literals = set of all literals / concepts with named individual except observation
 */

public class HybridSolver implements ISolver {

    private boolean REUSE_OF_MODELS = true;
    private boolean GET_MODELS_BY_REASONER = false;
    private boolean CHECKING_MINIMALITY_BY_QXP;
    private boolean MHS_MODE;
    private boolean ROLES_IN_EXPLANATIONS;
    private boolean CACHED_CONFLICTS_LONGEST_CONFLICT = true;
    private boolean CACHED_CONFLICTS_TABLE_OF_OCCURRENCE = false;

    private ILoader loader;
    private IReasonerManager reasonerManager;
    public OWLOntology ontology;

    public List<ModelNode> models;
    public List<ModelNode> negModels;
    public List<OWLAxiom> assertionsAxioms;
    public List<OWLAxiom> negAssertionsAxioms;
    private Literals literals;
    private Literals abd_literals;
    public List<Explanation> explanations;
    public List<OWLAxiom> path;
    public List<OWLAxiom> pathDuringCheckingMinimality;
    public Abducibles abducibles;
    private ModelExtractor modelExtractor;
    private ExplanationsFilter explanationsFilter;

    List<OWLAxiom> lenghtOneExplanations = new ArrayList<>();
    List<Set<OWLAxiom>> pathsInCertainDepth = new ArrayList<>();

    public int lastUsableModelIndex;
    public OWLAxiom negObservation;

    public ThreadTimes threadTimes;
    public long currentTimeMillis;

    public Map<Integer, Double> level_times = new HashMap<>();

    public boolean checkingMinimalityWithQXP = false;
    private Explanation foundedExplanation;

    public HybridSolver(ThreadTimes threadTimes, long currentTimeMillis) {
        this.threadTimes = threadTimes;
        this.currentTimeMillis = currentTimeMillis;
        if(Configuration.MHS_MODE){
            MHS_MODE = true;
            CHECKING_MINIMALITY_BY_QXP = false;
        } else {
            MHS_MODE = false;
            CHECKING_MINIMALITY_BY_QXP = true;
        }
        ROLES_IN_EXPLANATIONS = false;
    }

    @Override
    public void solve(ILoader loader, IReasonerManager reasonerManager) throws OWLOntologyStorageException, OWLOntologyCreationException {
        this.loader = loader;
        this.reasonerManager = reasonerManager;
        this.ontology = this.loader.getOriginalOntology();
        this.modelExtractor = new ModelExtractor(loader, reasonerManager, this);
        this.explanationsFilter = new ExplanationsFilter(loader, reasonerManager, this);

        negObservation = loader.getNegObservation().getOwlAxiom();
        this.abducibles = loader.getAbducibles();
        if (!reasonerManager.isOntologyConsistent())
            return;
        initialize();
        if (reasonerManager.isOntologyWithLiteralsConsistent(literals.getOwlAxioms(), ontology))
            return;
        startSolving();
        explanationsFilter.showExplanations(MHS_MODE);
    }

    private void initialize() {
        models = new ArrayList<>();
        negModels = new ArrayList<>();

        assertionsAxioms = new ArrayList<>();
        negAssertionsAxioms = new ArrayList<>();

        loader.getOntologyManager().addAxiom(ontology, loader.getNegObservation().getOwlAxiom());
        reasonerManager.addAxiomToOntology(loader.getNegObservation().getOwlAxiom());

        loader.getOntology().axioms(AxiomType.DECLARATION).forEach(axiom -> {
            List<OWLAxiom> classAssertionAxiom = AxiomManager.createClassAssertionAxiom(loader, axiom);
            for (int i = 0; i < classAssertionAxiom.size(); i++) {
                if (i % 2 == 0) {
                    assertionsAxioms.add(classAssertionAxiom.get(i));
                } else {
                    negAssertionsAxioms.add(classAssertionAxiom.get(i));
                }
            }
            if(ROLES_IN_EXPLANATIONS){
                List<OWLAxiom> objectPropertyAssertionAxiom = AxiomManager.createObjectPropertyAssertionAxiom(loader, axiom);
                for (int i = 0; i < objectPropertyAssertionAxiom.size(); i++) {
                    if (i % 2 == 0) {
                        assertionsAxioms.add(objectPropertyAssertionAxiom.get(i));
                    } else {
                        negAssertionsAxioms.add(objectPropertyAssertionAxiom.get(i));
                    }
                }
            }
        });

        if (loader.isMultipleObservationOnInput()){
            assertionsAxioms.removeAll(loader.getObservation().getAxiomsInMultipleObservations());
            negAssertionsAxioms.removeAll(loader.getObservation().getAxiomsInMultipleObservations());
        } else {
            assertionsAxioms.remove(loader.getObservation().getOwlAxiom());
            negAssertionsAxioms.remove(loader.getObservation().getOwlAxiom());
        }

        Set<OWLAxiom> allLiterals = new HashSet<>();
        allLiterals.addAll(assertionsAxioms);
        allLiterals.addAll(negAssertionsAxioms);

        literals = new Literals(allLiterals);
        Set<OWLAxiom> to_abd = new HashSet<>();

        if (Configuration.NEGATION_ALLOWED){
            for (OWLAxiom ax : allLiterals){
                if (abducibles.getIndividuals().containsAll(ax.getIndividualsInSignature()) &&
                        abducibles.getClasses().containsAll(ax.getClassesInSignature()) &&
                        abducibles.getRoles().containsAll(ax.getObjectPropertiesInSignature())){
                    to_abd.add(ax);
                }
            }
        }
        else {
            for (OWLAxiom ax : assertionsAxioms){
                if (abducibles.getIndividuals().containsAll(ax.getIndividualsInSignature()) &&
                        abducibles.getClasses().containsAll(ax.getClassesInSignature()) &&
                        abducibles.getRoles().containsAll(ax.getObjectPropertiesInSignature())){
                    to_abd.add(ax);
                }
            }
        }

        abd_literals = new Literals(to_abd);
        if (abd_literals.getOwlAxioms().isEmpty()){
            if (Configuration.NEGATION_ALLOWED){
                abd_literals = literals;
            }
            else{
                abd_literals.addLiterals(assertionsAxioms);
            }
        }
    }

    private void startSolving() throws OWLOntologyStorageException, OWLOntologyCreationException {
        explanations = new LinkedList<>();
        path = new ArrayList<>();
        ICheckRules checkRules = new CheckRules(loader, reasonerManager);
        Integer currentDepth = 0;

        if(MHS_MODE){
            isOntologyConsistent();
        } else {
            Conflict conflict = getMergeConflict();
            for (Explanation e: conflict.getExplanations()){
                e.setDepth(e.getOwlAxioms().size());
            }
            explanations = conflict.getExplanations();
        }

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

        /*System.out.println("BERIEME PRVY MODEL ");
        for(OWLAxiom a : root.data){
            System.out.print(Printer.print(a) + " ");
        }
        System.out.println();*/

        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();

            if (node.depth > currentDepth){
                explanationsFilter.showExplanationsWithDepth(currentDepth, false);
                currentDepth++;
                if(MHS_MODE){
                    /*System.out.println("MHS MOD CESTY "+pathsInCertainDepth.size());
                    for(List<OWLAxiom> a: pathsInCertainDepth){
                        System.out.println(a);
                    }*/
                    pathsInCertainDepth = new ArrayList<>();
                }
            }

            if (Configuration.TIMEOUT != null && threadTimes.getTotalUserTimeInSec() > Configuration.TIMEOUT) {
                System.out.println("timeout");
                explanationsFilter.showExplanationsWithDepth(currentDepth + 1, true);
                currentDepth = null;
                break;
            }

            if (ModelNode.class.isAssignableFrom(node.getClass())) {
                ModelNode model = (ModelNode) node;

                if (model.depth.equals(Configuration.DEPTH)) {
                    break;
                }

                /*System.out.print("MODEL LABEL ");
                System.out.println(model.label);
                for(OWLAxiom a : model.label){
                    System.out.print(Printer.print(a) + " ");
                }
                System.out.println();

                System.out.print("BERIEME MODEL ");
                System.out.println(model.data);
                for(OWLAxiom a : model.data){
                    System.out.print(Printer.print(a) + " ");
                }
                System.out.println();*/

                for (OWLAxiom child : model.data){

                    if (Configuration.TIMEOUT != null && threadTimes.getTotalUserTimeInSec() > Configuration.TIMEOUT) {
                        break;
                    }

                    if (model.label.contains(AxiomManager.getComplementOfOWLAxiom(loader, child)) ||
                            child.equals(loader.getObservation().getOwlAxiom()) ||
                            (loader.isMultipleObservationOnInput() && loader.getObservation().getAxiomsInMultipleObservations().contains(child))){
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

                    System.out.print("PATH ");
                    for(OWLAxiom a : path){
                        System.out.print(Printer.print(a) + " ");
                    }
                    System.out.print(" - ");

                    boolean isMinimal = checkRules.isMinimal(explanations, explanation);
                    if (!isMinimal){
                        System.out.println("NOT MINIMAL");
                        path.clear();
                        continue;
                    }

                    if(MHS_MODE){
                        //check
                        boolean ch = false;
                        for(Set s : pathsInCertainDepth){
                            if(s.containsAll(path)){
                                /*for(OWLAxiom a : path){
                                    System.out.print(Printer.print(a));
                                }
                                System.out.println();*/
                                ch = true;
                                break;
                            }
                        }
                        if (ch){
                            //System.out.println("FILTRUJEM ");
                            System.out.println("EQUAL WITH OTHER PATH");
                            path.clear();
                            continue;
                        }
                        /*if (pathsInCertainDepth.contains(path)){
                            System.out.println("FILTRUJEM");
                            path.clear();
                            continue;
                        }*/
                        pathsInCertainDepth.add(new HashSet<>(path));
                    }

                    if (checkRules.isExplanation(explanation)){// zmenila som
                        //System.out.println("JE EXPL " + explanation.getOwlAxioms());
                        explanation.setDepth(explanation.getOwlAxioms().size());
                        //System.out.println("CHECKING " + CHECKING_MINIMALITY_BY_QXP);
                        if(CHECKING_MINIMALITY_BY_QXP){
                            if(isMinimalByCallingQXP(explanation)){
                                explanations.add(explanation);
                            } else {
                                explanations.add(foundedExplanation);
                                foundedExplanation = null;
                            }
                        } else {
                            if(MHS_MODE){
                                /*System.out.print("SEM PRISLO ");
                                for(OWLAxiom i : explanation.getOwlAxioms()){
                                    System.out.print(Printer.print(i) + " ");
                                }*/
                                System.out.println();
                                if(checkRules.isConsistent(explanation) && checkRules.isRelevant(explanation)){
                                    System.out.println("PRESLO");
                                    explanations.add(explanation);
                                } else {
                                    //System.out.println("NEPRESLO");
                                    System.out.println("NOT RELEVANT OR CONSISTENT");
                                    path.clear();
                                    continue;
                                }
                            } else {
                                explanations.add(explanation);
                            }
                        }
                        path.clear();
                        continue;
                    }

                    if (!REUSE_OF_MODELS || !usableModelInModels()) {
                        if (Configuration.TIMEOUT != null && threadTimes.getTotalUserTimeInSec() > Configuration.TIMEOUT) {
                            System.out.println("timeout");
                            explanationsFilter.showExplanationsWithDepth(currentDepth + 1, true);
                            currentDepth = null;
                            return;
                        }
                        if(MHS_MODE){
                            if(!isOntologyConsistent()){
                                //System.out.println("KONTROLUJE CI JE CONSISTENT A RELEVANT");
                                if(checkRules.isConsistent(explanation) && checkRules.isRelevant(explanation)){
                                    //System.out.println("preslo");
                                    System.out.println("PRESLO");
                                    explanations.add(explanation);
                                }
                                System.out.println("UZ NETREBALO POKRACOVAT");
                                path.clear();
                                continue;
                            }
                            //problem, ak zisti nieco?? ci asi ani nie... ?
                            //isOntologyConsistent();
                            /*if(!isOntologyConsistent()){
                                path.clear();
                                continue;
                            }*/
                        }else {
                            if (!addNewExplanations()){
                                path.clear();
                                continue;
                            }
                        }
                    }
                    else{
                        lenghtOneExplanations = new ArrayList<>();
                    }

                    ModelNode modelNode = new ModelNode();
                    modelNode.label = explanation.getOwlAxioms();
                    //System.out.println("VELKOST MODELOV " + negModels.size());
                    if (usableModelInModels()){
                        modelNode.data = negModels.get(lastUsableModelIndex).data;
                        /*System.out.println("NEG MODEL DATA: ");
                        for(OWLAxiom c : model.data){
                            System.out.print(Printer.print(c) + " ");
                        }*/
                        modelNode.data.removeAll(path);
                        /*System.out.println("   REMOVED NEG MODEL DATA: ");
                        for(OWLAxiom c : model.data){
                            System.out.print(Printer.print(c) + " ");
                        }
                        System.out.println();*/
                        if (modelNode.data.isEmpty()){
                            System.out.println("NOT MODEL IN ABD");
                            path.clear();
                            continue;
                        }
                    }
                    modelNode.depth = model.depth + 1;
                    modelNode.add_node_explanations(model);
                    if(!MHS_MODE){
                        modelNode.add_to_explanations(lenghtOneExplanations);       // TU JE CHYBA?
                    }
                    /*System.out.print("MODEL NODE NA KONCI ");
                    for(OWLAxiom a : modelNode.data){
                        System.out.print(Printer.print(a) + " ");
                    }
                    System.out.println();*/

                    queue.add(modelNode);
                    System.out.println();
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

            if (explanations.contains(CS)) {
                break;
            }

            if(CHECKING_MINIMALITY_BY_QXP){
                if(isMinimalByCallingQXP(CS)){
                    explanations.add(CS);
                } else {
                    explanations.add(foundedExplanation);
                    foundedExplanation = null;
                }
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

        if(checkingMinimalityWithQXP) pathDuringCheckingMinimality.addAll(sets.get(0).getOwlAxioms());
        else path.addAll(sets.get(0).getOwlAxioms());

        Explanation D2 = getConflict(sets.get(0).getOwlAxioms(), sets.get(1));

        if(checkingMinimalityWithQXP) pathDuringCheckingMinimality.removeAll(sets.get(0).getOwlAxioms());
        else path.removeAll(sets.get(0).getOwlAxioms());


        if(checkingMinimalityWithQXP) pathDuringCheckingMinimality.addAll(D2.getOwlAxioms());
        else path.addAll(D2.getOwlAxioms());

        Explanation D1 = getConflict(D2.getOwlAxioms(), sets.get(0));

        if(checkingMinimalityWithQXP) pathDuringCheckingMinimality.removeAll(D2.getOwlAxioms());
        else path.removeAll(D2.getOwlAxioms());

        Set<OWLAxiom> conflicts = new HashSet<>();
        conflicts.addAll(D1.getOwlAxioms());
        conflicts.addAll(D2.getOwlAxioms());

        return new Explanation(conflicts, conflicts.size(), threadTimes.getTotalUserTimeInSec());
    }

    private List<Literals> divideIntoSets(Literals literals) {
        if(CACHED_CONFLICTS_LONGEST_CONFLICT && explanations.size() > 0){
            return divideIntoSetsAccordingTheLongestConflict(literals);
        } else if (CACHED_CONFLICTS_TABLE_OF_OCCURRENCE){
            return divideIntoSetsAccordingTableOfLiteralsPairOccurrence(literals);
        }
        return divideIntoSetsWithoutCondition(literals);
    }

    private List<Literals> divideIntoSetsAccordingTheLongestConflict(Literals literals){
        Explanation theLongestExplanation = getTheLongestExplanation();
        Set<OWLAxiom> axiomsFromExplanation = new HashSet<>(theLongestExplanation.getOwlAxioms());

        List<Literals> dividedLiterals = new ArrayList<>();
        dividedLiterals.add(new Literals());
        dividedLiterals.add(new Literals());

        int count = 0;
        for(OWLAxiom owlAxiom : axiomsFromExplanation){
            if(literals.getOwlAxioms().contains(owlAxiom)){
                dividedLiterals.get(count % 2).getOwlAxioms().add(owlAxiom);
                count++;
            }
        }

        for(OWLAxiom owlAxiom : literals.getOwlAxioms()) {
            if(!axiomsFromExplanation.contains(owlAxiom)){
                dividedLiterals.get(count % 2).getOwlAxioms().add(owlAxiom);
                count++;
            }
        }
        return dividedLiterals;
    }

    private Explanation getTheLongestExplanation(){
        int index = 0;
        int length = 0;
        for(int i = 0; i < explanations.size(); i++){
            if(explanations.get(i).getDepth() > length){
                index = i;
            }
        }
        return explanations.get(index);
    }

    private List<Literals> divideIntoSetsAccordingTableOfLiteralsPairOccurrence(Literals literals){
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

    private List<Literals> divideIntoSetsWithoutCondition(Literals literals){
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
                if(CHECKING_MINIMALITY_BY_QXP){
                    if(isMinimalByCallingQXP(conflict)){
                        conflict.setDepth(conflict.getOwlAxioms().size());
                        explanations.add(conflict);
                    } else {
                        explanations.add(foundedExplanation);
                        foundedExplanation = null;
                    }
                } else {
                    conflict.setDepth(conflict.getOwlAxioms().size());
                    explanations.add(conflict);
                }
            }
        }
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
            return (!modelExtractor.getNegModelByReasoner().data.isEmpty());
        }
        return (!modelExtractor.getNegModelByOntology().data.isEmpty());
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
        System.out.println("CHECK " + explanation.getOwlAxioms());
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
        foundedExplanation = newExplanation;
        return false;
    }

    public void removeAxiomsFromOntology(List<OWLAxiom> axioms){
        reasonerManager.resetOntology(loader.getOriginalOntology().axioms());
    }







}