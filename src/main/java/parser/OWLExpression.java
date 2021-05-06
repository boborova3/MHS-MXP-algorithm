package parser;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;

class OWLExpression {

    String token;

    OWLClassExpression classExpression;
    OWLObjectPropertyAssertionAxiom objectPropertyAssertionAxiom;
    OWLNegativeObjectPropertyAssertionAxiom negativeObjectPropertyAssertionAxiom;
    OWLObjectProperty objectProperty;

    OWLTyp typ;
}