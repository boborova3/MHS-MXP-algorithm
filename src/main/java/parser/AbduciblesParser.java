package parser;

import application.Application;
import application.ExitCode;
import common.Configuration;
import common.Prefixes;
import models.Abducibles;
import org.semanticweb.owlapi.model.*;
import reasoner.Loader;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class AbduciblesParser {

    private Logger logger = Logger.getLogger(ObservationParser.class.getSimpleName());
    private Loader loader;

    public AbduciblesParser(Loader loader) {
        this.loader = loader;
    }

    public Abducibles parse(){
        Set<OWLClass> classes = new HashSet<>();
        Set<OWLNamedIndividual> individuals = new HashSet<>();
        Set<OWLObjectProperty> roles = new HashSet<>();

        for(String concept : Configuration.ABDUCIBLES_CONCEPTS){
            classes.add(create_class(replacePrefixInAbducible(concept)));
        }
        for(String individual : Configuration.ABDUCIBLES_INDIVIDUALS){
            individuals.add(create_individual(replacePrefixInAbducible(individual)));
        }
        for(String role : Configuration.ABDUCIBLES_ROLES){
            roles.add(create_role(replacePrefixInAbducible(role)));
        }

        if (classes.isEmpty() && roles.isEmpty()){
            return new Abducibles(loader);
        }

        if(individuals.isEmpty()){
            return new Abducibles(loader, loader.getOntology().getIndividualsInSignature(), classes, roles);
        }

        return new Abducibles(loader, individuals, classes, roles);
    }

    private OWLClass create_class(String abd){
        return loader.getDataFactory().getOWLClass(IRI.create(abd));
    }

    private OWLNamedIndividual create_individual(String abd){
        return loader.getDataFactory().getOWLNamedIndividual(IRI.create(abd));
    }

    private OWLObjectProperty create_role(String abd){
        return loader.getDataFactory().getOWLObjectProperty(IRI.create(abd));
    }

    private String replacePrefixInAbducible(String abducible){
        String[] abducibleTemp = abducible.split(":");
        if(abducibleTemp.length == 1){
            return abducibleTemp[0];
        } else if (abducibleTemp.length == 2){
            if(!Prefixes.prefixes.containsKey(abducibleTemp[0])){
                System.err.println("Prefix " + abducibleTemp[0] + " in abducible '" + abducible + "' is unknown.");
                Application.finish(ExitCode.ERROR);
            }
            return abducible.replace(abducibleTemp[0] + ":", Prefixes.prefixes.get(abducibleTemp[0]));
        } else {
            System.err.println("Incorrect IRI in abducible '" + abducible + "', only one delimeter ':' may be used - between prefix and name.");
            Application.finish(ExitCode.ERROR);
        }
        return "";
    }
}
