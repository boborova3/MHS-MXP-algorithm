package apiImplementation;

import abductionapi.abducibles.AbducibleContainer;
import abductionapi.exception.InvalidObservationException;
import abductionapi.exception.InvalidSolverSettingException;
import abductionapi.exception.MultiObservationException;
import abductionapi.manager.AbductionManager;
import abductionapi.manager.ExplanationWrapper;
import abductionapi.manager.MultiObservationManager;
import abductionapi.manager.ThreadAbductionManager;
import abductionapi.monitors.AbductionMonitor;
import algorithms.hybrid.ApiExplanationManager;
import algorithms.hybrid.HybridSolver;
import common.Configuration;
import fileLogger.FileLogger;
import models.Explanation;
import org.semanticweb.owlapi.model.*;
import progress.ApiProgressManager;
import reasoner.*;
import timer.ThreadTimes;

import java.util.*;
import java.util.stream.Collectors;

public class HybridAbductionManager implements
        AbductionManager,
        MultiObservationManager,
        ThreadAbductionManager {

    HybridAbducibleContainer abducibles;
    OWLOntology ontology;
    Set<OWLAxiom> observations;
    double timeout = 0;
    int depth = 0;
    boolean pureMHS = false;
    Set<ExplanationWrapper> explanations;
    AbductionMonitor abductionMonitor;
    HybridSolver solver;
    ApiLoader loader;
    ReasonerManager reasonerManager;
    ThreadTimes timer;

    HybridAbductionManager(){
        FileLogger.initializeLogger();
    }

    public void setExplanations(Collection<Explanation> explanations){
        this.explanations = explanations.stream()
                                        .map(Explanation::createExplanationWrapper)
                                        .collect(Collectors.toSet());
    }

    @Override
    public void setKnowledgeBase(OWLOntology ontology) {
        this.ontology = ontology;
    }

    @Override
    public OWLOntology getKnowledgeBase() {
        return ontology;
    }

    @Override
    public void setObservation(OWLAxiom axiom) throws MultiObservationException, InvalidObservationException {
        if (checkObservationType(axiom))
            observations = Collections.singleton(axiom);
        else throwInvalidObservationException(axiom);
    }

    private void throwInvalidObservationException(OWLAxiom axiom){
        throw new InvalidObservationException(
                "Axiom " + axiom + " has invalid type for an observation: " + axiom.getAxiomType()
        );
    }

    private boolean checkObservationType(OWLAxiom axiom){
        AxiomType<?> type = axiom.getAxiomType();
        return  AxiomType.CLASS_ASSERTION == type ||
                AxiomType.OBJECT_PROPERTY_ASSERTION == type ||
                AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION == type;
    }

    @Override
    public void setMultipleObservations(Set<OWLAxiom> observation) throws InvalidObservationException {
        observation.forEach(this::addSingleObservation);
    }

    private void addSingleObservation(OWLAxiom axiom) throws MultiObservationException, InvalidObservationException {
        if (checkObservationType(axiom))
            observations.add(axiom);
        else throwInvalidObservationException(axiom);
    }

    @Override
    public OWLAxiom getObservation() throws MultiObservationException {
        if (observations.isEmpty())
            return null;
        if (observations.size() > 1)
            throw new MultiObservationException("There are multiple observations in this abduction manager.");
        else return new ArrayList<>(observations).get(0);
    }

    @Override
    public Set<OWLAxiom> getMultipleObservations() {
        return observations;
    }

    @Override
    public void setTimeout(double seconds) {
        timeout = seconds;
    }

    @Override
    public double getTimeout() {
        return timeout;
    }

    @Override
    public void setAdditionalSolverSettings(String s) {
        String[] arguments = s.split(" ");
            for (int i = 0; i < arguments.length; i++){
                try {
                    switch (arguments[i]) {
                        case "-d":
                            depth = Integer.parseInt(arguments[i + 1]);
                            i++;
                            continue;
                        case "-mhs":
                            pureMHS = Boolean.parseBoolean(arguments[i + 1]);
                            i++;
                            continue;
                        default:
                            throw new InvalidSolverSettingException("Unknown solver argument:" + arguments[i]);
                    }
                } catch(NumberFormatException e){
                    throw new InvalidSolverSettingException("Invalid integer value:" + arguments[i+1]);
                }
            }
    }

    @Override
    public Set<ExplanationWrapper> getExplanations() {

        setupSolver();
        try {
            solver.solve(loader, reasonerManager);
        } catch (OWLOntologyCreationException | OWLOntologyStorageException e) {
            e.printStackTrace();
        }
        return explanations;
    }

    private void setupSolver(){

        loader = new ApiLoader(this);

        try {
            loader.initialize(ReasonerType.JFACT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        reasonerManager = new ReasonerManager(loader);

        ApiExplanationManager explanationManager = new ApiExplanationManager(loader, reasonerManager, this);
        ApiProgressManager progressManager = new ApiProgressManager(this);

        timer = new ThreadTimes(100);
        timer.start();
        long currentTimeMillis = System.currentTimeMillis();

        solver = new HybridSolver(timer, currentTimeMillis, explanationManager, progressManager);

        setSolverConfiguration();

    }

    private void setSolverConfiguration(){
        if (depth > 0) Configuration.DEPTH = depth;
        Configuration.MHS_MODE = pureMHS;
        if (timeout > 0) Configuration.TIMEOUT = (long) timeout;
    }


    @Override
    public void getExplanationsIncrementally() {
        ThreadAbductionManager.super.getExplanationsIncrementally();
    }

    @Override
    public String getOutputAdditionalInfo() {
        return null;
    }

    @Override
    public void setAbducibles(AbducibleContainer abducibles) {
        if (! (abducibles instanceof HybridAbducibleContainer))
            return;
        this.abducibles = (HybridAbducibleContainer) abducibles;
    }

    @Override
    public HybridAbducibleContainer getAbducibles() {
        return abducibles;
    }

    @Override
    public void run() {}

    @Override
    public AbductionMonitor getAbductionMonitor() {
        return abductionMonitor;
    }

}
