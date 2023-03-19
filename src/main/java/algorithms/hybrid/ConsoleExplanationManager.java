package algorithms.hybrid;

import models.Explanation;
import org.semanticweb.owlapi.model.*;
import reasoner.ILoader;
import reasoner.IReasonerManager;

public class ConsoleExplanationManager extends ExplanationManager {

    public ConsoleExplanationManager(ILoader loader, IReasonerManager reasonerManager){
        super(loader, reasonerManager);
    }

    public void addPossibleExplanation(Explanation explanation) {
        possibleExplanations.add(explanation);
    }

    public void processExplanations(String message) throws OWLOntologyCreationException, OWLOntologyStorageException {
        try{
            showExplanations(true);
        } catch (OWLOntologyCreationException | OWLOntologyStorageException e) {
            e.printStackTrace();
        }
        showMessages(solver.getInfo(), message);

        if (message != null){
            System.out.println();
            System.out.println(message);
        }
    }

}
