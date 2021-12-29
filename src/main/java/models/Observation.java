package models;

import common.Printer;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.List;

public class Observation {

    private OWLAxiom axiom;
    private List<OWLAxiom> axiomsInMultipleObservations;
    private OWLNamedIndividual reductionIndividual;

    public Observation(OWLAxiom axiom) {
        this.axiom = axiom;
        this.axiomsInMultipleObservations = null;
        this.reductionIndividual = null;
    }

    public Observation(OWLAxiom axiom, List<OWLAxiom> axiomsInMultipleObservations, OWLNamedIndividual reductionIndividual) {
        this.axiom = axiom;
        this.axiomsInMultipleObservations = axiomsInMultipleObservations;
        this.reductionIndividual = reductionIndividual;
    }

    public OWLAxiom getOwlAxiom() {
        return axiom;
    }

    public List<OWLAxiom> getAxiomsInMultipleObservations() {
        return axiomsInMultipleObservations;
    }

    public OWLNamedIndividual getReductionIndividual() {
        return reductionIndividual;
    }

    @Override
    public String toString() {
        if(axiomsInMultipleObservations == null || reductionIndividual == null){
            return Printer.print(axiom);
        }
        StringBuilder result = new StringBuilder();
        result.append("Reduced observation: ");
        result.append(axiom);
        result.append("\n Observation consist of multiple observations: ");
        for (OWLAxiom a : axiomsInMultipleObservations){
            result.append(Printer.print(a));
        }
        result.append("\n");
        return result.toString();
    }

}
