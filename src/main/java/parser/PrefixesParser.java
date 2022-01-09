package parser;

import common.Prefixes;
import org.semanticweb.owlapi.model.*;
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
