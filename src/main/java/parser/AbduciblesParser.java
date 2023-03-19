package parser;

import application.Application;
import application.ExitCode;
import common.Configuration;
import common.Prefixes;
import models.Abducibles;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import reasoner.ConsoleLoader;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class AbduciblesParser {

    private Logger logger = Logger.getLogger(ObservationParser.class.getSimpleName());
    private ConsoleLoader loader;

    public AbduciblesParser(ConsoleLoader loader) {
        this.loader = loader;
    }

    public Abducibles parse(){

        checkFormatOfAbducibles();

        if(Configuration.AXIOM_BASED_ABDUCIBLES.size() > 0){
            return createAxiomBasedAbducibles();
        }

        if(Configuration.ABDUCIBLES_FILE_NAME != null){
            return createAbduciblesFromOntologyFile();
        }

        Set<OWLClass> classes = new HashSet<>();
        Set<OWLNamedIndividual> individuals = new HashSet<>();
        Set<OWLObjectProperty> roles = new HashSet<>();

        for(String concept : Configuration.ABDUCIBLES_CONCEPTS){
            classes.add(createClass(replacePrefixInAbducible(concept)));
        }
        for(String individual : Configuration.ABDUCIBLES_INDIVIDUALS){
            individuals.add(createIndividual(replacePrefixInAbducible(individual)));
        }
        for(String role : Configuration.ABDUCIBLES_ROLES){
            roles.add(createRole(replacePrefixInAbducible(role)));
        }

        if (classes.isEmpty() && roles.isEmpty()){
            return new Abducibles(loader);
        }

        if(individuals.isEmpty()){
            return new Abducibles(loader, loader.getOntology().getIndividualsInSignature(), classes, roles);
        }

        return new Abducibles(loader, individuals, classes, roles);
    }

    private void checkFormatOfAbducibles(){
        if(Configuration.AXIOM_BASED_ABDUCIBLES.size() > 0 && (Configuration.ABDUCIBLES_ROLES.size() > 0 || Configuration.ABDUCIBLES_INDIVIDUALS.size() > 0 || Configuration.ABDUCIBLES_CONCEPTS.size() > 0)){
            System.err.println("Incorrect format of abducibles. You have to choose axiom based abducibles (switch -abd) or list of individuals, concepts, roles (switches -aI, -aC, -aR)");
            Application.finish(ExitCode.ERROR);
        }

        if(Configuration.AXIOM_BASED_ABDUCIBLES.size() > 0 && Configuration.ABDUCIBLES_FILE_NAME != null){
            System.err.println("Incorrect format of abducibles. You have to choose listed axiom based abducibles (switch -abd) or axiom based abducibles from ontology file (switch -abdF)");
            Application.finish(ExitCode.ERROR);
        }

        if(Configuration.ABDUCIBLES_FILE_NAME != null && (Configuration.ABDUCIBLES_ROLES.size() > 0 || Configuration.ABDUCIBLES_INDIVIDUALS.size() > 0 || Configuration.ABDUCIBLES_CONCEPTS.size() > 0)){
            System.err.println("Incorrect format of abducibles. You have to choose axiom based abducibles from ontology file (switch -abdF) or list of individuals, concepts, roles (switches -aI, -aC, -aR)");
            Application.finish(ExitCode.ERROR);
        }
    }

    private Abducibles createAbduciblesOnlyFromInputFile() throws OWLOntologyCreationException, OWLOntologyStorageException {
        StringBuilder abduciblesInString = new StringBuilder();
        for(String axiom : Configuration.AXIOM_BASED_ABDUCIBLES){
            abduciblesInString.append(axiom + " ");
        }

        OWLOntology abduciblesOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new StringDocumentSource(abduciblesInString.toString()));
        StringDocumentTarget documentTarget = new StringDocumentTarget();
        abduciblesOntology.saveOntology(documentTarget);

        if(abduciblesOntology.getAxioms().size() <= 1){
            return null;
        }

        Set<OWLAxiom> abducibles = new HashSet<>();
        for(OWLAxiom axiom : abduciblesOntology.getAxioms()){
            if(axiom.getAxiomType() == AxiomType.CLASS_ASSERTION || axiom.getAxiomType() == AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION || axiom.getAxiomType() == AxiomType.OBJECT_PROPERTY_ASSERTION){
                abducibles.add(axiom);
            }
        }
        return new Abducibles(loader, abducibles);
    }

    private Abducibles createAbduciblesFromInputFileAndOntologyFile() throws OWLOntologyCreationException, OWLOntologyStorageException {
        StringBuilder abduciblesInString = new StringBuilder();

        for(String prefix : Prefixes.prefixes.keySet()){
            abduciblesInString.append("Prefix: " + prefix  + " <" + Prefixes.prefixes.get(prefix) + "> ");
        }

        for(OWLAxiom axiom : loader.getOntology().getAxioms(AxiomType.DECLARATION)){
            OWLDeclarationAxiom axiom1 = (OWLDeclarationAxiom) axiom;
            if(axiom1.getClassesInSignature().size() != 0){
                abduciblesInString.append(" Class: " + axiom1.getEntity());
            }
        }

        for(String axiom : Configuration.AXIOM_BASED_ABDUCIBLES){
            String[] temp = axiom.split(" ");
            String axiom1 = "";
            for(String s : temp){
                boolean contains = false;
                for(String prefix : Prefixes.prefixes.keySet()){
                    if(s.contains(prefix)){
                        contains = true;
                        s = s.replace(prefix, Prefixes.prefixes.get(prefix));
                    }
                }
                if(contains){
                    if(s.endsWith(",")){
                        s = s.substring(0, s.length() - 1) + ">,";
                    } else {
                        s = s + ">";
                    }
                    if(s.contains(" not ")){
                        s = " not <" + s;
                    } else {
                        s = "<" + s;
                    }
                    System.out.println(s);
                }
                axiom1 += s + " ";
            }
            abduciblesInString.append(" " + axiom1);
        }

        System.out.println(abduciblesInString);

        OWLOntology abduciblesOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new StringDocumentSource(abduciblesInString.toString().trim()));
        StringDocumentTarget documentTarget = new StringDocumentTarget();
        abduciblesOntology.saveOntology(documentTarget);

        System.out.println(abduciblesOntology);

        Set<OWLAxiom> abducibles = new HashSet<>();
        for(OWLAxiom axiom : abduciblesOntology.getAxioms()){
            if(axiom.getAxiomType() == AxiomType.CLASS_ASSERTION || axiom.getAxiomType() == AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION || axiom.getAxiomType() == AxiomType.OBJECT_PROPERTY_ASSERTION){
                abducibles.add(axiom);
            }
        }
        System.out.println("ABDUCIBLES");
        System.out.println(abducibles);
        return new Abducibles(loader, abducibles);
    }

    private Abducibles createAxiomBasedAbducibles(){
        /*Set<OWLAxiom> abducibles = new HashSet<>();
        for(String axiom : Configuration.AXIOM_BASED_ABDUCIBLES){
            abducibles.add(createAxiom(axiom));
        }

        loader.setAxiomBasedAbduciblesOnInput(true);
        return new Abducibles(loader, abducibles);*/
        loader.setAxiomBasedAbduciblesOnInput(true);
        Abducibles resultAbducibles = null;
        try {
            resultAbducibles = createAbduciblesOnlyFromInputFile();
            if(resultAbducibles != null){
                return resultAbducibles;
            }
            resultAbducibles = createAbduciblesFromInputFileAndOntologyFile();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }

        return resultAbducibles;
    }

    /*private BidirectionalShortFormProvider getShortFormProvider() {
        Set<OWLOntology> ontologies = OWLManager.createOWLOntologyManager().getOntologies(); // my OWLOntologyManager
        ShortFormProvider sfp = new ManchesterOWLSyntaxPrefixNameShortFormProvider(
                OWLManager.createOWLOntologyManager().getOntologyFormat(loader.getOriginalOntology()));
        BidirectionalShortFormProvider shortFormProvider = new BidirectionalShortFormProviderAdapter(
                ontologies, sfp);
        return shortFormProvider;
    }*/

    private Abducibles createAbduciblesFromOntologyFile() {
        Set<OWLAxiom> abducibles = new HashSet<>();
        OWLOntology abduciblesOntology = null;
        try {
            abduciblesOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File(Configuration.ABDUCIBLES_FILE_NAME));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        for(OWLAxiom axiom : abduciblesOntology.getAxioms()){
            if(axiom.getAxiomType() == AxiomType.CLASS_ASSERTION || axiom.getAxiomType() == AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION || axiom.getAxiomType() == AxiomType.OBJECT_PROPERTY_ASSERTION){
                abducibles.add(axiom);
            }
        }
        loader.setAxiomBasedAbduciblesOnInput(true);
        return new Abducibles(loader, abducibles);
    }

    private OWLClass createClass(String abd){
        return loader.getDataFactory().getOWLClass(IRI.create(abd));
    }

    /*private OWLClassExpression createNegClass(String abd){
        return loader.getDataFactory().getOWLClass(IRI.create(abd)).getComplementNNF();
    }*/

    private OWLNamedIndividual createIndividual(String abd){
        return loader.getDataFactory().getOWLNamedIndividual(IRI.create(abd));
    }

    private OWLObjectProperty createRole(String abd){
        return loader.getDataFactory().getOWLObjectProperty(IRI.create(abd));
    }

    /*private OWLAxiom createAxiom(String abd){
        String abdWithoutRightParentheses = abd.substring(0, abd.length() - 1);
        if(isConceptAssertion(abd)){
            return createConceptAssertionAxiom(abdWithoutRightParentheses);
        }
        return createRoleAssertionAxiom(abdWithoutRightParentheses);
    }*/

    /*private boolean isConceptAssertion(String abd){
        int comma = abd.indexOf(',');
        int leftParentheses = abd.indexOf('(');
        int rightParentheses = abd.indexOf(')');
        if(leftParentheses == -1 || rightParentheses == -1 || leftParentheses + 1 == rightParentheses){
            System.err.println("Incorrect format in abducibles (correct: concept(individual) or role(individual1, individual2))");
            Application.finish(ExitCode.ERROR);
        }
        if(comma == -1){
            return true;
        }
        if(leftParentheses < comma && comma < rightParentheses && leftParentheses + 1 != comma && rightParentheses - 1 != comma){
            return false;
        }
        System.err.println("Incorrect format in abducibles (correct: concept(individual) or role(individual1, individual2))");
        Application.finish(ExitCode.ERROR);
        return false;
    }

    private OWLAxiom createConceptAssertionAxiom(String abdWithoutRightParentheses){
        String[] temp = abdWithoutRightParentheses.split("\\(");
        OWLClassExpression owlClass;
        if('-' == temp[0].charAt(0)){
            owlClass = createNegClass(replacePrefixInAbducible(temp[0].substring(1)));
        } else {
            owlClass = createClass(replacePrefixInAbducible(temp[0]));
        }
        OWLIndividual owlIndividual = createIndividual(replacePrefixInAbducible(temp[1]));
        return loader.getDataFactory().getOWLClassAssertionAxiom(owlClass, owlIndividual);
    }

    private OWLAxiom createRoleAssertionAxiom(String abdWithoutRightParentheses){
        String[] temp = abdWithoutRightParentheses.split("\\(");
        boolean negRole = false;
        OWLObjectProperty owlObjectProperty;

        if('-' == temp[0].charAt(0)){
            negRole = true;
            owlObjectProperty = createRole(replacePrefixInAbducible(temp[0].substring(1)));
        } else {
            owlObjectProperty = createRole(replacePrefixInAbducible(temp[0]));
        }

        String[] temp2 = temp[1].split(",");
        OWLIndividual owlIndividualSubject = createIndividual(replacePrefixInAbducible(temp2[0]));
        OWLIndividual owlIndividualObject = createIndividual(replacePrefixInAbducible(temp2[1]));

        if(negRole){
            return loader.getDataFactory().getOWLNegativeObjectPropertyAssertionAxiom(owlObjectProperty, owlIndividualSubject, owlIndividualObject);
        }
        return loader.getDataFactory().getOWLObjectPropertyAssertionAxiom(owlObjectProperty, owlIndividualSubject, owlIndividualObject);
    }*/

    private String replacePrefixInAbducible(String abducible){
        String[] abducibleTemp = abducible.split(":");
        if(abducibleTemp.length == 1){
            return abducibleTemp[0];
        } else if("http".equals(abducibleTemp[0])){
            return abducible;
        } else if (abducibleTemp.length == 2){
            String pref = abducibleTemp[0] + ":";
            if(!Prefixes.prefixes.containsKey(pref)){
                System.err.println("Prefix " + abducibleTemp[0] + " in abducible '" + abducible + "' is unknown.");
                Application.finish(ExitCode.ERROR);
            }
            return abducible.replace(pref, Prefixes.prefixes.get(pref));
        } else {
            System.err.println("Incorrect IRI in abducible '" + abducible + "', only one delimeter ':' may be used - between prefix and name.");
            Application.finish(ExitCode.ERROR);
        }
        return "";
    }
}
