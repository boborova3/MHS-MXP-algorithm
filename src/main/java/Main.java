import algorithms.ISolver;
import algorithms.hybrid.HybridSolver;
import common.ArgumentParser;
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
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/lubm-0_4_3_MXP_notNegation.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/lubm-0_4_3_MXP_notNegation_1.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/lubm-0_4_3_MXP_notNegation_2.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/input_fam_5_turtle.txt";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/nove_in/lubm-0_1_0_MXP_notNegation_without.in";

        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/nove_in/lubm-0_5_0_MXP_notNegation_J.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/pokus.in";
        /*skusam*/
        x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/input_fam_2.txt";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/divideSets.in";

        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/multiple_obs/tom.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/multiple_obs/lubm3.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/multiple_obs/lubm.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/complex_obs/pokus.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/multiple_obs/pokus_rovnakyIndividual.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/testExtractingModels/pokus9.in";

        x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/nove_in/lubm-0_2_0+_MXP_notNegation.in";


        Logger.getRootLogger().setLevel(Level.OFF);
        BasicConfigurator.configure();

        ArgumentParser argumentParser = new ArgumentParser();
        //argumentParser.parse(args);
        argumentParser.parse(x);

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
