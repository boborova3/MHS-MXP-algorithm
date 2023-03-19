package apiImplementation;

import abductionapi.abducibles.*;
import abductionapi.exception.AxiomAbducibleException;
import models.Abducibles;
import org.semanticweb.owlapi.model.OWLAxiom;
import reasoner.ILoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HybridAxiomAbducibleContainer
        extends HybridAbducibleContainer
        implements AxiomAbducibleContainer {

    Set<OWLAxiom> axioms = new HashSet<>();

    HybridAxiomAbducibleContainer(){}

    @Override
    public void addAxiom(OWLAxiom assertion) throws AxiomAbducibleException {
            axioms.add(assertion);
    }

    @Override
    public void addAxioms(Set<OWLAxiom> axioms) throws AxiomAbducibleException {
        axioms.forEach(this::addAxiom);
    }

    @Override
    public void addAxioms(List<OWLAxiom> axioms) throws AxiomAbducibleException {
        new HashSet<>(axioms).forEach(this::addAxiom);
    }

    @Override
    public Abducibles exportAbducibles(ILoader loader) {
        return new Abducibles(loader, axioms);
    }
}
