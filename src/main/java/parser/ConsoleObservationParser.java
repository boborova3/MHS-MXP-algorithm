package parser;

import common.Configuration;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import reasoner.Loader;

import java.util.logging.Level;

public class ConsoleObservationParser extends ObservationParser {

    public ConsoleObservationParser(Loader loader) {
        super(loader);
    }

    @Override
    public void parse() throws Exception {
        try{
            createOntologyFromObservation();
        } catch (OWLOntologyCreationException e){
            throw new OWLOntologyCreationException("Invalid format of observation");
        }
        logger.log(Level.INFO, "Observation: ".concat(Configuration.OBSERVATION));
    }

    @Override
    protected void createOntologyFromObservation() throws OWLOntologyCreationException, OWLOntologyStorageException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology observationOntology = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(Configuration.OBSERVATION));

        StringDocumentTarget documentTarget = new StringDocumentTarget();
        observationOntology.saveOntology(documentTarget);

        //variable "format" - used in PrefixesParser
        OWLDocumentFormat format = manager.getOntologyFormat(observationOntology);
        loader.setObservationOntologyFormat(format);

        processAxiomsFromObservation(observationOntology);
    }
}

