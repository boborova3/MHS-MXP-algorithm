import algorithms.ISolver;
import algorithms.hybrid.HybridSolver;
import parser.ArgumentParser;
import common.Configuration;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import reasoner.*;
import timer.ThreadTimes;


public class Main {

    public static void main(String[] args) throws Exception {

        String[] x = new String[1];
// UKAZKOVE VSTUPY - spusti sa odkomentovany riadok

        // testing extracting models + bugs
//        x[0] = "./in/testExtractingModels/pokus9_2.in";
        x[0] = "./in/testExtractingModels/pokus9_1.in"; //problemovy priklad s modelmi
//        x[0] = "./in/testExtractingModels/pokus9.in";
//        x[0] = "./in/testExtractingModels/pokus6.in";
//        x[0] = "./in/testExtractingModels/pokus6_2.in";
//        x[0] = "./in/testExtractingModels/pokus4.in";

        // role explanations
//        x[0] = "./in/roleExplanation/input_fam.in";
//        x[0] = "./in/roleExplanation/input_fam_noloop.in";
//        x[0] = "./in/roleExplanation/jack2.in";
//        x[0] = "./in/roleExplanation/jack3.in";

        // relevance
//        x[0] = "./in/relevance/input_partially.in";
//        x[0] = "./in/relevance/input_strict.in";

        // custom output path
//        x[0] ="./in/logPath/pokus1.in";
//        x[0] ="./in/logPath/pokus2.in";
//        x[0] ="./in/logPath/pokus3.in";
//        x[0] ="./in/logPath/pokus4.in";
//        x[0] ="./in/logPath/pokus5_wrong.in";
//        x[0] ="./in/logPath/pokus6_wrong.in";

        // messages
//        x[0] = "./in/messages/pokus6_inconsistent_obs.in";
//        x[0] = "./in/messages/pokus6_inconsistent_ont.in";

        // abducibles
//        x[0] = "./in/abd/input_fam.in";
//        x[0] = "./in/abd/input_fam_abd.in";

        // complex observation
//        x[0] = "./in/complexObs/familyX_not.in";
//        x[0] = "./in/complexObs/familyX_or.in";

        // multiple observation
//        x[0] = "./in/multipleObs/tom.in";
//        x[0] = "./in/multipleObs/tom1.in";
//        x[0] = "./in/multipleObs/tom2.in";
//        x[0] = "./in/multipleObs/tom3.in";
//        x[0] = "./in/multipleObs/family.in";
//        x[0] = "./in/multipleObs/family2.in";


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
