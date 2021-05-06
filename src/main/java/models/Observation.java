package models;

import common.Printer;
import org.semanticweb.owlapi.model.OWLAxiom;


public class Observation {

    private OWLAxiom axiom;

    public Observation(OWLAxiom axiom) {
        this.axiom = axiom;
    }

    public OWLAxiom getOwlAxiom() {
        return axiom;
    }

    @Override
    public String toString() {
        return Printer.print(axiom);
    }

}
