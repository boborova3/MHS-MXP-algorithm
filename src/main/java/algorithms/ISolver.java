package algorithms;

import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import reasoner.ILoader;
import reasoner.IReasonerManager;


public interface ISolver {

    void solve(ILoader loader, IReasonerManager reasonerManager) throws OWLOntologyStorageException;

}
