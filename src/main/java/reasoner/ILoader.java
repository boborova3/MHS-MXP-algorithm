package reasoner;

import java.util.*;
import models.Abducibles;
import models.Individuals;
import models.Observation;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.knowledgeexploration.OWLKnowledgeExplorerReasoner;


public interface ILoader {

    void initialize(ReasonerType reasonerType) throws Exception;

    void changeReasoner(ReasonerType reasonerType);

    void initializeReasoner();

    Observation getObservation();

    void setObservation(OWLAxiom observation);

    void setObservation(OWLAxiom observation, List<OWLAxiom> axiomsInMultipleObservations, OWLNamedIndividual reductionIndividual);

    Observation getNegObservation();

    void setNegObservation(OWLAxiom negObservation);

    OWLOntologyManager getOntologyManager();

    OWLOntology getOntology();

    OWLKnowledgeExplorerReasoner getReasoner();

    void setOWLReasonerFactory(OWLReasonerFactory reasonerFactory);

    String getOntologyIRI();

    OWLDataFactory getDataFactory();

    Individuals getIndividuals();

    void addNamedIndividual(OWLNamedIndividual namedIndividual);

    OWLOntology getOriginalOntology();

    Abducibles getAbducibles();

    boolean isMultipleObservationOnInput();

    boolean isAxiomBasedAbduciblesOnInput();
}
