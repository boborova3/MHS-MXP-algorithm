package parser;

import application.Application;
import application.ExitCode;
import common.Configuration;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import reasoner.Loader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ObservationParser implements IObservationParser {

    private Logger logger = Logger.getLogger(ObservationParser.class.getSimpleName());
    private Loader loader;
    private OWLOntology observationOntology;

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

        //variable "format" - used in PrefixesParser
        OWLDocumentFormat format = manager.getOntologyFormat(observationOntology);
        loader.setObservationOntologyFormat(format);

        processAxiomsFromObservation();
    }

    private void processAxiomsFromObservation(){
        Set<OWLAxiom> set = observationOntology.getAxioms();
        List<OWLAxiom> resultingObservation = new ArrayList<>();

        for (OWLAxiom axiom : set){
            AxiomType type = axiom.getAxiomType();
            if(AxiomType.CLASS_ASSERTION == type || AxiomType.OBJECT_PROPERTY_ASSERTION == type || AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION == type) {
                resultingObservation.add(axiom);
            }
        }

        if(resultingObservation.size() > 0){
            chooseProcessingObservationAccordingType(resultingObservation);
        } else {
            System.err.println("In -o is not founded any correct observation - class assertion, object property assertion or negative object property assertion.");
            Application.finish(ExitCode.ERROR);
        }
    }

    private void chooseProcessingObservationAccordingType(List<OWLAxiom> observationsAxioms){
        //single obs / complex obs
        if(observationsAxioms.size() == 1){
            loader.setObservation(observationsAxioms.get(0));

            if(observationsAxioms.get(0).getAxiomType() == AxiomType.CLASS_ASSERTION){
                processClassAssertionAxiom( (OWLClassAssertionAxiom) observationsAxioms.get(0));

            } else if (observationsAxioms.get(0).getAxiomType() == AxiomType.OBJECT_PROPERTY_ASSERTION){
                processObjectPropertyAssertionAxiom( (OWLObjectPropertyAssertionAxiom) observationsAxioms.get(0));

            } else if (observationsAxioms.get(0).getAxiomType() == AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION){
                processNegativeObjectPropertyAssertionAxiom( (OWLNegativeObjectPropertyAssertionAxiom) observationsAxioms.get(0));
            }

        //multiple obs
        } else {
            loader.setMultipleObservationOnInput(true);

            OWLNamedIndividual reductionIndividual = loader.getDataFactory().getOWLNamedIndividual("http://www.co-ode.org/ontologies/ont.owl#s" + System.currentTimeMillis());
            OWLClassExpression reductionClass = createReducedClassFromMultipleObservations(observationsAxioms);
            processMultipleObservations(reductionClass, observationsAxioms, reductionIndividual);
        }

        loader.initializeReasoner();
    }

    private void processClassAssertionAxiom(OWLClassAssertionAxiom axiom){
        OWLClassExpression axiomClass = axiom.getClassExpression();
        OWLNamedIndividual axiomIndividual = axiom.getIndividual().asOWLNamedIndividual();

        addIndividualToLoader(axiomIndividual);

        OWLAxiom negAxiom = loader.getDataFactory().getOWLClassAssertionAxiom(axiomClass.getComplementNNF(), axiomIndividual);
        loader.setNegObservation(negAxiom);
    }

    private void processObjectPropertyAssertionAxiom(OWLObjectPropertyAssertionAxiom axiom){
        OWLObjectPropertyExpression axiomProperty = axiom.getProperty();
        OWLNamedIndividual axiomSubject = axiom.getSubject().asOWLNamedIndividual();
        OWLNamedIndividual axiomObject = axiom.getObject().asOWLNamedIndividual();

        addIndividualToLoader(axiomSubject);
        addIndividualToLoader(axiomObject);

        OWLAxiom negAxiom = loader.getDataFactory().getOWLNegativeObjectPropertyAssertionAxiom(axiomProperty, axiomSubject, axiomObject);
        loader.setNegObservation(negAxiom);
    }

    private void processNegativeObjectPropertyAssertionAxiom(OWLNegativeObjectPropertyAssertionAxiom axiom){
        OWLObjectPropertyExpression axiomProperty = axiom.getProperty();
        OWLNamedIndividual axiomSubject = axiom.getSubject().asOWLNamedIndividual();
        OWLNamedIndividual axiomObject = axiom.getObject().asOWLNamedIndividual();

        addIndividualToLoader(axiomSubject);
        addIndividualToLoader(axiomObject);

        OWLAxiom negAxiom = loader.getDataFactory().getOWLObjectPropertyAssertionAxiom(axiomProperty, axiomSubject, axiomObject);
        loader.setNegObservation(negAxiom);
    }


    private void addIndividualToLoader(OWLNamedIndividual individual){
        loader.addNamedIndividual(individual);
        loader.getOntologyManager().addAxiom(loader.getOntology(), loader.getDataFactory().getOWLDeclarationAxiom(individual));
        loader.getOntologyManager().addAxiom(loader.getOriginalOntology(), loader.getDataFactory().getOWLDeclarationAxiom(individual));
    }

    private OWLClassExpression createReducedClassFromMultipleObservations(List<OWLAxiom> resultingObservation){
        List<OWLClassExpression> unions = new ArrayList<>();

        for(OWLAxiom axiom : resultingObservation){
            OWLClassExpression union = createOneUnion(axiom);
            unions.add(union);
        }

        return loader.getDataFactory().getOWLObjectIntersectionOf(unions);
    }

    private OWLClassExpression createOneUnion(OWLAxiom axiom){
        OWLClassExpression union = null;

        if(axiom.getAxiomType() == AxiomType.CLASS_ASSERTION){
            OWLClassAssertionAxiom classAssertionAxiom = (OWLClassAssertionAxiom) axiom;
            //urcim ze to nie je rovnaky individual
            OWLObjectOneOf individualFromAxiom = loader.getDataFactory().getOWLObjectOneOf(classAssertionAxiom.getIndividual());
            OWLClassExpression differentIndividuals = individualFromAxiom.getObjectComplementOf();

            union = loader.getDataFactory().getOWLObjectUnionOf(classAssertionAxiom.getClassExpression(), differentIndividuals);

        } else if (axiom.getAxiomType() == AxiomType.OBJECT_PROPERTY_ASSERTION){
            OWLObjectPropertyAssertionAxiom objectPropertyAssertionAxiom = (OWLObjectPropertyAssertionAxiom) axiom;

            OWLObjectOneOf individualFromAxiom = loader.getDataFactory().getOWLObjectOneOf(objectPropertyAssertionAxiom.getSubject());
            OWLClassExpression differentIndividuals = individualFromAxiom.getObjectComplementOf();

            OWLObjectHasValue existentialRestriction = loader.getDataFactory().getOWLObjectHasValue(objectPropertyAssertionAxiom.getProperty(), objectPropertyAssertionAxiom.getObject());
            union = loader.getDataFactory().getOWLObjectUnionOf(existentialRestriction, differentIndividuals);

        } else if (axiom.getAxiomType() == AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION){
            OWLNegativeObjectPropertyAssertionAxiom negativeObjectPropertyAssertionAxiom = (OWLNegativeObjectPropertyAssertionAxiom) axiom;

            OWLObjectOneOf individualFromAxiom = loader.getDataFactory().getOWLObjectOneOf(negativeObjectPropertyAssertionAxiom.getSubject());
            OWLClassExpression differentIndividuals = individualFromAxiom.getObjectComplementOf();

            OWLObjectOneOf objectFromAxiom = loader.getDataFactory().getOWLObjectOneOf(negativeObjectPropertyAssertionAxiom.getObject());
            OWLClassExpression differentObject = objectFromAxiom.getObjectComplementOf();

            OWLObjectAllValuesFrom valueRestriction = loader.getDataFactory().getOWLObjectAllValuesFrom(negativeObjectPropertyAssertionAxiom.getProperty(), differentObject);
            union = loader.getDataFactory().getOWLObjectUnionOf(valueRestriction, differentIndividuals);
        }

        if(union == null){
            System.err.println("In -o one of the multiple observations is different from class assertion, object property assertion or negative object property assertion axiom.");
            Application.finish(ExitCode.ERROR);
        }
        return union;
    }

    private void processMultipleObservations(OWLClassExpression reductionClass, List<OWLAxiom> observationsAxioms, OWLNamedIndividual reductionIndividual){
        OWLAxiom observationAfterReduction = loader.getDataFactory().getOWLClassAssertionAxiom(reductionClass, reductionIndividual);
        loader.setObservation(observationAfterReduction, observationsAxioms, reductionIndividual);
        processClassAssertionAxiom( (OWLClassAssertionAxiom) observationAfterReduction);

        Set<OWLNamedIndividual> individualsUsedAsNominal = reductionClass.getIndividualsInSignature();
        for(OWLNamedIndividual i : individualsUsedAsNominal){
            addIndividualToLoader(i);
        }

        //asi to tu nemusi byt
        addIndividualToLoader(reductionIndividual);

        OWLAxiom negAxiom = loader.getDataFactory().getOWLClassAssertionAxiom(reductionClass.getComplementNNF(), reductionIndividual);
        loader.setNegObservation(negAxiom);
    }
}

