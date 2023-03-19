package algorithms.hybrid;

import apiImplementation.HybridAbductionManager;
import models.Explanation;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import reasoner.ILoader;
import reasoner.IReasonerManager;

public class ApiExplanationManager extends ConsoleExplanationManager {

    private final HybridAbductionManager abductionManager;

    public ApiExplanationManager(ILoader loader, IReasonerManager reasonerManager, HybridAbductionManager abductionManager) {
        super(loader, reasonerManager);
        this.abductionManager = abductionManager;
    }

    public void addPossibleExplanation(Explanation explanation) {
        possibleExplanations.add(explanation);
        if (abductionManager.getAbductionMonitor() != null)
            abductionManager.sendExplanation(abductionManager.getAbductionMonitor(),
                                             explanation.createExplanationWrapper());
    }

    public void processExplanations(String message) throws OWLOntologyCreationException, OWLOntologyStorageException {
        showExplanations(false);
        abductionManager.setExplanations(finalExplanations);
    }

}
