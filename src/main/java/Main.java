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
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus9_1.in"; //modely problem
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus9_2.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus9.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus8.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus7.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus6_inconsistent_obs.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus6_inconsistent_ont.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus6.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus6noNeg.in";
//        x[0] ="C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus6_2.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus5.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus4.in";
//
        //SKUSANIE OUTPUT PATH
//        x[0] ="C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/logs/pokus1.in";
//        x[0] ="C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/logs/pokus2.in";
//        x[0] ="C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/logs/pokus3.in";
//        x[0] ="C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/logs/pokus4.in";
//        x[0] ="C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/logs/pokus5.in";
//        x[0] ="C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/logs/pokus6.in";

//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/input_fam.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/input_fam_2.in";

//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/multiple_obs/tom.in";


        // relevancia
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/examples/input_partially.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/examples/input_strict.in";

        // roly
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/jack2.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/input_fam.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/input_fam_noloop.in";



        // spravy
        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus6_inconsistent_obs.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus6.in";
//        x[0] ="C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus6_2.in";

        // priklad, co bol zle s tymi modelmi
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus9_1.in"; //modely problem




//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/input_01.in"; //model existuje oprava

        //konzistentne vysvetlenia zle odfiltrovane
//        x[0] ="C:/Users/2018/Desktop/new/MHS-MXP-algorithmNEW/in/testExtractingModels/pokus6.in";
//        x[0] ="C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testExtractingModels/pokus6_2.in";

        //roly + problem indexovy
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/testExtractingModels/input_19.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/input_fam.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/multiple_obs/familyr2.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/multiple_obs/family.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/multiple_obs/tom1.in";

//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/ont_input11949.in";

//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_13.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_14.in"; //pada??
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_14_1.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_14_2.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_15.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_15_1.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_15_2.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_15_3.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_16.in"; //nothing to explain
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_17.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_18.in"; //nothing to explain
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_18_1.in"; //pada
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_19.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/input_19_1.in";

//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/jack1.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/jack2.in";
//        x[0] = "C:/Users/2018/Desktop/new/MHS-MXP-algorithm/in/testingRoles/jack3.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithm/in/complex_obs/familyXcomplex01.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithm/in/complex_obs/familyXcomplex02.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithm/in/complex_obs/jackcomplex03.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithm/in/complex_obs/jackcomplex03b.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithm/in/complex_obs/familyXcomplex04.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithm/in/complex_obs/familyXcomplex05.in";

//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/roles_obs/input_fam2.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/roles_obs/jackroleobs.in";

//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/testing0/mhs-mxp/lubm-0_4_3_noNeg.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/testingFiles/testingFiles0/mhs-mxp/lubm-0_4_0_noNeg.in";

//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/multiple_obs/tom.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/multiple_obs/family.in";

        //eval
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_1/mhs/lubm-0_2_2_noNeg.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_1/mhs-mxp/lubm-0_2_2_noNeg.in";

//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_1/mhs/lubm-0_3_2_noNeg.in";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_1/mhs-mxp/lubm-0_3_2_noNeg.in";

        //eval 2
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs-mxp/in_ore_ont_155_1.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_1430_1_mhs.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs-mxp/in_ore_ont_1784_10.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_1931_1_mhs.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_2860_1_mhs.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_3884_1_mhs.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_4469_1_mhs.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_4796_1_mhs.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_5204_1_mhs.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_6859_1_mhs.txt";

//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_8460_1_mhs.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_8362_1_mhs.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_8578_1_mhs.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_8975_1_mhs.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_10807_1_mhs.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_11296_1_mhs.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_12566_1_mhs.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_14883_1_mhs.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_15291_1_mhs.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2/mhs/in_ore_ont_16814_1_mhs.txt";

//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/eval_2_skuska/in_ore_ont_155_1.txt";
//        x[0] = "C:/Users/2018/Desktop/MHS-MXP-algorithmNEW/in/inputRelationship.in";





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
