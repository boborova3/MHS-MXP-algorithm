package algorithms.hybrid;

import common.Printer;
import models.Explanation;
import openllet.owlapi.OpenlletReasonerFactory;
import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import reasoner.AxiomManager;
import reasoner.ILoader;
import reasoner.IReasonerManager;

import java.util.ArrayList;
import java.util.List;

public class CheckRules implements ICheckRules {

    private ILoader loader;
    private IReasonerManager reasonerManager;

    CheckRules(ILoader loader, IReasonerManager reasonerManager) {
        this.loader = loader;
        this.reasonerManager = reasonerManager;
    }

    @Override
    public boolean isConsistent(Explanation explanation) {
        reasonerManager.removeAxiomFromOntology(loader.getNegObservation().getOwlAxiom());
        reasonerManager.addAxiomsToOntology(explanation.getOwlAxioms());
        boolean isConsistent = reasonerManager.isOntologyConsistent();
        reasonerManager.resetOntology(loader.getOriginalOntology().axioms());
        reasonerManager.addAxiomToOntology(loader.getNegObservation().getOwlAxiom());
        return isConsistent;
    }

    @Override
    public boolean isExplanation(Explanation explanation) {
        reasonerManager.addAxiomsToOntology(explanation.getOwlAxioms());
        boolean isConsistent = reasonerManager.isOntologyConsistent();
        reasonerManager.resetOntology(loader.getOriginalOntology().axioms());
        return !isConsistent;
    }

    @Override
    public boolean isMinimal(List<Explanation> explanationList, Explanation explanation) {
        if (explanation == null || !(explanation.getOwlAxioms() instanceof List)) {
            return false;
        }

        for (Explanation minimalExplanation : explanationList) {
            if (explanation.getOwlAxioms().containsAll(minimalExplanation.getOwlAxioms())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isRelevant(Explanation explanation) throws OWLOntologyCreationException {
        OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = ontologyManager.createOntology(explanation.getOwlAxioms());

        OWLReasoner reasoner = new OpenlletReasonerFactory().createNonBufferingReasoner(ontology);

        if(loader.isMultipleObservationOnInput()){
            for(OWLAxiom obs : loader.getObservation().getAxiomsInMultipleObservations()){
                OWLAxiom negObs = AxiomManager.getComplementOfOWLAxiom(loader, obs);
                ontologyManager.addAxiom(ontology, negObs);
                if(!reasoner.isConsistent()){
                    return false;
                }
                ontologyManager.removeAxiom(ontology, negObs);
            }
            return true;
        } else {
            ontologyManager.addAxiom(ontology, loader.getNegObservation().getOwlAxiom());
            return reasoner.isConsistent();
        }
    }
}