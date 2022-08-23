import algorithms.ISolver;
import algorithms.hybrid.HybridSolver;
import parser.ArgumentParser;
import common.Configuration;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import reasoner.ILoader;
import reasoner.IReasonerManager;
import reasoner.Loader;
import reasoner.ReasonerManager;
import timer.ThreadTimes;


public class Main {

    public static void main(String[] args) throws Exception {

        String[] x = new String[1];
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithm/in/testExtractingModels/pokus9_1.in"; //modely problem

//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithm/in/testExtractingModels/input_01.in"; //model existuje oprava

        //konzistentne vysvetlenia zle odfiltrovane
//        x[0] ="C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus6.in";
//        x[0] ="C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus6_2.in";

        //roly + problem indexovy
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_19.in";
        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/input_fam.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/input_fam4.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/multiple_obs/tom.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/multiple_obs/tom1.in";

//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_13.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_14.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_15.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_16.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_17.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_18.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_19.in";

//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_13.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_14.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_15.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_16.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_17.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_18.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_19.in";

        Logger.getRootLogger().setLevel(Level.OFF);
        BasicConfigurator.configure();

        ArgumentParser argumentParser = new ArgumentParser();
//        argumentParser.parse(args);
        argumentParser.parse(x); // for testing

        ILoader loader = new Loader();
        loader.initialize(Configuration.REASONER);

        ThreadTimes threadTimes = new ThreadTimes(100);
        threadTimes.start();

        IReasonerManager reasonerManager = new ReasonerManager(loader);
        ISolver solver = createSolver(threadTimes);

        if (solver != null) {
            solver.solve(loader, reasonerManager);
        }

        threadTimes.interrupt();
    }

    private static ISolver createSolver(ThreadTimes threadTimes) {
        long currentTimeMillis = System.currentTimeMillis();
        return new HybridSolver(threadTimes, currentTimeMillis);
    }
}
