package common;

import reasoner.ReasonerType;

import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;

public class Configuration {
    public static String OBSERVATION = "";
    public static String INPUT_ONT_FILE = "";
    public static String INPUT_FILE_NAME = "";
    public static String OUTPUT_PATH = "";
    public static String ABDUCIBLES_FILE_NAME = null;
    public static ReasonerType REASONER = ReasonerType.JFACT;     // we work only with JFact for now
    public static Integer DEPTH;
    public static Long TIMEOUT;
    public static ArrayList<String> ABDUCIBLES_CONCEPTS = new ArrayList<>();
    public static ArrayList<String> ABDUCIBLES_INDIVIDUALS = new ArrayList<>();
    public static ArrayList<String> ABDUCIBLES_ROLES = new ArrayList<>();
    public static ArrayList<String> AXIOM_BASED_ABDUCIBLES = new ArrayList<>();
    public static ArrayList<String> PREFIXES = new ArrayList<>();
    public static boolean NEGATION_ALLOWED = true;
    public static boolean LOOPING_ALLOWED = true;
    public static boolean MHS_MODE = false;
    public static boolean ROLES_IN_EXPLANATIONS_ALLOWED = false; // unstable for now
    public static boolean STRICT_RELEVANCE = true;

    //constants set before run program
    public static boolean REUSE_OF_MODELS = true;
    public static boolean CHECKING_MINIMALITY_BY_QXP = false;
    public static boolean CACHED_CONFLICTS_LONGEST_CONFLICT = false;
    public static boolean CACHED_CONFLICTS_MEDIAN = false;
    public static boolean CHECK_RELEVANCE_DURING_BUILDING_TREE_IN_MHS_MXP = false;

    //public static boolean RETURN_CACHED_EXPLANATION_IN_QXP = true;
}
