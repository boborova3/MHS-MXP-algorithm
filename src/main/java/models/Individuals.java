package models;

import common.DLSyntax;
import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.LinkedList;
import java.util.List;

public class Individuals {

    private List<OWLNamedIndividual> namedIndividuals;

    public Individuals() {
        this.namedIndividuals = new LinkedList<>();
    }

    public void addNamedIndividual(OWLNamedIndividual namedIndividual) {
        namedIndividuals.add(namedIndividual);
    }

    public List<OWLNamedIndividual> getNamedIndividuals() {
        return namedIndividuals;
    }

    @Override
    public String toString() {
        List<String> individuals = new LinkedList<>();

        for (OWLNamedIndividual namedIndividual : namedIndividuals) {
            individuals.add(namedIndividual.getIRI().getFragment());
        }

        return StringUtils.join(individuals, DLSyntax.DELIMITER_INDIVIDUAL);
    }
}
