package reasoner;

import apiImplementation.HybridAbductionManager;
import models.Abducibles;
import models.Individuals;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import parser.ApiObservationParser;
import parser.IObservationParser;

public class ApiLoader extends Loader {

    private final HybridAbductionManager abductionManager;

    public ApiLoader(HybridAbductionManager abductionManager){
        this.abductionManager = abductionManager;
    }

    @Override
    public void initialize(ReasonerType reasonerType) throws Exception {
        loadReasoner(reasonerType);
        loadObservation();
        loadAbducibles();
    }

    @Override
    protected void setupOntology() throws OWLOntologyCreationException {

        ontology = this.abductionManager.getKnowledgeBase();
        ontologyManager = ontology.getOWLOntologyManager();

        observationOntologyFormat = ontology.getFormat();
        ontologyIRI = ontology.getOntologyID().getOntologyIRI().toString();

        originalOntology = ontologyManager.createOntology();
        copyOntology(ontology, originalOntology);

        initialOntology = ontologyManager.createOntology();
        copyOntology(ontology, initialOntology);
    }

    private void copyOntology(OWLOntology oldOntology, OWLOntology newOntology){
        ontologyManager.addAxioms(newOntology, oldOntology.getAxioms());
    }

    protected void loadObservation() throws Exception {
        namedIndividuals = new Individuals();
        IObservationParser observationParser = new ApiObservationParser(this, abductionManager);
        observationParser.parse();
    }

    @Override
    protected void loadPrefixes() {}

    @Override
    protected void loadAbducibles(){
        abducibles = abductionManager.getAbducibles().exportAbducibles(this);
        if (abducibles.noAbduciblesSpecified())
            abducibles = new Abducibles(this);
    }
}
