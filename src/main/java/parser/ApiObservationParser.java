package parser;

import apiImplementation.HybridAbductionManager;
import common.Configuration;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import reasoner.ApiLoader;

import java.util.logging.Level;

public class ApiObservationParser extends ObservationParser {

    private final HybridAbductionManager manager;

    public ApiObservationParser(ApiLoader loader, HybridAbductionManager manager){
        super(loader);
        this.manager = manager;
    }

    @Override
    public void parse() throws Exception {
        createOntologyFromObservation();
        logger.log(Level.INFO, "Observation: ".concat(Configuration.OBSERVATION));
    }

    @Override
    protected void createOntologyFromObservation() throws OWLOntologyCreationException {

        OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        OWLOntology observationOntology = ontologyManager.createOntology(manager.getMultipleObservations());
        processAxiomsFromObservation(observationOntology);

    }

}
