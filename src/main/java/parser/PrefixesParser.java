package parser;

import common.Configuration;
import common.DLSyntax;
import common.Prefixes;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import reasoner.Loader;

import java.util.Map;
import java.util.logging.Logger;

public class

PrefixesParser {

    private Logger logger = Logger.getLogger(ObservationParser.class.getSimpleName());
    private OWLDocumentFormat observationOntologyFormat;

    public PrefixesParser(OWLDocumentFormat observationOntologyFormat) {
        this.observationOntologyFormat = observationOntologyFormat;
    }

    public void parse() {
        if (observationOntologyFormat.isPrefixOWLDocumentFormat()) {
            Prefixes.prefixes = observationOntologyFormat.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap();
        }
}
}
