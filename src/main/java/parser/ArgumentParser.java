package parser;

import application.Application;
import application.ExitCode;
import common.Configuration;
import common.DLSyntax;
import reasoner.ReasonerType;

import java.io.*;
import java.util.ArrayList;


public class ArgumentParser {

    public void parse(String[] args) {

        if (args.length != 1){
            System.err.println("Wrong number of argument for main function: Run program with one configuration input file as argument");
            Application.finish(ExitCode.ERROR);
        }
        Configuration.INPUT_FILE_NAME = new File(args[0]).getName().split("\\.")[0];
        ArrayList<String[]> lines = read_input_file(args[0]);

        boolean read_concepts = false;
        boolean read_individuals = false;
        boolean read_prefixes= false;
        boolean read_roles = false;
        boolean read_abducibles = false;

        for (String[] line: lines){
            String new_line = line[0].trim();
            if (read_concepts || read_individuals || read_prefixes || read_roles || read_abducibles){
                if (new_line.equals("}")){
                    read_prefixes = false;
                    read_concepts = false;
                    read_individuals = false;
                    read_roles = false;
                    read_abducibles = false;
                } else if (read_concepts) {
                    add_abd(new_line, false, true, false);
                } else if (read_individuals) {
                    add_abd(new_line, false,false, false);
                } else if (read_roles) {
                    add_abd(new_line, false,false, true);
                } else if (read_abducibles) {
                    add_axiom_based_abd(line);
                } else{
                    String last = (line.length == 2) ? line[1] : "";
                    add_prefix(new_line + " " + last);
                }
                continue;
            }
            String next = line[1];
            switch(new_line) {
                case "-f:":
                    if (!(new File(next).exists())){
                        System.err.println("Could not open -f file " + next);
                        Application.finish(ExitCode.ERROR);
                    }
                    Configuration.INPUT_ONT_FILE = next;
                    break;
                case "-o:":
                    String observation = String.join(" ", line).replace("-o: ", "");
                    Configuration.OBSERVATION = observation;
                    break;
                case "-output:":
                    String path = String.join(" ", line).replace("-output: ", "");
                    if (path.matches("^[\\w\\\\/]*[\\w]+$")) {
                        Configuration.OUTPUT_PATH = path;
                    } else {
                        System.err.println("Wrong output path -output " + path + "\nOutput path should contain only alphanumeric symbols, _ and separators (\\,/) and cannot end with a separator");
                        Application.finish(ExitCode.ERROR);
                    }
                    break;

                    // Note: we work with only JFact reasoner for now

//                case "-r:":
//                    try {
//                        Configuration.REASONER = ReasonerType.valueOf(next.toUpperCase());
//                    }
//                    catch (IllegalArgumentException e){
//                        System.err.println("Reasoner type -r " + next + " is unknown, the only allowed reasoners are hermit|pellet|jfact");
//                        Application.finish(ExitCode.ERROR);
//                    }
//                    break;

                case "-d:":
                    try {
                        Configuration.DEPTH = Integer.valueOf(next);
                    }
                    catch (NumberFormatException e) {
                        System.err.println("Wrong tree depth -d " + next + ", choose a whole number value");
                        Application.finish(ExitCode.ERROR);
                    }
                    break;
                case "-t:":
                    try {
                        Configuration.TIMEOUT = Long.valueOf(next);
                    }
                    catch (NumberFormatException e) {
                        System.err.println("Wrong timeout value -t " + next + ", choose a whole number value");
                        Application.finish(ExitCode.ERROR);
                    }
                    break;
                case "-aI:":
                    if (next.equals("{")){
                        read_individuals = true;
                    } else {
                        add_abd(next, false, false, false);
                    }
                    break;
                case "-aC:":
                    if (next.equals("{")){
                        read_concepts = true;
                    } else {
                        add_abd(next, false,true, false);
                    }
                    break;
                case "-aR:":
                    if (next.equals("{")){
                        read_roles = true;
                    } else {
                        add_abd(next, false,false, true);
                    }
                    break;
                case "-abd:":
                    if (next.equals("{")){
                        read_abducibles = true;
                    } else {
                        add_abd(next, true,false, false);
                    }
                    break;
                case "-mhs:":
                    if (next.equals("true")) {
                        Configuration.MHS_MODE = true;
                    } else if (!next.equals("false")) {
                        System.err.println("Wrong MHS mode value -mhs" + next + ", allowed values are 'true' and 'false'");
                    }
                    break;
                case "-l:":
                    if (next.equals("false")) {
                        Configuration.LOOPING_ALLOWED = false;
                    } else if (!next.equals("true")) {
                        System.err.println("Wrong looping allowed value -l" + next + ", allowed values are 'true' and 'false'");
                    }
                    break;
                case "-r:":
                    if (next.equals("true")) {
                        Configuration.ROLES_IN_EXPLANATIONS_ALLOWED = true;
                    } else if (!next.equals("false")) {
                        System.err.println("Wrong roles in explanations allowed value -r" + next + ", allowed values are 'true' and 'false'");
                    }
                    break;
                case "-sR:":
                    if (next.equals("false")) {
                        Configuration.STRICT_RELEVANCE = false;
                    } else if (!next.equals("true")) {
                        System.err.println("Wrong strict relevance value -sR" + next + ", allowed values are 'true' and 'false'");
                }
                case "-n:":
                    if (next.equals("false")) {
                        Configuration.NEGATION_ALLOWED = false;
                    } else if (!next.equals("true")) {
                        System.err.println("Wrong negation allowed value -n" + next + ", allowed values are 'true' and 'false'");
                    }
                    break;
                case "-abdF:":
                    if (!(new File(next).exists())){
                        System.err.println("Could not open -abdF file " + next);
                        Application.finish(ExitCode.ERROR);
                    }
                    Configuration.ABDUCIBLES_FILE_NAME = next;
                    break;
                default:
                    System.err.println("Unknown option " + line[0] + " in input file");
                    Application.finish(ExitCode.ERROR);
            }
        }
        if (Configuration.INPUT_ONT_FILE.equals("") || Configuration.OBSERVATION.equals("")){
            System.err.println("Input file -f and observation -o are both required argument");
            Application.finish(ExitCode.ERROR);
        }
        if (Configuration.REASONER == null) {
            Configuration.REASONER = ReasonerType.JFACT;
        }
        if (Configuration.DEPTH == null){
            Configuration.DEPTH = Integer.MAX_VALUE;
        }
    }

    private void add_prefix(String prefix){
        if (!prefix.matches("[a-zA-Z0-9]+: " + DLSyntax.IRI_REGEX)){
            System.err.println("Prefix '" + prefix + "' does not match the form 'prefix_shortcut: prefix'");
            Application.finish(ExitCode.ERROR);
        }
        Configuration.PREFIXES.add(prefix);
    }

    private void add_abd(String abd, boolean axiomBasedAbducibles, boolean isConcept, boolean isRole){
//        System.out.println(abd);
        if (axiomBasedAbducibles)
            Configuration.AXIOM_BASED_ABDUCIBLES.add(abd);
        else if (isConcept)
            Configuration.ABDUCIBLES_CONCEPTS.add(abd);
        else if (isRole)
            Configuration.ABDUCIBLES_ROLES.add(abd);
        else
            Configuration.ABDUCIBLES_INDIVIDUALS.add(abd);
    }

    private void add_axiom_based_abd(String[] abd){
        String assertion = "";
        for(String abd1 : abd){
            assertion += abd1 + " ";
        }
        Configuration.AXIOM_BASED_ABDUCIBLES.add(assertion);
    }

    private ArrayList<String[]> read_input_file(String input_file_path) {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(input_file_path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String strLine;
        ArrayList<String[]> lines = new ArrayList<>();
        try {
            while ((strLine = reader.readLine()) != null) {
                if (strLine.equals("")){
                    continue;
                }
                String[] words = strLine.split("\\s+");
                lines.add(words);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

}
