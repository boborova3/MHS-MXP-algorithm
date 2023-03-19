package progress;

public class ConsoleProgressManager extends ProgressManager {

    protected void processProgress(){
        StringBuilder builder = buildProgressBar(currentProgress, message);
        System.out.println(builder);
    }

    private StringBuilder buildProgressBar(int progress, String message){
        StringBuilder builder = new StringBuilder("|");
        for (int i = 0; i < BAR_LENGTH; i++){
            if (i < progress)
                builder.append('-');
            else
                builder.append(' ');
        }
        builder.append("|   ").append(message);
        return builder;
    }
}
