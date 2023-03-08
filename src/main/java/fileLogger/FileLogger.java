package fileLogger;

import common.Configuration;
import common.DLSyntax;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileLogger {

    public static final String HYBRID_INFO_LOG__PREFIX = "info";
    public static final String HYBRID_ERROR_LOG__PREFIX = "error";
    public static final String HYBRID_LOG_FILE__PREFIX = "hybrid";
    public static final String HYBRID_LEVEL_LOG_FILE__PREFIX = "hybrid_level";
    public static final String HYBRID_EXP_TIMES_LOG_FILE__PREFIX = "hybrid_explanation_times";
    public static final String HYBRID_PARTIAL_EXPLANATIONS_LOG_FILE__PREFIX = "hybrid_partial_explanations";
    public static final String HYBRID_PARTIAL_EXPLANATIONS_ACCORDING_TO_LEVELS_LOG_FILE__PREFIX = "hybrid_partial_level_explanations";
    public static final String LOG_FILE__POSTFIX = ".log";
    private static String FILE_DIRECTORY = "";

    public static void appendToFile(String fileName, long currentTimeMillis, String log) {
        if(Configuration.MHS_MODE){
            FILE_DIRECTORY = "logs_mhs";
        } else {
            FILE_DIRECTORY = "logs";
        }
        createFileIfNotExists(fileName, currentTimeMillis);
        try {
            String file_path = getFilePath(fileName, currentTimeMillis);
            System.out.println(Paths.get(file_path));
            Files.write(Paths.get(file_path), log.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static void createFileIfNotExists(String fileName, long currentTimeMillis) {
        File file = new File(getFilePath(fileName, currentTimeMillis));
        try {
            file.createNewFile();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static String getFilePath(String fileName, long currentTimeMillis) {
        String directoryPath;

        if (Configuration.OUTPUT_PATH.isEmpty()) {
            directoryPath = getDefaultOutputPath();
        }
        else {
            directoryPath = FILE_DIRECTORY
                    .concat(File.separator)
                    .concat(Configuration.OUTPUT_PATH);
        }
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        return directoryPath.concat(File.separator).concat("" + currentTimeMillis + "__").concat(Configuration.INPUT_FILE_NAME + "__").concat(fileName).concat(LOG_FILE__POSTFIX);
    }

    private static String getDefaultOutputPath() {
        String directoryPath;
        String[] inputFile;

        inputFile = Configuration.INPUT_ONT_FILE.split("[/\\\\]");
        if (inputFile.length <= 1) inputFile = Configuration.INPUT_ONT_FILE.split(File.separator);

        String input = inputFile[inputFile.length - 1];
        String ontologyName = input;
        String[] inputFileParts = input.split("\\.");
        if (inputFileParts.length > 0) {
            ontologyName = inputFileParts[0];
        }

        directoryPath = FILE_DIRECTORY
                .concat(File.separator)
                .concat(ontologyName)
                .concat(File.separator)
                .concat(Configuration.INPUT_FILE_NAME);
        return directoryPath;
    }

    private static String observationToFilePath(){
        String[] observation = Configuration.OBSERVATION.substring(0, Configuration.OBSERVATION.length()-1).split("\\"+ DLSyntax.LEFT_PARENTHESES);
        for (int i = 0; i < observation.length; i++){
            if (observation[i].contains(DLSyntax.DELIMITER_ONTOLOGY)){
                observation[i] = observation[i].substring(observation[i].indexOf(DLSyntax.DELIMITER_ONTOLOGY)+1);
            }
            else {
                observation[i] = observation[i].substring(observation[i].indexOf(DLSyntax.DELIMITER_ASSERTION)+1);
            }
        }
        return String.join("_", observation);
    }
}


