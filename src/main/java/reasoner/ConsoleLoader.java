package reasoner;

import common.Configuration;
import common.LogMessage;
import models.Abducibles;
import models.Individuals;
import models.Observation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.knowledgeexploration.OWLKnowledgeExplorerReasoner;
import parser.*;
import uk.ac.manchester.cs.jfact.JFactFactory;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

public class ConsoleLoader extends Loader {

    @Override
    public void initialize(ReasonerType reasonerType) throws Exception {
        loadReasoner(reasonerType);
        loadObservation();
        loadPrefixes();
        loadAbducibles();
    }

    @Override
    protected void setupOntology() throws OWLOntologyCreationException {
        ontology = ontologyManager.loadOntologyFromOntologyDocument(new File(Configuration.INPUT_ONT_FILE));
        originalOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File(Configuration.INPUT_ONT_FILE));
        initialOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File(Configuration.INPUT_ONT_FILE));
    }

    @Override
    public void changeReasoner(ReasonerType reasonerType) {
        // Note: we only use JFact for now

//        switch (reasonerType) {
//            case PELLET:
//                setOWLReasonerFactory(new OpenlletReasonerFactory());
//                break;
//
//            case HERMIT:
//                setOWLReasonerFactory(new ReasonerFactory());
//                break;
//
//            case JFACT:
//                setOWLReasonerFactory(new JFactFactory());
//                break;
//        }

        setOWLReasonerFactory(new JFactFactory());
        reasoner = (OWLKnowledgeExplorerReasoner) reasonerFactory.createReasoner(ontology);
        logger.log(Level.INFO, LogMessage.INFO_ONTOLOGY_LOADED);
    }

    @Override
    public void initializeReasoner() {
        reasoner.flush();
    }

    @Override
    protected void loadObservation() throws Exception {
        namedIndividuals = new Individuals();

        IObservationParser observationParser = new ConsoleObservationParser(this);
        observationParser.parse();
    }

    @Override
    protected void loadPrefixes(){
        PrefixesParser prefixesParser = new PrefixesParser(observationOntologyFormat);
        prefixesParser.parse();
    }

    @Override
    protected void loadAbducibles(){
        AbduciblesParser abduciblesParser = new AbduciblesParser(this);
        abducibles = abduciblesParser.parse();
    }

    public Abducibles getAbducibles(){
        return abducibles;
    }

    @Override
    public Observation getObservation() {
        return observation;
    }

    @Override
    public void setObservation(OWLAxiom observation) {
        this.observation = new Observation(observation);
    }

    @Override
    public void setObservation(OWLAxiom observation, List<OWLAxiom> axiomsInMultipleObservations, OWLNamedIndividual reductionIndividual){
        this.observation = new Observation(observation, axiomsInMultipleObservations, reductionIndividual);
    }

    @Override
    public Observation getNegObservation() {
        return negObservation;
    }

    @Override
    public void setNegObservation(OWLAxiom negObservation) {
        this.negObservation = new Observation(negObservation);
    }

    @Override
    public OWLOntologyManager getOntologyManager() {
        return ontologyManager;
    }

    @Override
    public OWLOntology getOntology() {
        return ontology;
    }

    @Override
    public OWLKnowledgeExplorerReasoner getReasoner() {
        return reasoner;
    }

    @Override
    public void setOWLReasonerFactory(OWLReasonerFactory reasonerFactory) {
        this.reasonerFactory = reasonerFactory;
    }

    @Override
    public String getOntologyIRI() {
        if (ontologyIRI == null) {
            ontologyIRI = ontology.getOntologyID().getOntologyIRI().get().toString();
        }
        return ontologyIRI;
    }

    @Override
    public OWLDataFactory getDataFactory() {
        return ontologyManager.getOWLDataFactory();
    }

    @Override
    public Individuals getIndividuals() {
        return namedIndividuals;
    }

    @Override
    public void addNamedIndividual(OWLNamedIndividual namedIndividual) {
        namedIndividuals.addNamedIndividual(namedIndividual);
    }

    @Override
    public OWLOntology getOriginalOntology() {
        return originalOntology;
    }

    @Override
    public OWLOntology getInitialOntology() {
        return initialOntology;
    }


    public void setObservationOntologyFormat(OWLDocumentFormat observationOntologyFormat) {
        this.observationOntologyFormat = observationOntologyFormat;
    }

    public boolean isMultipleObservationOnInput() {
        return isMultipleObservationOnInput;
    }

    public void setMultipleObservationOnInput(boolean multipleObservationOnInput) {
        isMultipleObservationOnInput = multipleObservationOnInput;
    }

    public boolean isAxiomBasedAbduciblesOnInput() {
        return isAxiomBasedAbduciblesOnInput;
    }

    public void setAxiomBasedAbduciblesOnInput(boolean axiomBasedAbduciblesOnInput) {
        isAxiomBasedAbduciblesOnInput = axiomBasedAbduciblesOnInput;
    }
}
