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
        x[0] = "/home/iveta/Plocha/skola/diplomovka/testingFiles/testingFiles25/lubm-25_2_2_noNeg.in";
        x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/mhs_mod/family.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/mhs_mod/pokus9_1.in";
        x[0]= "/home/iveta/Plocha/skola/diplomovka/testingFiles/testingFiles0/lubm-0_3_5_noNeg.in";
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
