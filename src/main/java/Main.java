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

import java.util.ArrayList;
import java.util.List;


public class Main {

    public static void main(String[] args) throws Exception {

        String[] x = new String[1];
        List<String> inputs = new ArrayList<>();

        //textExtractingModels
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus0.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus2.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus3.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus4.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus5.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus6.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus7.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus8.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus9.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus9_1.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus9_2.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/posledny.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/mTest.in");
//
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus6_2.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus7_2.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus8_2.in");

        //jack
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/jack1.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/jack2.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/jack4.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/jack7.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/jack8.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/jack10.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/jack11.in");

        //inputs
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_01.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_02.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_02_2.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_02_3.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_03.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_04.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_05.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_06.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_07.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_08.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_09.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_10.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_11.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_12.in");


        //mimo priecinku
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/divideSets.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/input_fam.in"); //PROBLEM !
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/input_fam_2.in");

        //abducibles
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/abducibles/input_fam.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/abducibles/input_fam_abd.in"); //bolo treba spustit este raz samostatne !!
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/abducibles/input_fam_abd2.in"); //bolo treba spustit este raz
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/abducibles/input_fam1.in"); //bolo treba spustit este raz
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/abducibles/input_fam2.in"); //bolo treba spustit este raz
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/abducibles/input_fam3.in"); //bolo treba spustit este raz
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/abducibles/input_fam4.in"); //bolo treba spustit este raz
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/abducibles/input_fam5.in");

        //complex_obs
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/complex_obs/pokus.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/complex_obs/pokus1.in");

        //multiple_obs
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/multiple_obs/family.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/multiple_obs/family2.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/multiple_obs/family3.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/multiple_obs/lubm-0_3_5_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/multiple_obs/lubm-0_5_3_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/multiple_obs/pokus.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/multiple_obs/tom.in"); //PROBLEM !
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/multiple_obs/tom1.in"); //PROBLEM !
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/multiple_obs/tom2.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/multiple_obs/tom3.in");

        //testingFilesFromEvaluation_complexObs
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_1_0_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_1_1_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_1_2_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_1_3_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_1_4_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_1_5_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_1_6_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_1_7_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_1_8_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_1_9_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_2_0_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_2_1_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_2_2_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_2_3_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_2_4_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_2_5_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_2_6_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_2_7_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_2_8_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_2_9_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_3_0_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_3_1_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_3_2_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_3_3_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_3_4_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_3_5_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_3_6_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_3_7_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_3_8_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_3_9_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_4_0_noNeg.in");

//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_4_1_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_4_2_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_4_3_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_4_4_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_4_5_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_4_6_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_4_7_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_4_8_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_4_9_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_5_0_noNeg.in");

//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_5_1_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_5_2_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_5_3_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_5_4_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_5_5_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_5_6_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_5_7_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_5_8_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_5_9_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_5_10_noNeg.in");

//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_5_11_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_5_12_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_5_13_noNeg.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingFilesFromEvaluation_complexObs/lubm-0_5_14_noNeg.in");

        //roly
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/input_19.in"); //PROBLEM !
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/input_fam.in"); //PROBLEM !
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/multiple_obs/tom.in"); //PROBLEM !
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/multiple_obs/tom1.in"); //PROBLEM !
//
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/jack1.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/jack2.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/jack3.in");
//
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_13.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_14.in"); //PADA symetricke
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_15.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_16.in"); //symetricke, tiez nespravne
        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_17.in");
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_18.in"); //symetricke nejde
//        inputs.add("C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_19.in");

        for (String i : inputs) {
            x[0] = i;
            abduction(x);
        }
    }

    private static void abduction(String[] x) throws Exception {
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
