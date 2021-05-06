package reasoner;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.Collection;
import java.util.stream.Stream;

public class ReasonerManager implements IReasonerManager {

    private ILoader loader;

    public ReasonerManager(ILoader loader) {
        this.loader = loader;
    }

    @Override
    public void addAxiomToOntology(OWLAxiom axiom) {
        loader.getOntologyManager().addAxiom(loader.getOntology(), axiom);
        loader.initializeReasoner();
    }

    @Override
    public void addAxiomsToOntology(Collection<OWLAxiom> axioms) {
        loader.getOntologyManager().addAxioms(loader.getOntology(), axioms);
        loader.initializeReasoner();
    }

    @Override
    public void removeAxiomFromOntology(OWLAxiom axiom) {
        loader.getOntologyManager().removeAxiom(loader.getOntology(), axiom);
        loader.initializeReasoner();
    }

    @Override
    public void resetOntology(Stream<OWLAxiom> axioms) {
        loader.getOntologyManager().removeAxioms(loader.getOntology(), loader.getOntology().axioms());
        loader.initializeReasoner();
        loader.getOntologyManager().addAxioms(loader.getOntology(), axioms);
        loader.initializeReasoner();
    }

    @Override
    public boolean isOntologyConsistent() {
        loader.initializeReasoner();
        return loader.getReasoner().isConsistent();
    }

    @Override
    public boolean isOntologyWithLiteralsConsistent(Collection<OWLAxiom> axioms, OWLOntology ontology) {
        addAxiomsToOntology(axioms);
        boolean isConsistent = isOntologyConsistent();
        resetOntology(ontology.axioms());
        return isConsistent;
    }

}
