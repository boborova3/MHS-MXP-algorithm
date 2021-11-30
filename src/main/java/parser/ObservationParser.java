package parser;

import common.Configuration;
import common.DLSyntax;
import models.Individuals;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import reasoner.Loader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class ObservationParser implements IObservationParser {

    private Logger logger = Logger.getLogger(ObservationParser.class.getSimpleName());
    private Loader loader;
    private OWLOntology observationOntology;
    private boolean processMultipleObservation = false;
    private List<OWLAxiom> multipleObservations = new ArrayList<>();

    public ObservationParser(Loader loader){
        this.loader = loader;
    }

    @Override
    public void parse() throws Exception {
        try{
            createOntologyFromObservation();
        } catch (OWLOntologyCreationException e){
            throw new OWLOntologyCreationException("Invalid format of observation");
        } catch (OWLOntologyStorageException e){
            throw e;
        }
        logger.log(Level.INFO, "Observation: ".concat(Configuration.OBSERVATION));
    }

    private void createOntologyFromObservation() throws OWLOntologyCreationException, OWLOntologyStorageException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        observationOntology = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(Configuration.OBSERVATION));
        StringDocumentTarget documentTarget = new StringDocumentTarget();
        observationOntology.saveOntology(documentTarget);
        OWLDocumentFormat format = manager.getOntologyFormat(observationOntology);
        loader.setObservationOntologyFormat(format);
        processAxiomsFromObservation();
    }

    private void processAxiomsFromObservation(){
        Set<OWLAxiom> set = observationOntology.getAxioms();
        System.out.println(set);
        List<OWLAxiom> resultingObservation = new ArrayList<>();
        for (OWLAxiom axiom : set){
            //Ak by to bola negacia, platilo by to?? - PLATILO
            if(AxiomType.CLASS_ASSERTION == axiom.getAxiomType()) {
                resultingObservation.add(axiom);
                //System.out.println("ANO");
            }
        }
        if(resultingObservation.size() == 1){
            loader.setObservation(resultingObservation.get(0));
            processClassAssertionAxiom(resultingObservation.get(0));
        } else {
            processMultipleObservation = true;
            loader.setMultipleObservationOnInput(true);
            //System.out.println(resultingObservation);
            multipleObservations.addAll(resultingObservation);
            loader.setMultipleObservations(multipleObservations);
            OWLNamedIndividual reductionIndividual = loader.getDataFactory().getOWLNamedIndividual("http://www.co-ode.org/ontologies/ont.owl#" + DLSyntax.DELIMITER_ONTOLOGY + "s" + System.currentTimeMillis());
            loader.setReductionIndividual(reductionIndividual);
            //System.out.println(reductionIndividual);


            OWLClassExpression reductionClass = processMultipleObservations(resultingObservation);
            OWLAxiom observationAfterReduction = loader.getDataFactory().getOWLClassAssertionAxiom(reductionClass, reductionIndividual);
            loader.setObservation(observationAfterReduction);

            processClassAssertionAxiom(observationAfterReduction);
            //System.out.println(observationAfterReduction);
        }
        //differentiateIndividualsInOntology();
        //System.out.println(loader.getOntology());
        loader.initializeReasoner();
        //System.out.println("ONTOLOGY CONSISTENT " + loader.getReasoner().isConsistent());
        //OWLObjectOneOf individualFromAxiom = loader.getDataFactory().getOWLObjectOneOf(res.getIndividual());
        //OWLClassExpression differentIndividuals = individualFromAxiom.getObjectComplementOf();
    }

/*    private void differentiateIndividualsInOntology(){
        List<OWLNamedIndividual> individuals = loader.getIndividuals().getNamedIndividuals();
        List<OWLNamedIndividual> individuals2 = loader.getIndividuals().getNamedIndividuals();

        OWLAxiom a = loader.getDataFactory().getOWLDifferentIndividualsAxiom(individuals);
        loader.getOntologyManager().addAxiom(loader.getOntology(), a);
        loader.getOntologyManager().addAxiom(loader.getOriginalOntology(), a);

        for(int i = 0; i < individuals.size(); i++){
            for(int j = i + 1; j < individuals.size(); j++){
                if(individuals.get(i) != loader.getReductionIndividual() && individuals.get(j) != loader.getReductionIndividual()){
                    OWLAxiom a = loader.getDataFactory().getOWLDifferentIndividualsAxiom(individuals.get(i), individuals.get(j));
                    loader.getOntologyManager().addAxiom(loader.getOntology(), a);
                    loader.getOntologyManager().addAxiom(loader.getOriginalOntology(), a);
                }
            }
        }
    }*/

    private OWLClassExpression processMultipleObservations(List<OWLAxiom> resultingObservation){
        //vyberiem prvy
        OWLClassAssertionAxiom res = (OWLClassAssertionAxiom) resultingObservation.get(0);
        //urcim ze to nie je rovnaky individual
        OWLObjectOneOf individualFromAxiom = loader.getDataFactory().getOWLObjectOneOf(res.getIndividual());
        OWLClassExpression differentIndividuals = individualFromAxiom.getObjectComplementOf();
        //urcim ze to nie je rovnaky individual zjednoteny s rovnakou triedou
        OWLClassExpression union = loader.getDataFactory().getOWLObjectUnionOf(res.getClassExpression(), differentIndividuals);

        Set<OWLNamedIndividual> allInd = new HashSet<>();
        Set<OWLAxiom> axioms = loader.getOriginalOntology().getAxioms();

        for(OWLAxiom axiom : resultingObservation.subList(1, resultingObservation.size())){

            OWLClassAssertionAxiom temp = (OWLClassAssertionAxiom) axiom;
            OWLClassExpression axiomClass = temp.getClassExpression();

            OWLObjectOneOf individualFromAxiom2 = loader.getDataFactory().getOWLObjectOneOf(temp.getIndividual());
            //OWLSameIndividualAxiom individualFromAxiom2 = loader.getDataFactory().getOWLDifferentIndividualsAxiom();

            OWLClassExpression differentIndividuals2 = individualFromAxiom2.getObjectComplementOf();
            OWLObjectUnionOf union2 = loader.getDataFactory().getOWLObjectUnionOf(axiomClass, differentIndividuals2);

            union = loader.getDataFactory().getOWLObjectIntersectionOf(union, union2);
        }
        //System.out.println("UNION");
        //System.out.println(union);
        return union;
    }

    private void processClassAssertionAxiom(OWLAxiom axiom){
        OWLClassAssertionAxiom temp = (OWLClassAssertionAxiom) axiom;

        OWLClassExpression axiomClass = temp.getClassExpression();
        OWLNamedIndividual axiomIndividual = temp.getIndividual().asOWLNamedIndividual();
        List<OWLNamedIndividual> axiomIndividuals = temp.individualsInSignature().collect(Collectors.toList());

        for (OWLNamedIndividual i : axiomIndividuals){
            //System.out.println("NAMED INDIVIDUAL:");
            //System.out.println(i);

            /**RIESENIE PRE TO? ABY s0 nemobolo zahrnute vo vysvetleniach**/
            /*if(i != loader.getReductionIndividual() || !i.equals(loader.getReductionIndividual())){
                loader.addNamedIndividual(i);
            }*/

            loader.addNamedIndividual(i);

            if(processMultipleObservation && i != loader.getReductionIndividual()){
                //OWLAxiom a = loader.getDataFactory().getOWLDifferentIndividualsAxiom(i, loader.getReductionIndividual());
                //loader.getOntologyManager().addAxiom(loader.getOntology(), a);
                //loader.getOntologyManager().addAxiom(loader.getOriginalOntology(), a);
                //loader.initializeReasoner();
                //System.out.println(a);
                /*loader.getOntologyManager().addAxiom(loader.getOntology(), a);
                loader.getOntologyManager().addAxiom(loader.getOriginalOntology(), a);*/
            }
        }
        //loader.addNamedIndividual(axiomIndividual);
        loader.getOntologyManager().addAxiom(loader.getOntology(), loader.getDataFactory().getOWLDeclarationAxiom(axiomIndividual));
        loader.getOntologyManager().addAxiom(loader.getOriginalOntology(), loader.getDataFactory().getOWLDeclarationAxiom(axiomIndividual));


        System.out.println("AXIOM CLASS " + axiomClass);
        //OWLAxiom negAxiom = loader.getDataFactory().getOWLClassAssertionAxiom(axiomClass.getObjectComplementOf(), axiomIndividual);
        OWLAxiom negAxiom = loader.getDataFactory().getOWLClassAssertionAxiom(axiomClass.getComplementNNF(), axiomIndividual);
        loader.setNegObservation(negAxiom);
    }

/*    private OWLNamedIndividual parseIRIIndividual(OWLNamedIndividual namedIndividual){
        String[] pole = namedIndividual.getIRI().getIRIString().split(":");
        String hodnota = Prefixes.prefixes.get(pole[0]);
        String iri = hodnota + pole[1];
        System.out.println("SOM ABSOLUTE");
        OWLDataFactory manager = OWLManager.createOWLOntologyManager().getOWLDataFactory();
        return manager.getOWLNamedIndividual(iri);
        //namedIndividuals.add(individual);
    }

    private void parseAssertion(String[] expressions) {
        OWLExpression expression;
        OWLNamedIndividual namedIndividual = null;

        if (expressions[1].contains(DLSyntax.DELIMITER_OBJECT_PROPERTY)) {  // pre role, zatial netreba
            expression = parseObjectProperty(expressions);
        } else {
            if (!Prefixes.prefixes.values().stream().anyMatch(expressions[1]::startsWith)){
                if (!expressions[1].contains(":")){
                    System.err.println("Individual prefix in observation -o is unknown, define the prefix using -p parameter.");
                    Application.finish(ExitCode.ERROR);
                }
                String[] prefix_obj = expressions[1].split(DLSyntax.DELIMITER_ASSERTION);
                if (!Prefixes.prefixes.containsKey(prefix_obj[0])){
                    System.err.println("Prefix " + prefix_obj[0] + " in observation -o is unknown, define the prefix using -p parameter.");
                    Application.finish(ExitCode.ERROR);
                }
                expressions[1] = Prefixes.prefixes.get(prefix_obj[0]).concat(prefix_obj[1]);
            }
            namedIndividual = loader.getDataFactory().getOWLNamedIndividual(IRI.create(expressions[1]));
            loader.addNamedIndividual(namedIndividual);

            if (!Prefixes.prefixes.values().stream().anyMatch(expressions[0]::startsWith)){
                if (!expressions[0].contains(":")){
                    System.err.println("Concept prefix in observation -o is unknown, define the prefix using -p parameter.");
                    Application.finish(ExitCode.ERROR);
                }
                String[] prefix_obj = expressions[0].split(DLSyntax.DELIMITER_ASSERTION);
                if (!Prefixes.prefixes.containsKey(prefix_obj[0])){
                    System.err.println("Prefix " + prefix_obj[0] + " in observation -o is unknown, define the prefix with -p parameter.");
                    Application.finish(ExitCode.ERROR);
                }
                expressions[0] = Prefixes.prefixes.get(prefix_obj[0]).concat(prefix_obj[1]);
            }
            PostfixNotation postfixNotation = new PostfixNotation(expressions[0]);
            expression = parseExpression(postfixNotation.getPostfixExpression());

            loader.getOntologyManager().addAxiom(loader.getOntology(), loader.getDataFactory().getOWLDeclarationAxiom(namedIndividual));
            loader.getOntologyManager().addAxiom(loader.getOriginalOntology(), loader.getDataFactory().getOWLDeclarationAxiom(namedIndividual));
        }

        switch (expression.typ) {
            case CLASS_EXPRESSION:
                loader.setObservation(loader.getDataFactory().getOWLClassAssertionAxiom(expression.classExpression, namedIndividual));
                loader.setNegObservation(loader.getDataFactory().getOWLClassAssertionAxiom(expression.classExpression.getComplementNNF(), namedIndividual));
                break;

            case NEGATIVE_OBJECT_PROPERTY_ASSERTION:
                loader.setObservation(expression.negativeObjectPropertyAssertionAxiom);
                loader.setNegObservation(expression.objectPropertyAssertionAxiom);
                break;

            case OBJECT_PROPERTY_ASSERTION:
                loader.setObservation(expression.objectPropertyAssertionAxiom);
                loader.setNegObservation(expression.negativeObjectPropertyAssertionAxiom);
                break;

            default:
                break;
        }


        //TODO test if nominal needs to be added to ontology as individual if it is not already in
    }

    private OWLExpression parseObjectProperty(String[] expressions) {
        String[] individuals = expressions[0].split(DLSyntax.DELIMITER_INDIVIDUAL);

        OWLNamedIndividual subject = loader.getDataFactory().getOWLNamedIndividual(IRI.create(loader.getOntologyIRI().concat(DLSyntax.DELIMITER_ONTOLOGY).concat(individuals[0])));
        OWLNamedIndividual object = loader.getDataFactory().getOWLNamedIndividual(IRI.create(loader.getOntologyIRI().concat(DLSyntax.DELIMITER_ONTOLOGY).concat(individuals[1])));

        loader.getOntologyManager().addAxiom(loader.getOntology(), loader.getDataFactory().getOWLDeclarationAxiom(subject));
        loader.getOntologyManager().addAxiom(loader.getOntology(), loader.getDataFactory().getOWLDeclarationAxiom(object));
        // pridanie prefixu k roli? -> vytvorenie roly
        OWLObjectProperty objectProperty = loader.getDataFactory().getOWLObjectProperty(IRI.create(loader.getOntologyIRI().concat(DLSyntax.DELIMITER_ONTOLOGY).concat(expressions[1])));

        loader.addNamedIndividual(subject);
        loader.addNamedIndividual(object);

        OWLExpression expression = new OWLExpression();
        // vytvorenie role a jej negacie
        OWLObjectPropertyAssertionAxiom objectPropertyAssertionAxiom = loader.getDataFactory().getOWLObjectPropertyAssertionAxiom(objectProperty, subject, object);
        OWLNegativeObjectPropertyAssertionAxiom negativeObjectPropertyAssertionAxiom = loader.getDataFactory().getOWLNegativeObjectPropertyAssertionAxiom(objectProperty, subject, object);

        if (containsNegation(expressions[1])) {
            expression.negativeObjectPropertyAssertionAxiom = negativeObjectPropertyAssertionAxiom;
            expression.objectPropertyAssertionAxiom = objectPropertyAssertionAxiom;
            expression.typ = OWLTyp.NEGATIVE_OBJECT_PROPERTY_ASSERTION;
        } else {
            expression.objectPropertyAssertionAxiom = objectPropertyAssertionAxiom;
            expression.negativeObjectPropertyAssertionAxiom = negativeObjectPropertyAssertionAxiom;
            expression.typ = OWLTyp.OBJECT_PROPERTY_ASSERTION;
        }

        return expression;
    }

    private OWLExpression parseExpression(List<String> postfixExpression) {
        Stack<OWLExpression> stack = new Stack<>();

        if (postfixExpression.size() == 1) {
            OWLExpression expression = new OWLExpression();

            expression.classExpression = createClassExpression(postfixExpression.get(0));
            expression.typ = OWLTyp.CLASS_EXPRESSION;

            return expression;
        } else if (postfixExpression.size() == 3 && isRole(postfixExpression.get(2))) {
            OWLExpression expression = new OWLExpression();

            String left = postfixExpression.get(0);
            String right = postfixExpression.get(1);

            if (containsNegation(left)) {
                expression.negativeObjectPropertyAssertionAxiom = createNegativeObjectPropertyAssertionAxiom(left, right);
                expression.objectPropertyAssertionAxiom = createObjectPropertyAssertionAxiom(left, right);
                expression.typ = OWLTyp.NEGATIVE_OBJECT_PROPERTY_ASSERTION;
            } else {
                expression.objectPropertyAssertionAxiom = createObjectPropertyAssertionAxiom(left, right);
                expression.negativeObjectPropertyAssertionAxiom = createNegativeObjectPropertyAssertionAxiom(left, right);
                expression.typ = OWLTyp.OBJECT_PROPERTY_ASSERTION;
            }

            return expression;
        }

        for (String token : postfixExpression) {
            if (!isOperator(token)) {
                OWLExpression expression = new OWLExpression();

                if (isNominal(token)) {
                    String nominal = token.split(DLSyntax.DELIMITER_EXPRESSION)[1];
                    OWLNamedIndividual namedIndividual = createNamedIndividual(nominal);
                    OWLObjectOneOf objectOneOf = loader.getDataFactory().getOWLObjectOneOf(namedIndividual);
                    expression.classExpression = objectOneOf.asObjectUnionOf();
                    expression.typ = OWLTyp.CLASS_EXPRESSION;
                } else {

                    expression.token = token;
                    expression.typ = OWLTyp.TOKEN_NOT_DEFINED;
                }

                stack.push(expression);

            } else {
                OWLExpression right = stack.pop();
                OWLExpression left = stack.pop();

                if (right.typ == OWLTyp.TOKEN_NOT_DEFINED) {
                    right.classExpression = createClassExpression(right.token);
                    right.token = null;
                    right.typ = OWLTyp.CLASS_EXPRESSION;
                }

                switch (token) {
                    case DLSyntax.CONJUNCTION:
                        OWLExpression intersection = new OWLExpression();

                        if (left.typ == OWLTyp.TOKEN_NOT_DEFINED) {
                            left.classExpression = createClassExpression(left.token);
                            left.token = null;
                            left.typ = OWLTyp.CLASS_EXPRESSION;
                        }

                        intersection.classExpression = createIntersectionOf(right, left);
                        intersection.typ = OWLTyp.CLASS_EXPRESSION;

                        stack.push(intersection);
                        break;

                    case DLSyntax.DISJUNCTION:
                        OWLExpression union = new OWLExpression();

                        if (left.typ == OWLTyp.TOKEN_NOT_DEFINED) {
                            left.classExpression = createClassExpression(left.token);
                            left.token = null;
                            left.typ = OWLTyp.CLASS_EXPRESSION;
                        }

                        union.classExpression = createUnionOf(right, left);
                        union.typ = OWLTyp.CLASS_EXPRESSION;

                        stack.push(union);
                        break;

                    case DLSyntax.EXISTS:
                        left.objectProperty = createObjectPropertyExpression(left.token);
                        left.token = null;
                        left.typ = OWLTyp.OBJECT_PROPERTY;

                        OWLObjectSomeValuesFrom objectSomeValuesFrom = createExistentialRestriction(right, left);
                        OWLExpression someValues = new OWLExpression();

                        someValues.classExpression = objectSomeValuesFrom;
                        someValues.typ = OWLTyp.CLASS_EXPRESSION;

                        stack.push(someValues);
                        break;

                    case DLSyntax.FOR_ALL:
                        left.objectProperty = createObjectPropertyExpression(left.token);
                        left.token = null;
                        left.typ = OWLTyp.OBJECT_PROPERTY;

                        OWLObjectAllValuesFrom objectAllValuesFrom = createValueRestriction(right, left);
                        OWLExpression allValues = new OWLExpression();

                        allValues.classExpression = objectAllValuesFrom;
                        allValues.typ = OWLTyp.CLASS_EXPRESSION;

                        stack.push(allValues);
                        break;

                    default:
                        throw new RuntimeException("Invalid operator ( " + token + " )");
                }
            }
        }

        return stack.pop();
    }

    private boolean isOperator(String token) {
        return token.equals(DLSyntax.CONJUNCTION) || token.equals(DLSyntax.DISJUNCTION) || token.equals(DLSyntax.EXISTS) || token.equals(DLSyntax.FOR_ALL);
    }

    private OWLObjectIntersectionOf createIntersectionOf(OWLExpression right, OWLExpression left) {
        Set<OWLClassExpression> intersectionOfConcepts = new HashSet<>();

        List<OWLExpression> expressions = new ArrayList<>();
        expressions.add(right);
        expressions.add(left);

        for (OWLExpression expression : expressions) {
            if (expression.typ == OWLTyp.CLASS_EXPRESSION) {
                intersectionOfConcepts.add(expression.classExpression);
            } else {
                throw new RuntimeException("Intersection: Wrong type: typ = " + expression.typ.toString());
            }
        }

        return loader.getDataFactory().getOWLObjectIntersectionOf(intersectionOfConcepts);
    }

    private OWLObjectUnionOf createUnionOf(OWLExpression right, OWLExpression left) {
        Set<OWLClassExpression> unionOfConcepts = new HashSet<>();

        List<OWLExpression> expressions = new ArrayList<>();
        expressions.add(right);
        expressions.add(left);

        for (OWLExpression expression : expressions) {
            if (expression.typ == OWLTyp.CLASS_EXPRESSION) {
                unionOfConcepts.add(expression.classExpression);
            } else {
                throw new RuntimeException("Union: Wrong type: typ = " + expression.typ.toString());
            }
        }

        return loader.getDataFactory().getOWLObjectUnionOf(unionOfConcepts);
    }

    private OWLObjectSomeValuesFrom createExistentialRestriction(OWLExpression right, OWLExpression left) {
        if (right.typ == OWLTyp.CLASS_EXPRESSION) {
            return loader.getDataFactory().getOWLObjectSomeValuesFrom(left.objectProperty, right.classExpression);
        }

        throw new RuntimeException("SomeValuesFrom: Wrong right expression: typ = " + right.typ.toString());
    }

    private OWLObjectAllValuesFrom createValueRestriction(OWLExpression right, OWLExpression left) {
        if (right.typ == OWLTyp.CLASS_EXPRESSION) {
            return loader.getDataFactory().getOWLObjectAllValuesFrom(left.objectProperty, right.classExpression);
        }

        throw new RuntimeException("All values from: Wrong right expression: typ = " + right.typ.toString());
    }

    private OWLClassExpression createClassExpression(String name) {
        OWLClassExpression classExpression;

        if (containsNegation(name)) {
            String className = name.split(DLSyntax.DELIMITER_EXPRESSION)[1];
            classExpression = loader.getDataFactory().getOWLClass(IRI.create(className)).getComplementNNF();
        } else {
            classExpression = loader.getDataFactory().getOWLClass(IRI.create(name));
        }

        return classExpression;
    }

    private OWLObjectProperty createObjectPropertyExpression(String name) {
        OWLObjectProperty objectProperty;

        if (containsNegation(name)) {
            String objectPropertyName = name.split(DLSyntax.DELIMITER_EXPRESSION)[1];
            objectProperty = loader.getDataFactory().getOWLObjectProperty(IRI.create(loader.getOntologyIRI().concat(DLSyntax.DELIMITER_ONTOLOGY).concat(objectPropertyName)));
        } else {
            objectProperty = loader.getDataFactory().getOWLObjectProperty(IRI.create(loader.getOntologyIRI().concat(DLSyntax.DELIMITER_ONTOLOGY).concat(name)));
        }

        return objectProperty;
    }

    private OWLNamedIndividual createNamedIndividual(String name) {
        return loader.getDataFactory().getOWLNamedIndividual(IRI.create(loader.getOntologyIRI().concat(DLSyntax.DELIMITER_ONTOLOGY).concat(name)));
    }

    private OWLObjectPropertyAssertionAxiom createObjectPropertyAssertionAxiom(String left, String right) {
        OWLObjectProperty objectProperty = createObjectPropertyExpression(left);

        OWLNamedIndividual subject = loader.getIndividuals().getNamedIndividuals().get(0);
        OWLNamedIndividual object = loader.getDataFactory().getOWLNamedIndividual(IRI.create(loader.getOntologyIRI().concat(DLSyntax.DELIMITER_ONTOLOGY).concat(right)));

        loader.addNamedIndividual(object);
        loader.getOntologyManager().addAxiom(loader.getOntology(), loader.getDataFactory().getOWLDeclarationAxiom(object));

        return loader.getDataFactory().getOWLObjectPropertyAssertionAxiom(objectProperty, subject, object);
    }

    private OWLNegativeObjectPropertyAssertionAxiom createNegativeObjectPropertyAssertionAxiom(String left, String right) {
        OWLObjectProperty objectProperty = createObjectPropertyExpression(left);

        OWLNamedIndividual subject = loader.getIndividuals().getNamedIndividuals().get(0);
        OWLNamedIndividual object = loader.getDataFactory().getOWLNamedIndividual(IRI.create(loader.getOntologyIRI().concat(DLSyntax.DELIMITER_ONTOLOGY).concat(right)));

        loader.addNamedIndividual(object);
        loader.getOntologyManager().addAxiom(loader.getOntology(), loader.getDataFactory().getOWLDeclarationAxiom(object));

        return loader.getDataFactory().getOWLNegativeObjectPropertyAssertionAxiom(objectProperty, subject, object);
    }

    private boolean containsNegation(String concept) {
        return concept.contains(DLSyntax.NEGATION);
    }

    private boolean isNominal(String nominal) {
        return nominal.startsWith(DLSyntax.NOMINAL);
    }

    private boolean isRole(String role) {
        String possibleRole = role;

        if (containsNegation(role)) {
            possibleRole = role.split(DLSyntax.DELIMITER_EXPRESSION)[1];
        }

        return possibleRole.equals(DLSyntax.EXISTS) || possibleRole.equals(DLSyntax.FOR_ALL);
    }

    private String get_whole_name(String name){
        if (!name.contains(DLSyntax.DELIMITER_ONTOLOGY)){
            String[] prefix_obj = name.split(DLSyntax.DELIMITER_ASSERTION);
            if (!Prefixes.prefixes.containsKey(prefix_obj[0])){
                System.err.println("Prefix " + prefix_obj[0] + " in observation -o is unknown, define the prefix with -p parameter.");
                Application.finish(ExitCode.ERROR);
            }
            return Prefixes.prefixes.get(prefix_obj[0]).concat(prefix_obj[1]);
        }
        return name;
    }*/
}
