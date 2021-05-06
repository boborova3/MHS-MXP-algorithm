package algorithms.hybrid;

import models.Explanation;

import java.util.List;

public interface ICheckRules {

    boolean isConsistent(Explanation explanation);

    boolean isExplanation(Explanation explanation);

    boolean isMinimal(List<Explanation> explanationList, Explanation explanation);
}
