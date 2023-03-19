package progress;

import common.Configuration;

public abstract class ProgressManager {

    final protected int BAR_LENGTH = 30;
    protected int currentProgress = 0;
    protected String message;

    public void updateProgress(int depth, double time) {
        updateProgressAccordingToCorrectFactor(depth, time);
        processProgress();
    }

    public void updateProgress(double percentage, String message) {
        currentProgress = percentageToBlocks(percentage);
        this.message = message;
        processProgress();
    }

    protected void updateProgressAccordingToCorrectFactor(int depth, double time){

        if (Configuration.DEPTH != null && Configuration.DEPTH > 0 && Configuration.DEPTH < Integer.MAX_VALUE){
            updateProgressAccordingToDepthLimit(depth);
        }
        else if (Configuration.TIMEOUT != null){
            updateProgressAccordingToTimeLimit(time);
            updateMessageAccordingToTimeLimit(time);
            return;
        }
        updateMessageAccordingToDepth(depth);
    }

    abstract protected void processProgress();

    private int percentageToBlocks(double percentage){
        return (int) Math.ceil(BAR_LENGTH * percentage);
    }

    private void updateProgressAccordingToDepthLimit(int depth){
        double remainingPercentage = 1 - currentProgress / (double) BAR_LENGTH;
        int maxDepth = Configuration.DEPTH;
        double percentageToFill = remainingPercentage / Math.pow(maxDepth, maxDepth - depth);
        int newProgress = percentageToBlocks(percentageToFill);
        increaseProgress(newProgress);
    }

    private void updateMessageAccordingToDepth(int depth){
        message = "Finished tree depth: " + depth;
    }

    private void updateProgressAccordingToTimeLimit(double time){
        double percentage = time / (double) Configuration.TIMEOUT;
        currentProgress = percentageToBlocks(percentage);
    }

    private void updateMessageAccordingToTimeLimit(double time){
        message = "Seconds left until time-out: " + (Configuration.TIMEOUT - time);
    }

    private void increaseProgress(int newProgress){
        currentProgress += newProgress;
    }

}
