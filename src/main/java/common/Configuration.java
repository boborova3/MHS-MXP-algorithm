package common;

import reasoner.ReasonerType;

import java.util.ArrayList;

public class Configuration {
    public static String OBSERVATION = "";
    public static String INPUT_ONT_FILE = "";
    public static String INPUT_FILE_NAME = "";
    public static ReasonerType REASONER;
    public static Integer DEPTH;
    public static Long TIMEOUT;
    public static ArrayList<String> ABDUCIBLES_CONCEPTS = new ArrayList<>();
    public static ArrayList<String> ABDUCIBLES_INDIVIDUALS = new ArrayList<>();
    public static ArrayList<String> ABDUCIBLES_ROLES = new ArrayList<>();
    public static ArrayList<String> PREFIXES = new ArrayList<>();
    public static boolean NEGATION_ALLOWED = true;
    public static boolean LOOPING_ALLOWED = true;
    public static boolean MHS_MODE = false;
    public static boolean ROLES_IN_EXPLANATIONS_ALLOWED = true;
}
