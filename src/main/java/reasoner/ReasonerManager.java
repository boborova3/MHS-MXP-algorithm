package reasoner;

import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;

import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Set;
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
        System.out.println("TAKATO JE ONTOLOGIA TERAZ");
        loader.initializeReasoner();
        System.out.println(loader.getReasoner().getRootOntology().getAxioms());
        //System.out.println("JEJ KONZISTENTNOST " + loader.getReasoner().isConsistent());
        boolean temp = loader.getReasoner().isConsistent();
        return temp;
        //return loader.getReasoner().isConsistent();
    }

    @Override
    public boolean isOntologyWithLiteralsConsistent(Collection<OWLAxiom> axioms, OWLOntology ontology) {
        addAxiomsToOntology(axioms);
        boolean isConsistent = isOntologyConsistent();
        resetOntology(ontology.axioms());
        return isConsistent;
    }

}
