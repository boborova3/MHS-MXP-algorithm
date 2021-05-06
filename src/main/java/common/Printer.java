package common;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Printer {

    public static String print(OWLAxiom owlAxiom) {
        if (owlAxiom instanceof OWLClassAssertionAxiom) {
            return Printer.getClassAssertionAxiom(owlAxiom).concat(DLSyntax.LEFT_PARENTHESES).
                    concat(Printer.getNamedIndividual(owlAxiom)).concat(DLSyntax.RIGHT_PARENTHESES);
//            return Printer.getNamedIndividual(owlAxiom).concat(DLSyntax.DELIMITER_ASSERTION).concat(Printer.getClassAssertionAxiom(owlAxiom));
        }
        return Printer.getObjectPropertyAssertionAxiom(owlAxiom);
    }

    private static String getNamedIndividual(OWLAxiom owlAxiom) {
        List<String> owlNamedIndividuals = new ArrayList<>();
        List<OWLNamedIndividual> individualsInSignature = owlAxiom.individualsInSignature().collect(Collectors.toList());

        for (OWLNamedIndividual owlNamedIndividual : individualsInSignature) {
            owlNamedIndividuals.add(owlNamedIndividual.getIRI().getFragment());
        }

        return StringUtils.join(owlNamedIndividuals, DLSyntax.DELIMITER_INDIVIDUAL);
    }

    public static String getClassAssertionAxiom(OWLAxiom owlAxiom) {
        List<OWLAxiom> axioms = new ArrayList<>();
        axioms.add(owlAxiom);

        Set<OWLAxiom> classAssertionAxioms = AxiomType.getAxiomsOfTypes(axioms, AxiomType.CLASS_ASSERTION);
        List<String> classAssertions = new ArrayList<>();

        for (OWLAxiom classAssertionAxiom : classAssertionAxioms) {
            OWLClassExpression owlClassExpression = ((OWLClassAssertionAxiom) classAssertionAxiom).getClassExpression();

            if (owlClassExpression instanceof OWLClass) {
                classAssertions.add(owlClassExpression.asOWLClass().getIRI().getFragment());
            } else if (owlClassExpression instanceof OWLObjectComplementOf) {
                classAssertions.add(DLSyntax.DISPLAY_NEGATION + owlClassExpression.getComplementNNF().asOWLClass().getIRI().getFragment());
            }
        }

        return StringUtils.join(classAssertions, " ");
    }

    private static String getObjectPropertyAssertionAxiom(OWLAxiom owlAxiom) {
        String property = "";
        String subject = "";
        String object = "";

        if (OWLObjectPropertyAssertionAxiom.class.isAssignableFrom(owlAxiom.getClass())) {
            OWLObjectPropertyAssertionAxiom objectPropertyAssertionAxiom = (OWLObjectPropertyAssertionAxiom) owlAxiom;

            property = objectPropertyAssertionAxiom.getProperty().getNamedProperty().getIRI().getFragment();

            if (objectPropertyAssertionAxiom.getSubject().isOWLNamedIndividual()) {
                subject = objectPropertyAssertionAxiom.getSubject().asOWLNamedIndividual().getIRI().getFragment();
            }

            if (objectPropertyAssertionAxiom.getObject().isOWLNamedIndividual()) {
                object = objectPropertyAssertionAxiom.getObject().asOWLNamedIndividual().getIRI().getFragment();
            }

        } else if (OWLNegativeObjectPropertyAssertionAxiom.class.isAssignableFrom(owlAxiom.getClass())) {
            OWLNegativeObjectPropertyAssertionAxiom negativeObjectPropertyAssertionAxiom = (OWLNegativeObjectPropertyAssertionAxiom) owlAxiom;

            property = DLSyntax.DISPLAY_NEGATION + negativeObjectPropertyAssertionAxiom.getProperty().getNamedProperty().getIRI().getFragment();

            if (negativeObjectPropertyAssertionAxiom.getSubject().isOWLNamedIndividual()) {
                subject = negativeObjectPropertyAssertionAxiom.getSubject().asOWLNamedIndividual().getIRI().getFragment();
            }

            if (negativeObjectPropertyAssertionAxiom.getObject().isOWLNamedIndividual()) {
                object = negativeObjectPropertyAssertionAxiom.getObject().asOWLNamedIndividual().getIRI().getFragment();
            }
        }

        return property.concat(DLSyntax.LEFT_PARENTHESES).concat(subject).concat(DLSyntax.DELIMITER_OBJECT_PROPERTY).concat(object).concat(DLSyntax.RIGHT_PARENTHESES);
    }
}
