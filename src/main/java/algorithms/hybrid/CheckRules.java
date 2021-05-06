package algorithms.hybrid;

import common.Printer;
import models.Explanation;
import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.OWLAxiom;
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
//        reasonerManager.addAxiomToOntology(loader.getNegObservation().getOwlAxiom());
        reasonerManager.addAxiomsToOntology(explanation.getOwlAxioms());
        boolean isConsistent = reasonerManager.isOntologyConsistent();
//        reasonerManager.removeAxiomFromOntology(loader.getNegObservation().getOwlAxiom());
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

    private void printAxioms(List<OWLAxiom> axioms){
        List<String> result = new ArrayList<>();
        for (OWLAxiom owlAxiom : axioms) {
            result.add(Printer.print(owlAxiom));
        }
        System.out.println("{" + StringUtils.join(result, ",") + "}");
    }


}