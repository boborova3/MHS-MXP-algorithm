package progress;

import apiImplementation.HybridAbductionManager;

public class ApiProgressManager extends ProgressManager {

    HybridAbductionManager abductionManager;

    public ApiProgressManager(HybridAbductionManager abductionManager){
        this.abductionManager = abductionManager;
    }

    protected void processProgress(){
        if (abductionManager.getAbductionMonitor() != null)
            abductionManager.updateProgress(abductionManager.getAbductionMonitor(), currentProgress, message);
    }



}
