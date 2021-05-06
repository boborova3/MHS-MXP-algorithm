package parser;

import common.Configuration;
import common.DLSyntax;
import common.Prefixes;
import java.util.logging.Logger;

public class PrefixesParser {

    private Logger logger = Logger.getLogger(ObservationParser.class.getSimpleName());

    public PrefixesParser() {}

    public void parse(){
        for (String prefix: Configuration.PREFIXES){
            String[] prefix_split = prefix.split(DLSyntax.DELIMITER_ASSERTION, 2);
                Prefixes.prefixes.put(prefix_split[0].trim(), prefix_split[1].trim());
        }
    }
}
