package algorithms.hybrid;

import models.Explanation;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.util.List;

public interface ICheckRules {

    boolean isConsistent(Explanation explanation);

    boolean isExplanation(Explanation explanation);

    boolean isMinimal(List<Explanation> explanationList, Explanation explanation);

    boolean isRelevant(Explanation explanation) throws OWLOntologyCreationException;
}
