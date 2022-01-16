package algorithms.hybrid;

import models.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;
import uk.ac.manchester.cs.jfact.kernel.Axiom;

public class AxiomPair {
    public OWLAxiom first;
    public OWLAxiom second;

    public AxiomPair(OWLAxiom first, OWLAxiom second){
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AxiomPair) {
            AxiomPair axiomPair = (AxiomPair) obj;
            return (first.equals(axiomPair.first) && second.equals(axiomPair.second)) || (first.equals(axiomPair.second) && second.equals(axiomPair.first));
        }
        return false;
    }
}
