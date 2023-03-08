package algorithms.hybrid;

import algorithms.ISolver;
import com.google.common.collect.Iterables;
import common.Configuration;

import common.Printer;
import models.Abducibles;
import models.Explanation;
import models.Literals;
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

    private ILoader loader;
    private IReasonerManager reasonerManager;
    private Literals abd_literals;
    private ModelExtractor modelExtractor;
    private ExplanationsFilter explanationsFilter;
    private SetDivider setDivider;
    private List<OWLAxiom> lenghtOneExplanations = new ArrayList<>();
    private Set<Set<OWLAxiom>> pathsInCertainDepth = new HashSet<>();

    public OWLOntology ontology;
    public List<ModelNode> models;
    public List<ModelNode> negModels;
    public List<OWLAxiom> assertionsAxioms;
    public List<OWLAxiom> negAssertionsAxioms;
    public List<Explanation> possibleExplanations = new LinkedList<>();
    public Set<OWLAxiom> path = new HashSet<>();
    //public List<OWLAxiom> path;
    public Set<OWLAxiom> pathDuringCheckingMinimality;
    public Abducibles abducibles;
    public int lastUsableModelIndex;
    public OWLAxiom negObservation;
    public ThreadTimes threadTimes;
    public long currentTimeMillis;
    public Map<Integer, Double> level_times = new HashMap<>();
    public boolean checkingMinimalityWithQXP = false;
    private ICheckRules checkRules;
    private Integer currentDepth;

    public HybridSolver(ThreadTimes threadTimes, long currentTimeMillis) {
        System.out.println();
        System.out.println(String.join("\n", getInfo()));
        System.out.println();

        this.threadTimes = threadTimes;
        this.currentTimeMillis = currentTimeMillis;
    }

    public List<String> getInfo() {
        String optimizationQXP = "Optimization QXP: " + Configuration.CHECKING_MINIMALITY_BY_QXP;
        String optimizationLongestConf = "Optimization Cached Conflicts - The Longest Conflict: " + Configuration.CACHED_CONFLICTS_LONGEST_CONFLICT;
        String optimizationMedian = "Optimization Cached Conflicts - Median: " + Configuration.CACHED_CONFLICTS_MEDIAN;
        String roles = "Roles: " + Configuration.ROLES_IN_EXPLANATIONS_ALLOWED;
        String looping = "Looping allowed: " + Configuration.LOOPING_ALLOWED;
        String negation = "Negation: " +  Configuration.NEGATION_ALLOWED;
        String mhs_mode = "MHS MODE: " + Configuration.MHS_MODE;
        String relevance = "Strict relevance: " + Configuration.STRICT_RELEVANCE;
        String depth = "Depth limit: ";
        if (Configuration.DEPTH != null) depth += Configuration.DEPTH; else depth += "none";
        String timeout = "Timeout: ";
        if (Configuration.TIMEOUT != null) timeout += Configuration.TIMEOUT; else timeout += "none";

        return Arrays.asList(optimizationQXP, optimizationLongestConf, optimizationMedian,
                roles, looping, negation, mhs_mode, relevance, depth, timeout);
    }

    @Override
    public void solve(ILoader loader, IReasonerManager reasonerManager) throws OWLOntologyStorageException, OWLOntologyCreationException {
        this.loader = loader;
        this.reasonerManager = reasonerManager;
        this.ontology = this.loader.getOriginalOntology();
        this.modelExtractor = new ModelExtractor(loader, reasonerManager, this);
        this.explanationsFilter = new ExplanationsFilter(loader, reasonerManager, this);
        this.setDivider = new SetDivider(this);
        this.checkRules = new CheckRules(loader, reasonerManager);

        negObservation = loader.getNegObservation().getOwlAxiom();
        this.abducibles = loader.getAbducibles();

        initialize();

        String message;
        if (!reasonerManager.isOntologyConsistent()) {
            message = "MESSAGE: nothing to explain";
            makeFinalLog(message);
        }
//        else if (reasonerManager.isOntologyWithLiteralsConsistent(abd_literals.getOwlAxioms(), ontology)) { //tato pociatocna podmienka v MHS-MXP algoritme nevystupuje
//            message = "MESSAGE: no conflicts, consistent with abducibles";
//            makeFinalLog(message);
//        }
        else {
            trySolve();
        }
    }

    private void trySolve() throws OWLOntologyStorageException, OWLOntologyCreationException {
        try {
            startSolving();
        } catch (Throwable e) {
            makeErrorAndPartialLog(e);
            throw e;
        } finally {
            makeFinalLog(null);
        }
    }

    private void makeErrorAndPartialLog(Throwable e) {
        explanationsFilter.showError(e);

        Double time = threadTimes.getTotalUserTimeInSec();
        level_times.put(currentDepth, time);
        explanationsFilter.showExplanationsWithDepth(currentDepth, false, true, time);
        if(!Configuration.MHS_MODE){
            explanationsFilter.showExplanationsWithDepth(currentDepth + 1, false, true, time);
            explanationsFilter.showExplanationsWithLevel(currentDepth, false, true, time);
        }
    }

    private void makeFinalLog(String message) throws OWLOntologyStorageException, OWLOntologyCreationException {
        explanationsFilter.showExplanations();
        explanationsFilter.showMessages(getInfo(), message);

        if (message != null) {
            System.out.println();
            System.out.println(message);
        }
    }

    private void initialize() {
        models = new ArrayList<>();
        negModels = new ArrayList<>();

        assertionsAxioms = new ArrayList<>();
        negAssertionsAxioms = new ArrayList<>();

        loader.getOntologyManager().addAxiom(ontology, loader.getNegObservation().getOwlAxiom());
        reasonerManager.addAxiomToOntology(loader.getNegObservation().getOwlAxiom());

        if(loader.isAxiomBasedAbduciblesOnInput()){
            Set<OWLAxiom> abduciblesWithoutObservation = abducibles.getAxiomBasedAbducibles();
            if (loader.isMultipleObservationOnInput()){
                if (Configuration.STRICT_RELEVANCE) {
                    abduciblesWithoutObservation.removeAll(loader.getObservation().getAxiomsInMultipleObservations());
                }
            } else {
                abduciblesWithoutObservation.remove(loader.getObservation().getOwlAxiom());
            }
            abd_literals = new Literals(abduciblesWithoutObservation);
            return;
        }

        for(OWLClass owlClass : abducibles.getClasses()){
            if (owlClass.isTopEntity() || owlClass.isBottomEntity()) continue;
            List<OWLAxiom> classAssertionAxiom = AxiomManager.createClassAssertionAxiom(loader, owlClass);
            for (int i = 0; i < classAssertionAxiom.size(); i++) {
                if (i % 2 == 0) {
                    assertionsAxioms.add(classAssertionAxiom.get(i));
                } else {
                    negAssertionsAxioms.add(classAssertionAxiom.get(i));
                }
            }
        }

        if(Configuration.ROLES_IN_EXPLANATIONS_ALLOWED){
            for(OWLObjectProperty objectProperty : abducibles.getRoles()){
                if (objectProperty.isTopEntity() || objectProperty.isBottomEntity()) continue;
                List<OWLAxiom> objectPropertyAssertionAxiom = AxiomManager.createObjectPropertyAssertionAxiom(loader, objectProperty);
                for (int i = 0; i < objectPropertyAssertionAxiom.size(); i++) {
                    if (i % 2 == 0) {
                        assertionsAxioms.add(objectPropertyAssertionAxiom.get(i));
                    } else {
                        negAssertionsAxioms.add(objectPropertyAssertionAxiom.get(i));
                    }
                }
            }
        }

        if (loader.isMultipleObservationOnInput()){
            if (Configuration.STRICT_RELEVANCE) {
                assertionsAxioms.removeAll(loader.getObservation().getAxiomsInMultipleObservations());
                negAssertionsAxioms.removeAll(loader.getObservation().getAxiomsInMultipleObservations());
            }
        } else {
            assertionsAxioms.remove(loader.getObservation().getOwlAxiom());
            negAssertionsAxioms.remove(loader.getObservation().getOwlAxiom());
        }

        Set<OWLAxiom> to_abd = new HashSet<>();

        if(Configuration.NEGATION_ALLOWED){
            to_abd.addAll(assertionsAxioms);
            to_abd.addAll(negAssertionsAxioms);
        } else {
            to_abd.addAll(assertionsAxioms);
        }

        abd_literals = new Literals(to_abd);
    }

    private void startSolving() throws OWLOntologyStorageException, OWLOntologyCreationException {
        currentDepth = 0;

        Queue<TreeNode> queue = new LinkedList<>();
        initializeTree(queue);

        if (isTimeout()) {
            makeTimeoutPartialLog();
            return;
        }

        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();

            if(increaseDepth(node)){
                currentDepth++;
            }
            if(isTimeout() || !ModelNode.class.isAssignableFrom(node.getClass())){
                makeTimeoutPartialLog();
                break;
            }

            ModelNode model = (ModelNode) node;
            if (model.depth.equals(Configuration.DEPTH)) {
                break;
            }

//            System.out.println("MODEL (data)");
//            System.out.println(Printer.print(new ArrayList<>(model.data)));
//            System.out.println("PATH (label)");
//            System.out.println(Printer.print(new ArrayList<>(model.label)));

            for (OWLAxiom child : model.data){

                //System.out.println("Child " + child);

                if(isTimeout()){
                    makeTimeoutPartialLog();
                    return;
                }

                if(isIncorrectPath(model, child)){
                    continue;
                }

                Explanation explanation = new Explanation();
                explanation.addAxioms(model.label);
                explanation.addAxiom(child);
                explanation.setAcquireTime(threadTimes.getTotalUserTimeInSec());
                explanation.setLevel(currentDepth);
                //explanation.setLevel(currentDepth + 1);

                path = new HashSet<>(explanation.getOwlAxioms());

                if(canBePruned(explanation)){
                    //System.out.println("IS PRUNED");
                    path.clear();
                    continue;
                }

                if (!Configuration.REUSE_OF_MODELS || !usableModelInModels()) {
                    if(isTimeout()){
                        makeTimeoutPartialLog();
                        return;
                    }
                    if(Configuration.MHS_MODE){
                        if(!isOntologyConsistent()){
                            explanation.setDepth(explanation.getOwlAxioms().size());
                            possibleExplanations.add(explanation);
                            //System.out.println("IDE O VYSVETLENIE");
                            path.clear();
                            continue;
                        }
                    } else {
                        if (!addNewExplanations()){
                            path.clear();
                            if (isTimeout()) {
                                makeTimeoutPartialLog();
                                return;
                            }
                            continue;
                        }
                        if (isTimeout()) {
                            makeTimeoutPartialLog();
                            return;
                        }
                    }
                }
                else{
                    lenghtOneExplanations = new ArrayList<>();
                }
                addNodeToTree(queue, explanation, model);
            }
        }
        path.clear();

        if(!level_times.containsKey(currentDepth)){
            //level_times.put(currentDepth, threadTimes.getTotalUserTimeInSec());
            makePartialLog();
        }
        currentDepth = 0;
    }

    private void makePartialLog() {
        Double time = threadTimes.getTotalUserTimeInSec();
        level_times.put(currentDepth, time);
        explanationsFilter.showExplanationsWithDepth(currentDepth, false, false, time);
        if(!Configuration.MHS_MODE){
            explanationsFilter.showExplanationsWithLevel(currentDepth, false, false, time);
        }
        pathsInCertainDepth = new HashSet<>();
    }

    private void initializeTree(Queue<TreeNode> queue) throws OWLOntologyStorageException {
        if(Configuration.MHS_MODE){
            if(!isOntologyConsistent()){
                return;
            }
        } else {
            Conflict conflict = getMergeConflict();
            for (Explanation e: conflict.getExplanations()){
                e.setDepth(e.getOwlAxioms().size());
            }
            possibleExplanations = conflict.getExplanations();
        }

        ModelNode root = createModelNodeFromExistingModel(true, null, null);
        if(root == null){
            return;
        }
        queue.add(root);
    }

    private void addToExplanations(Explanation explanation){
        explanation.setDepth(explanation.getOwlAxioms().size());
        if(Configuration.CHECKING_MINIMALITY_BY_QXP){
            Explanation newExplanation = getMinimalExplanationByCallingQXP(explanation);
            possibleExplanations.add(newExplanation);
            if(Configuration.CACHED_CONFLICTS_MEDIAN){
                setDivider.addPairsOfLiteralsToTable(newExplanation);
            }
        } else {
            possibleExplanations.add(explanation);
            if(Configuration.CACHED_CONFLICTS_MEDIAN){
                setDivider.addPairsOfLiteralsToTable(explanation);
            }
        }
    }

    private void addNodeToTree(Queue<TreeNode> queue, Explanation explanation, ModelNode model){
        ModelNode modelNode = createModelNodeFromExistingModel(false, explanation, model.depth + 1);
        if(modelNode == null){
            path.clear();
            return;
        }
        if(!Configuration.MHS_MODE){
            modelNode.add_node_explanations(model);
            modelNode.add_to_explanations(lenghtOneExplanations);
        }
        queue.add(modelNode);
        path.clear();
    }

    private ModelNode createModelNodeFromExistingModel(boolean isRoot, Explanation explanation, Integer depth){
        ModelNode modelNode = new ModelNode();
        if (usableModelInModels()){
            if(isRoot){
                modelNode.data = negModels.get(lastUsableModelIndex).data;
                modelNode.label = new LinkedList<>();
                modelNode.depth = 0;
            } else {
                modelNode.label = explanation.getOwlAxioms();
                modelNode.data = negModels.get(lastUsableModelIndex).data;
                modelNode.data.removeAll(path);
                modelNode.depth = depth;
            }
        }
        if(modelNode.data == null || modelNode.data.isEmpty()){
            return null;
        }
        return modelNode;
    }

    private boolean increaseDepth(TreeNode node){
        if (node.depth > currentDepth){
            makePartialLog();
            return true;
        }
        return false;
    }

    private boolean isTimeout(){
        if (Configuration.TIMEOUT != null && threadTimes.getTotalUserTimeInSec() > Configuration.TIMEOUT) {
            System.out.println("timeout");
            //makeTimeoutPartialLog();
            return true;
        }
        return false;
    }

    private void makeTimeoutPartialLog() {
        Double time = threadTimes.getTotalUserTimeInSec();
        level_times.put(currentDepth, time);
        explanationsFilter.showExplanationsWithDepth(currentDepth, true, false, time);
        if(!Configuration.MHS_MODE){
            explanationsFilter.showExplanationsWithDepth(currentDepth + 1, true, false, time);
            explanationsFilter.showExplanationsWithLevel(currentDepth, true,false, time);
        }
    }

    private boolean canBePruned(Explanation explanation) throws OWLOntologyCreationException {
        if (!checkRules.isMinimal(possibleExplanations, explanation)){
            return true;
        }
        if(pathsInCertainDepth.contains(path)){
            return true;
        }
        pathsInCertainDepth.add(new HashSet<>(path));

        if(Configuration.CHECK_RELEVANCE_DURING_BUILDING_TREE_IN_MHS_MXP){
            if(!checkRules.isRelevant(explanation)){
                return true;
            }
        }

        if(Configuration.MHS_MODE){
            if(!checkRules.isRelevant(explanation)){
                return true;
            }
            if(!checkRules.isConsistent(explanation)){
                return true;
            }
        }

        if(!Configuration.MHS_MODE){
            if (checkRules.isExplanation(explanation)){
                addToExplanations(explanation);
                return true;
            }
        }
        return false;
    }

    private boolean isIncorrectPath(ModelNode model, OWLAxiom child){
        if (model.label.contains(AxiomManager.getComplementOfOWLAxiom(loader, child)) ||
                child.equals(loader.getObservation().getOwlAxiom())){
            return true;
        }

        if (!abd_literals.contains(child)){
            return true;
        }

        return false;
    }

    private Conflict getMergeConflict() {
        return findConflicts(abd_literals);
    }

    private List<Explanation> findExplanations(){
        abd_literals.removeLiterals(path);
        abd_literals.removeLiterals(lenghtOneExplanations);
        if(Configuration.CACHED_CONFLICTS_LONGEST_CONFLICT){
            setDivider.setIndexesOfExplanations(possibleExplanations.size());
        }
        Conflict conflict = findConflicts(abd_literals);
        abd_literals.addLiterals(path);
        abd_literals.addLiterals(lenghtOneExplanations);
        return conflict.getExplanations();
    }

    private Conflict findConflicts(Literals literals) {
        path.remove(negObservation);
        reasonerManager.addAxiomsToOntology(path);

        if (isTimeout()) {
            return new Conflict(new Literals(), new LinkedList<>());
        }

        if (isOntologyWithLiteralsConsistent(literals.getOwlAxioms())) {
            return new Conflict(literals, new LinkedList<>());
        }
        resetOntologyToOriginal();
        if (literals.getOwlAxioms().size() == 1) {
            List<Explanation> explanations = new LinkedList<>();
            explanations.add(new Explanation(literals.getOwlAxioms(), literals.getOwlAxioms().size(), currentDepth, threadTimes.getTotalUserTimeInSec()));
            return new Conflict(new Literals(), explanations);
        }

        int indexOfExplanation = -1;
        if(Configuration.CACHED_CONFLICTS_LONGEST_CONFLICT){
            indexOfExplanation = setDivider.getIndexOfTheLongestAndNotUsedConflict();
        }

        List<Literals> sets = setDivider.divideIntoSets(literals);
        double median = setDivider.getMedian();

        Conflict conflictC1 = findConflicts(sets.get(0));
        if(Configuration.CACHED_CONFLICTS_LONGEST_CONFLICT){
            setDivider.addIndexToIndexesOfExplanations(indexOfExplanation);
        } else if(Configuration.CACHED_CONFLICTS_MEDIAN){
            setDivider.setMedian(median);
        }

//        if (isTimeout()) {
//            return new Conflict(new Literals(), new LinkedList<>());
//        }

        Conflict conflictC2 = findConflicts(sets.get(1));
        if(Configuration.CACHED_CONFLICTS_LONGEST_CONFLICT){
            setDivider.addIndexToIndexesOfExplanations(indexOfExplanation);
        } else if(Configuration.CACHED_CONFLICTS_MEDIAN){
            setDivider.setMedian(median);
        }

//        if (isTimeout()) {
//            return new Conflict(new Literals(), new LinkedList<>());
//        }

        List<Explanation> explanations = new LinkedList<>();
        explanations.addAll(conflictC1.getExplanations());
        explanations.addAll(conflictC2.getExplanations());

        Literals conflictLiterals = new Literals();
        conflictLiterals.getOwlAxioms().addAll(conflictC1.getLiterals().getOwlAxioms());
        conflictLiterals.getOwlAxioms().addAll(conflictC2.getLiterals().getOwlAxioms());

        while (!isOntologyWithLiteralsConsistent(conflictLiterals.getOwlAxioms())) {
            if (isTimeout()) break;
            path.addAll(conflictC2.getLiterals().getOwlAxioms());
            Explanation X = getConflict(conflictC2.getLiterals().getOwlAxioms(), conflictC1.getLiterals(), path);
            path.removeAll(conflictC2.getLiterals().getOwlAxioms());

            path.addAll(X.getOwlAxioms());
            Explanation CS = getConflict(X.getOwlAxioms(), conflictC2.getLiterals(), path);
            path.removeAll(X.getOwlAxioms());

            CS.getOwlAxioms().addAll(X.getOwlAxioms());

            conflictLiterals.getOwlAxioms().removeAll(conflictC1.getLiterals().getOwlAxioms());
            X.getOwlAxioms().stream().findFirst().ifPresent(axiom -> conflictC1.getLiterals().getOwlAxioms().remove(axiom));
            conflictLiterals.getOwlAxioms().addAll(conflictC1.getLiterals().getOwlAxioms());

            if (explanations.contains(CS) || isTimeout()) {
                break;
            }

            Explanation newExplanation = CS;
            if(Configuration.CHECKING_MINIMALITY_BY_QXP){
                newExplanation = getMinimalExplanationByCallingQXP(CS);
            }
            explanations.add(newExplanation);
            if(Configuration.CACHED_CONFLICTS_MEDIAN){
                setDivider.addPairsOfLiteralsToTable(newExplanation);
            }
        }

        return new Conflict(conflictLiterals, explanations);
    }

    private Explanation getConflict(Collection<OWLAxiom> axioms, Literals literals, Set<OWLAxiom> actualPath) {
        if (isTimeout()) {
            return new Explanation();
        }
        if (!axioms.isEmpty() && !isOntologyConsistent()) {
            return new Explanation();
        }

        if (literals.getOwlAxioms().size() == 1) {
            return new Explanation(literals.getOwlAxioms(), 1, currentDepth, threadTimes.getTotalUserTimeInSec());
        }

        List<Literals> sets = setDivider.divideIntoSetsWithoutCondition(literals);

        actualPath.addAll(sets.get(0).getOwlAxioms());
        Explanation D2 = getConflict(sets.get(0).getOwlAxioms(), sets.get(1), actualPath);
        actualPath.removeAll(sets.get(0).getOwlAxioms());

        actualPath.addAll(D2.getOwlAxioms());
        Explanation D1 = getConflict(D2.getOwlAxioms(), sets.get(0), actualPath);
        actualPath.removeAll(D2.getOwlAxioms());

        Set<OWLAxiom> conflicts = new HashSet<>();
        conflicts.addAll(D1.getOwlAxioms());
        conflicts.addAll(D2.getOwlAxioms());

        return new Explanation(conflicts, conflicts.size(), currentDepth, threadTimes.getTotalUserTimeInSec());
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
            if (checkRules.isMinimal(possibleExplanations, conflict)){
                Explanation newExplanation = conflict;
                if(Configuration.CHECKING_MINIMALITY_BY_QXP){
                    newExplanation = getMinimalExplanationByCallingQXP(conflict);
                }
                newExplanation.setDepth(newExplanation.getOwlAxioms().size());
                possibleExplanations.add(newExplanation);
                if(Configuration.CACHED_CONFLICTS_MEDIAN){
                    setDivider.addPairsOfLiteralsToTable(newExplanation);
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
        return modelExtractor.getNegModelByOntology().modelIsValid;
    }

    public Explanation getMinimalExplanationByCallingQXP(Explanation explanation){
        Set<OWLAxiom> temp = new HashSet<>();
        temp.addAll(explanation.getOwlAxioms());
        if(path != null){
            temp.addAll(path);
        }
        Literals potentialExplanations = new Literals(temp);

        checkingMinimalityWithQXP = true;
        pathDuringCheckingMinimality = new HashSet<>();
        Explanation newExplanation = getConflict(new ArrayList<>(), potentialExplanations, pathDuringCheckingMinimality);
        checkingMinimalityWithQXP = false;
        pathDuringCheckingMinimality = new HashSet<>();

        return newExplanation;
    }

    public void resetOntologyToOriginal(){
        reasonerManager.resetOntology(loader.getOriginalOntology().axioms());
    }

}