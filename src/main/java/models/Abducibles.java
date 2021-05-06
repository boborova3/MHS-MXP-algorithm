package models;

import common.Printer;
import openllet.owlapi.OWL;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import reasoner.ILoader;
import reasoner.Loader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Abducibles {

//    private Set<OWLAxiom> owlAxioms;
    private Set<OWLNamedIndividual> individuals;
    private Set<OWLClass> classes;
    private Set<OWLObjectProperty> roles;
    private Set<OWLAxiom> axioms;
    private ILoader loader;

    public Abducibles(ILoader loader) {
        this.individuals = new HashSet<>(loader.getOntology().getIndividualsInSignature());
        this.classes = new HashSet<>(loader.getOntology().getClassesInSignature());
        this.roles = new HashSet<>(loader.getOntology().getObjectPropertiesInSignature());
        this.individuals.addAll(loader.getObservation().getOwlAxiom().getIndividualsInSignature());
        this.loader = loader;
    }

    public Abducibles(ILoader loader, Set<OWLNamedIndividual> ind, Set<OWLClass> cl, Set<OWLObjectProperty> op) {
        this.individuals = ind;
        this.classes = cl;
        this.roles = op;
        this.loader = loader;
    }

    public Set<OWLNamedIndividual> getIndividuals() {
        return individuals;
    }

    public Set<OWLClass> getClasses() {
        return classes;
    }

    public Set<OWLObjectProperty> getRoles() { return roles; }

    public Set<OWLAxiom> getAxioms() {
        if (axioms == null || axioms.size() < individuals.size() * classes.size() * 2){
            this.axioms = new HashSet<>();
            for (OWLNamedIndividual ind: individuals){
                for (OWLClass cl: classes){
                    axioms.add(loader.getDataFactory().getOWLClassAssertionAxiom(cl, ind));
                    axioms.add(loader.getDataFactory().getOWLClassAssertionAxiom(cl.getComplementNNF(), ind));
                }
                for (OWLNamedIndividual object: individuals){
                    if (ind.equals(object)){
                        continue;
                    }
                    for (OWLObjectProperty op: roles){
                        axioms.add(loader.getDataFactory().getOWLObjectPropertyAssertionAxiom(op, ind, object));
//                        axioms.add(loader.getDataFactory().getOWLObjectPropertyAssertionAxiom(op.get, ind, object));
                    }
                }
            }
        }
        return axioms;
    }

    public void addAbducibles(Abducibles abducibles) {
        this.individuals.addAll(abducibles.getIndividuals());
        this.classes.addAll(abducibles.getClasses());
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (OWLNamedIndividual ind : individuals) {
            result.append(ind).append(";");
        }
        for (OWLClass cl : classes) {
            result.append(cl).append(";");
        }
        for (OWLObjectProperty op: roles){
            result.append(op).append(";");
        }
        return result.toString();
    }

    public void removeIndividuals(List<OWLNamedIndividual> ind){
        individuals.removeAll(ind);
    }

    public void removeClasses(List<OWLClass> cl){
        individuals.removeAll(cl);
    }

    public void removeRoles(List<OWLObjectProperty> op){
        roles.removeAll(op);
    }

    public void addIndividuals(List<OWLNamedIndividual> ind){
        individuals.addAll(ind);
    }

    public void addClasses(List<OWLClass> cl){
        classes.addAll(cl);
    }

    public void addRoles(List<OWLObjectProperty> op){
        roles.addAll(op);
    }

    public boolean noAbduciblesSpecified(){
        return individuals.isEmpty() || (classes.isEmpty()); //&& roles.isEmpty());
    }
}
