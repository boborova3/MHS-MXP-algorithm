package algorithms.hybrid;
import com.google.inject.internal.asm.$ClassReader;
import common.Configuration;
import common.Printer;
import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.knowledgeexploration.OWLKnowledgeExplorerReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import reasoner.AxiomManager;
import reasoner.ILoader;
import reasoner.IReasonerManager;
import uk.ac.manchester.cs.jfact.kernel.DlCompletionTree;
import uk.ac.manchester.cs.jfact.kernel.dl.interfaces.ConceptExpression;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

public class ModelExtractor {

    private ILoader loader;
    private IReasonerManager reasonerManager;
    private HybridSolver hybridSolver;
    private OWLOntologyManager ontologyManager;

    public ModelExtractor(ILoader loader, IReasonerManager reasonerManager, HybridSolver hybridSolver){
        this.loader = loader;
        this.reasonerManager = reasonerManager;
        this.hybridSolver = hybridSolver;
        this.ontologyManager = OWLManager.createOWLOntologyManager();

    }

    public ModelNode getNegModelByOntology(){  // mrozek
        OWLDataFactory dfactory = ontologyManager.getOWLDataFactory();
        ModelNode negModelNode = new ModelNode();
        ModelNode modelNode = new ModelNode();
        Set<OWLAxiom> negModelSet = new HashSet<>();
        Set<OWLAxiom> modelSet = new HashSet<>();

        if(!isOntologyConsistentWithPath()){
            modelNode.modelIsValid = false;
            negModelNode.modelIsValid = false;
            //modelNode.data = new HashSet<>();
            return modelNode;
        }

        ArrayList<OWLNamedIndividual> individualArray;
        if(loader.isAxiomBasedAbduciblesOnInput()){
            individualArray = new ArrayList<>(loader.getOntology().getIndividualsInSignature());
        } else {
            individualArray = new ArrayList<>(hybridSolver.abducibles.getIndividuals());
        }

//        for (OWLNamedIndividual ind : individualArray) {
//            assignTypesToIndividual(dfactory, ind, negModelSet, modelSet);
//        }
//        if (Configuration.ROLES_IN_EXPLANATIONS_ALLOWED) {
//            for (OWLNamedIndividual ind : individualArray) {
//                assignRolesToIndividual(dfactory, ind, individualArray, negModelSet, modelSet);
//            }
//        }

        if (Configuration.ROLES_IN_EXPLANATIONS_ALLOWED) {
            assignTypesAndRolesToIndividual(dfactory, individualArray, negModelSet, modelSet);
        } else {
            for (OWLNamedIndividual ind : individualArray) {
                assignTypesToIndividual(dfactory, ind, negModelSet, modelSet);
            }
        }

        deletePathFromOntology();

        if(loader.isAxiomBasedAbduciblesOnInput()){
            modelSet.retainAll(hybridSolver.abducibles.getAxiomBasedAbducibles());
            modelNode.data = modelSet;
            negModelSet.retainAll(hybridSolver.abducibles.getAxiomBasedAbducibles());
            negModelNode.data = negModelSet;

        } else {
            modelNode.data = modelSet;
            negModelNode.data = negModelSet;
        }

        hybridSolver.lastUsableModelIndex = hybridSolver.models.indexOf(modelNode);

        if (!modelNode.data.isEmpty() && hybridSolver.lastUsableModelIndex == -1) {
            hybridSolver.lastUsableModelIndex = hybridSolver.models.size();
            addModel(modelNode, negModelNode);
        }
        return negModelNode;
    }

    public boolean isOntologyConsistentWithPath(){
        if(hybridSolver.checkingMinimalityWithQXP) {
            return isOntologyConsistentWithPath(hybridSolver.pathDuringCheckingMinimality);
        }
        else {
            return isOntologyConsistentWithPath(hybridSolver.path);
        }
    }

    public boolean isOntologyConsistentWithPath(Set<OWLAxiom> path){
        if (path != null) {
            if(loader.isMultipleObservationOnInput()){
                for(OWLAxiom axiom : loader.getObservation().getAxiomsInMultipleObservations()){
                    path.remove(AxiomManager.getComplementOfOWLAxiom(loader, axiom));
                }
            } else {
                path.remove(hybridSolver.negObservation);
            }
            reasonerManager.addAxiomsToOntology(path);
            if (!reasonerManager.isOntologyConsistent()){
                hybridSolver.removeAxiomsFromOntology(path);
                return false;
            }
        }
        return true;
    }

    public void assignTypesToIndividual(OWLDataFactory dfactory, OWLNamedIndividual ind, Set<OWLAxiom> negModelSet, Set<OWLAxiom> modelSet){
        /**berie sa ontologia z hybridSolvera, co ale nie je menena ontologia, ako je v loader.getOntology()**/
        //System.out.println("INDIVIDUAL " + ind);
        Set<OWLClassExpression> ontologyTypes = EntitySearcher.getTypes(ind, hybridSolver.ontology).collect(toSet());
        //tu su zlozene koncepty priamo z ontologie, povodna ontologia

//        ontologyTypes.addAll(nodeClassSet2classExpSet(loader.getReasoner().getTypes(ind, false).getNodes()));
        //nenajde potom vsetky riesenia ??

        //Set<OWLClassExpression> ontologyTypes = EntitySearcher.getTypes(ind, loader.getOntology()).collect(toSet());
        //s tymto druhym to nefunguje, menena ontologia

//        System.out.println("ONTOLOGY TYPES " + ontologyTypes);

        Set<OWLClassExpression> knownTypes = new HashSet<>(); // to, kde ind urcite patri z ontologie
        Set<OWLClassExpression> knownNotTypes = new HashSet<>(); // to, kde ind nepatri z ontologie
        divideTypesAccordingOntology(ontologyTypes, knownTypes, knownNotTypes);

        Set<OWLClassExpression> newNotTypes = classSet2classExpSet(hybridSolver.ontology.classesInSignature().collect(toSet()));
//        System.out.println("NEW NOT TYPES " + newNotTypes); //triedy z ontologie
        newNotTypes.remove(dfactory.getOWLThing());
        newNotTypes.removeAll(knownNotTypes);

        OWLObjectOneOf individual = ontologyManager.getOWLDataFactory().getOWLObjectOneOf(ind);
        //OWLClassExpression nominal = individual.asObjectUnionOf();

        OWLKnowledgeExplorerReasoner.RootNode rootNode = loader.getReasoner().getRoot(individual);
        Set<OWLClassExpression> foundTypes = loader.getReasoner().getObjectLabel(rootNode,false)
                .entities()
                .collect(toSet());

        newNotTypes.removeAll(foundTypes);
        foundTypes.removeAll(knownTypes); // odstranime tie, co tam su vzdy

        addAxiomsToModelsAccordingTypes(dfactory, negModelSet, modelSet, foundTypes, newNotTypes, ind);
    }

    public void assignRolesToIndividual(OWLDataFactory dfactory, OWLNamedIndividual ind, ArrayList<OWLNamedIndividual> individuals, Set<OWLAxiom> negModelSet, Set<OWLAxiom> modelSet) {
        Set<OWLAxiom> ontologyPropertyAxioms = hybridSolver.ontology.axioms()
                .filter(a -> a.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION)
                        && ((OWLObjectPropertyAssertionAxiom)a).getSubject() == ind)
                .collect(toSet());

        ontologyPropertyAxioms.addAll(hybridSolver.ontology.axioms()
                .filter(a -> a.isOfType(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION)
                        && ((OWLNegativeObjectPropertyAssertionAxiom)a).getSubject() == ind)
                .collect(toSet()));

//        System.out.println("ontology axioms");   //OK
//        System.out.println(ontologyPropertyAxioms);

        Set<OWLObjectPropertyAssertionAxiom> known = new HashSet<>();
        Set<OWLObjectPropertyAssertionAxiom> knownNot = new HashSet<>();
        dividePropertyAxiomsAccordingOntology(ontologyPropertyAxioms, known, knownNot); //OK

        Set<OWLAxiom> newNot = hybridSolver.assertionsAxioms.stream()
                .filter(p -> p.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION)
                        && ((OWLObjectPropertyAssertionAxiom)p).getSubject() == ind)
                .collect(toSet());

//        System.out.println("NEW NOT");
//        printAxioms(new ArrayList<>(newNot));

        newNot.removeAll(knownNot);

        Set<OWLObjectPropertyAssertionAxiom> found = new HashSet<>();

        List<OWLKnowledgeExplorerReasoner.RootNode> nodes = new ArrayList<>();
        // PROBLEM TO DlCompletionTree, pripadne porovnavanie rootNode naprogramovat este??
        // neda sa dat root node ako key do mapy .. nefunguje equals

        for (OWLNamedIndividual n : individuals) {
            OWLObjectOneOf i = dfactory.getOWLObjectOneOf(n);
            nodes.add(loader.getReasoner().getRoot(i));
        }

        OWLObjectOneOf individual = ontologyManager.getOWLDataFactory().getOWLObjectOneOf(ind);
        OWLKnowledgeExplorerReasoner.RootNode rootNode = loader.getReasoner().getRoot(individual);
        Set<OWLObjectPropertyExpression> roles = loader.getReasoner().getObjectNeighbours(rootNode, false)
                .entities()
                .collect(toSet());

        for (OWLObjectPropertyExpression role : roles) {
            if (role.isOWLObjectProperty()) {
                Collection<OWLKnowledgeExplorerReasoner.RootNode> nodes2 = loader.getReasoner()
                        .getObjectNeighbours(rootNode, role.getNamedProperty());

                for (OWLKnowledgeExplorerReasoner.RootNode r : nodes2) {
//                    System.out.println("r");
                    //ver 1
//                    for (OWLKnowledgeExplorerReasoner.RootNode n : nodes) {
//                        if (r.getNode().equals(n)) {
//                            OWLNamedIndividual object = individuals.get(nodes.indexOf(n));
//                            found.add(dfactory.getOWLObjectPropertyAssertionAxiom(role, ind, object));
//                            break;
//                        }
//                    }

                    //ver 2
                    if (nodes.stream().anyMatch(p -> p.getNode().equals(r.getNode()))) {
                        OWLKnowledgeExplorerReasoner.RootNode n = nodes.stream().filter(p -> p.getNode().equals(r.getNode())).findFirst().get();
                        OWLNamedIndividual object = individuals.get(nodes.indexOf(n));
                        found.add(dfactory.getOWLObjectPropertyAssertionAxiom(role, ind, object));
                    }
                }
            }
        }

        newNot.removeAll(found);
        found.removeAll(known);

//        System.out.println("FOUND"); //ok?
//        printAxioms(new ArrayList<>(found));

        addAxiomsToModelsAccordingTypes(negModelSet, modelSet, found, newNot);
    }

    // TODO: hlavne spojazdnit !
    // TODO: tie known atd mozem urobim mimo forcyklu, to su take, co su vzdy (property, types asi nie)
    // mozno ani nebude treba ist cez tu rolu pomocnu, ak pojdeme cez individualy ?? -> asi nejde
    //nedava to vsetky vysvetlenia

    public void assignTypesAndRolesToIndividual(OWLDataFactory dfactory, ArrayList<OWLNamedIndividual> individuals, Set<OWLAxiom> negModelSet, Set<OWLAxiom> modelSet) {

//        final String ROLE = "#newrole/role";
//        OWLEntity entity = dfactory.getOWLEntity(EntityType.OBJECT_PROPERTY, IRI.create(ROLE));
//        OWLAxiom declarationAxiom = dfactory.getOWLDeclarationAxiom(entity);
//        OWLObjectPropertyExpression property = dfactory.getOWLObjectProperty(ROLE);
//        reasonerManager.addAxiomToOntology(declarationAxiom);

        List<OWLKnowledgeExplorerReasoner.RootNode> individualNodes = new ArrayList<>();
//        int count = 0;

        for (OWLNamedIndividual i : individuals) {

            OWLObjectOneOf obj = dfactory.getOWLObjectOneOf(i);
            individualNodes.add(loader.getReasoner().getRoot(obj));

//            count++;
//
//            if (count > 1) {
//                int second = count - 1;
//                int first = count - 2;
//
//                OWLObjectPropertyAssertionAxiom axiom1 = dfactory.getOWLObjectPropertyAssertionAxiom(property, individuals.get(first), individuals.get(second));
//                OWLObjectPropertyAssertionAxiom axiom2 = dfactory.getOWLObjectPropertyAssertionAxiom(property, individuals.get(second), individuals.get(first));
//                reasonerManager.addAxiomToOntology(axiom1);
//                reasonerManager.addAxiomToOntology(axiom2);
//            }
        }

        List<OWLKnowledgeExplorerReasoner.RootNode> queue = new ArrayList<>();
        queue.add(loader.getReasoner().getRoot(dfactory.getOWLObjectOneOf(individuals.get(0))));

        List<OWLKnowledgeExplorerReasoner.RootNode> visited = new ArrayList<>();
        Set<OWLObjectPropertyAssertionAxiom> foundPropertyAssertions = new HashSet<>();

        while (!queue.isEmpty()) {
            OWLKnowledgeExplorerReasoner.RootNode rootNode = queue.remove(0);

            Set<OWLObjectPropertyExpression> roles = loader.getReasoner().getObjectNeighbours(rootNode, false)
                    .entities()
                    .collect(toSet());

            if (!individualNodes.stream().anyMatch(p -> p.getNode().equals(rootNode.getNode()))) continue;

            OWLKnowledgeExplorerReasoner.RootNode n2 = individualNodes.stream()
                    .filter(p -> p.getNode().equals(rootNode.getNode())).findFirst().get();

            OWLNamedIndividual subject = individuals.get(individualNodes.indexOf(n2));

            for (OWLObjectPropertyExpression role : roles) {
                if (role.isOWLObjectProperty()) {

                    Collection<OWLKnowledgeExplorerReasoner.RootNode> neighbourNodes = loader.getReasoner()
                            .getObjectNeighbours(rootNode, role.getNamedProperty());

                    if (visited.stream().anyMatch(p -> p.getNode().equals(rootNode.getNode()))) continue;

                    for (OWLKnowledgeExplorerReasoner.RootNode r : neighbourNodes) {

                            //spracovat
                            if (individualNodes.stream().anyMatch(p -> p.getNode().equals(r.getNode()))) {
                                OWLKnowledgeExplorerReasoner.RootNode n = individualNodes.stream().filter(p -> p.getNode().equals(r.getNode())).findFirst().get();

                                OWLNamedIndividual object = individuals.get(individualNodes.indexOf(n));

                                //types
                                Set<OWLClassExpression> foundTypes = loader.getReasoner().getObjectLabel(rootNode,false).entities().collect(toSet());
                                addAxiomsToModelsAccordingTypes(dfactory, negModelSet, modelSet, foundTypes, subject);

                                foundPropertyAssertions.add(dfactory.getOWLObjectPropertyAssertionAxiom(role, subject, object));

                            }

                            queue.add(r);
                            visited.add(rootNode);
                    }
                }
            }
        }

        addAxiomsToModelsAccordingTypes(negModelSet, modelSet, foundPropertyAssertions);

    }

    public void divideTypesAccordingOntology(Set<OWLClassExpression> ontologyTypes, Set<OWLClassExpression> knownTypes, Set<OWLClassExpression> knownNotTypes){
        for (OWLClassExpression exp : ontologyTypes) {
            assert (exp.isClassExpressionLiteral());
            if (exp.isOWLClass()) {
                knownTypes.add((exp));
            } else {
                knownNotTypes.add(exp.getComplementNNF());
            }
        }
        //System.out.println("KNOWN TYPES "  + knownTypes);
        //System.out.println("KNOWN NOT TYPES "  + knownNotTypes);
    }

    private void dividePropertyAxiomsAccordingOntology(Set<OWLAxiom> ontologyAxioms, Set<OWLObjectPropertyAssertionAxiom> known, Set<OWLObjectPropertyAssertionAxiom> knownNot) {
        for (OWLAxiom assertionAxiom : ontologyAxioms) {
            if (assertionAxiom.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
                known.add((OWLObjectPropertyAssertionAxiom) assertionAxiom);
            }
            else if (assertionAxiom.isOfType(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION)) {
                knownNot.add((OWLObjectPropertyAssertionAxiom) AxiomManager.getComplementOfOWLAxiom(loader, assertionAxiom));
            }
        }

//        System.out.println("KNOWN");
//        System.out.println(known); //OK
//        System.out.println("KNOWN NOT");
//        System.out.println(knownNot); //OK
    }

    public static Set<OWLClassExpression> nodeClassSet2classExpSet(Set<Node<OWLClass>> nodeList) {
        Set<OWLClassExpression> toReturn = new HashSet<>();
        for (Node<OWLClass> node : nodeList) {
            toReturn.addAll(node.getEntitiesMinusTop());
        }
        return toReturn;
    }

    public static Set<OWLClassExpression> classSet2classExpSet(Set<OWLClass> classSet) {
        Set<OWLClassExpression> toReturn = new HashSet<>();
        toReturn.addAll(classSet);
        return toReturn;
    }

    public void addAxiomsToModelsAccordingTypes(OWLDataFactory dfactory, Set<OWLAxiom> negModelSet, Set<OWLAxiom> modelSet, Set<OWLClassExpression> foundTypes, Set<OWLClassExpression> newNotTypes, OWLNamedIndividual ind){

        for (OWLClassExpression classExpression : foundTypes) {
            if(!loader.isAxiomBasedAbduciblesOnInput()){
                if (!hybridSolver.abducibles.getClasses().contains(classExpression)){
                    continue;
                }
            }
            OWLClassExpression negClassExp = classExpression.getComplementNNF();
            OWLAxiom axiom = dfactory.getOWLClassAssertionAxiom(negClassExp, ind);
            OWLAxiom axiom1 = dfactory.getOWLClassAssertionAxiom(classExpression, ind);
            negModelSet.add(axiom);
            modelSet.add(axiom1);
        }

        for (OWLClassExpression classExpression : newNotTypes) {
            if(!loader.isAxiomBasedAbduciblesOnInput()){
                if (!hybridSolver.abducibles.getClasses().contains(classExpression)){
                    continue;
                }
            }
            OWLClassExpression negClassExp = classExpression.getComplementNNF();
            OWLAxiom axiom = dfactory.getOWLClassAssertionAxiom(classExpression, ind);
            OWLAxiom axiom1 = dfactory.getOWLClassAssertionAxiom(negClassExp, ind);
            negModelSet.add(axiom);
            modelSet.add(axiom1);
        }
    }

    public void addAxiomsToModelsAccordingTypes(Set<OWLAxiom> negModelSet, Set<OWLAxiom> modelSet, Set<OWLObjectPropertyAssertionAxiom> foundTypes, Set<OWLAxiom> newNotTypes){

        for (OWLObjectPropertyAssertionAxiom axiom : foundTypes) {
            if(!loader.isAxiomBasedAbduciblesOnInput()){
                if (!hybridSolver.abducibles.getRoles().contains(axiom.getProperty().getNamedProperty())) {
                    continue;
                }
            }
            OWLAxiom neg = AxiomManager.getComplementOfOWLAxiom(loader, axiom);
            negModelSet.add(neg);
            modelSet.add(axiom);
        }

        for (OWLAxiom axiom : newNotTypes) {
            if (!loader.isAxiomBasedAbduciblesOnInput()) {
                if (!hybridSolver.abducibles.getRoles().contains(((OWLObjectPropertyAssertionAxiom)axiom).getProperty().getNamedProperty())) {
                    continue;
                }
            }
            OWLAxiom neg = AxiomManager.getComplementOfOWLAxiom(loader, axiom);
            negModelSet.add(axiom);
            modelSet.add(neg);
        }

//        printAxioms(new ArrayList<>(modelSet));

    }

    //TODO toto je problem asi ??? stale nefunguje dobre .. chybaju niektore
    public void addAxiomsToModelsAccordingTypes(OWLDataFactory dfactory, Set<OWLAxiom> negModelSet, Set<OWLAxiom> modelSet, Set<OWLClassExpression> foundTypes, OWLNamedIndividual ind){

        Set<OWLClassExpression> ontologyTypes = EntitySearcher.getTypes(ind, hybridSolver.ontology).collect(toSet());
        Set<OWLClassExpression> knownTypes = new HashSet<>();
        Set<OWLClassExpression> knownNotTypes = new HashSet<>();
        divideTypesAccordingOntology(ontologyTypes, knownTypes, knownNotTypes);

        Set<OWLClassExpression> newNotTypes = classSet2classExpSet(hybridSolver.ontology.classesInSignature().collect(toSet()));
        newNotTypes.remove(dfactory.getOWLThing());
        newNotTypes.removeAll(knownNotTypes);
        newNotTypes.removeAll(foundTypes);

        foundTypes.removeAll(knownTypes);

        for (OWLClassExpression classExpression : foundTypes) {
            if(!loader.isAxiomBasedAbduciblesOnInput()){
                if (!hybridSolver.abducibles.getClasses().contains(classExpression)){
                    continue;
                }
            }
            OWLClassExpression negClassExp = classExpression.getComplementNNF();
            OWLAxiom axiom = dfactory.getOWLClassAssertionAxiom(negClassExp, ind);
            OWLAxiom axiom1 = dfactory.getOWLClassAssertionAxiom(classExpression, ind);
            negModelSet.add(axiom);
            modelSet.add(axiom1);
        }

        for (OWLClassExpression classExpression : newNotTypes) {
            if(!loader.isAxiomBasedAbduciblesOnInput()){
                if (!hybridSolver.abducibles.getClasses().contains(classExpression)){
                    continue;
                }
            }
            OWLClassExpression negClassExp = classExpression.getComplementNNF();
            OWLAxiom axiom = dfactory.getOWLClassAssertionAxiom(classExpression, ind);
            OWLAxiom axiom1 = dfactory.getOWLClassAssertionAxiom(negClassExp, ind);
            negModelSet.add(axiom);
            modelSet.add(axiom1);
        }

    }

    public void addAxiomsToModelsAccordingTypes(Set<OWLAxiom> negModelSet, Set<OWLAxiom> modelSet, Set<OWLObjectPropertyAssertionAxiom> foundTypes){

        Set<OWLAxiom> ontologyPropertyAxioms = hybridSolver.ontology.axioms()
                .filter(a -> a.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION))
                .collect(toSet());

        ontologyPropertyAxioms.addAll(hybridSolver.ontology.axioms()
                .filter(a -> a.isOfType(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION))
                .collect(toSet()));

        Set<OWLObjectPropertyAssertionAxiom> known = new HashSet<>();
        Set<OWLObjectPropertyAssertionAxiom> knownNot = new HashSet<>();
        dividePropertyAxiomsAccordingOntology(ontologyPropertyAxioms, known, knownNot);

        Set<OWLAxiom> newNot = hybridSolver.assertionsAxioms.stream()
                .filter(p -> p.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION))
                .collect(toSet());

        newNot.removeAll(knownNot);

        newNot.removeAll(foundTypes);
        foundTypes.removeAll(known);

        for (OWLObjectPropertyAssertionAxiom axiom : foundTypes) {
            if(!loader.isAxiomBasedAbduciblesOnInput()){
                if (!hybridSolver.abducibles.getRoles().contains(axiom.getProperty().getNamedProperty())) {
                    continue;
                }
            }
            OWLAxiom neg = AxiomManager.getComplementOfOWLAxiom(loader, axiom);
            negModelSet.add(neg);
            modelSet.add(axiom);
        }

        for (OWLAxiom axiom : newNot) {
            if (!loader.isAxiomBasedAbduciblesOnInput()) {
                if (!hybridSolver.abducibles.getRoles().contains(((OWLObjectPropertyAssertionAxiom)axiom).getProperty().getNamedProperty())) {
                    continue;
                }
            }
            OWLAxiom neg = AxiomManager.getComplementOfOWLAxiom(loader, axiom);
            negModelSet.add(axiom);
            modelSet.add(neg);
        }

//        printAxioms(new ArrayList<>(modelSet));

    }

    public void printAxioms(List<OWLAxiom> axioms){
        List<String> result = new ArrayList<>();
        for (OWLAxiom owlAxiom : axioms) {

            //owlAxiom.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION) || owlAxiom.isOfType(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION)
            if (owlAxiom.isOfType(AxiomType.CLASS_ASSERTION)) {
                result.add(Printer.print(owlAxiom));
            }
        }
        System.out.println("{" + StringUtils.join(result, ",") + "}");
    }

    public void deletePathFromOntology(){
        if(hybridSolver.checkingMinimalityWithQXP){
            hybridSolver.removeAxiomsFromOntology(hybridSolver.pathDuringCheckingMinimality);
        } else {
            hybridSolver.removeAxiomsFromOntology(hybridSolver.path);
        }
    }

    public void addModel(ModelNode model, ModelNode negModel){
        hybridSolver.lastUsableModelIndex = hybridSolver.models.indexOf(model);
        if (hybridSolver.lastUsableModelIndex != -1 || model.data.isEmpty()){
            return;
        }
        hybridSolver.lastUsableModelIndex = hybridSolver.models.size();
        hybridSolver.models.add(model);
        hybridSolver.negModels.add(negModel);
    }

    public ModelNode getNegModelByReasoner() { //nefunguje?
        ModelNode modelNode = new ModelNode();
        Set<OWLAxiom> model = new HashSet<>();

        if (hybridSolver.path != null) {
            hybridSolver.path.remove(hybridSolver.negObservation);
            reasonerManager.addAxiomsToOntology(hybridSolver.path);
            if (!reasonerManager.isOntologyConsistent()){
                hybridSolver.removeAxiomsFromOntology(hybridSolver.path);
                modelNode.data = model;
                modelNode.modelIsValid = false;
                return modelNode;
            }
        }

        for (int i = 0; i < hybridSolver.assertionsAxioms.size(); i++) {
            OWLAxiom axiom = hybridSolver.assertionsAxioms.get(i);
            OWLAxiom complementOfAxiom = hybridSolver.negAssertionsAxioms.get(i);
            if (loader.getOntology().containsAxiom(axiom)){
                model.add(axiom);
            }
            else if (loader.getOntology().containsAxiom(complementOfAxiom)){
                model.add(complementOfAxiom);
            }
            else if (!model.contains(axiom) && !model.contains(complementOfAxiom)){
                reasonerManager.addAxiomToOntology(axiom);
                boolean isConsistent = reasonerManager.isOntologyConsistent();
                reasonerManager.removeAxiomFromOntology(axiom);

                reasonerManager.addAxiomToOntology(complementOfAxiom);
                boolean isComplementConsistent = reasonerManager.isOntologyConsistent();
                reasonerManager.removeAxiomFromOntology(complementOfAxiom);

                if (!isComplementConsistent && isConsistent) {
                    model.add(axiom);
                    reasonerManager.addAxiomToOntology(axiom);
                } else if (isComplementConsistent){
                    model.add(complementOfAxiom);
                    reasonerManager.addAxiomToOntology(complementOfAxiom);
                }
                else {
                    modelNode.data.clear();
                    hybridSolver.removeAxiomsFromOntology(hybridSolver.path);
                    return modelNode;
                }
            }
        }
        hybridSolver.removeAxiomsFromOntology(hybridSolver.path);
        modelNode.data = new HashSet<>();
        for (OWLAxiom axiom: model){
            if (hybridSolver.abducibles.getIndividuals().containsAll(axiom.individualsInSignature().collect(Collectors.toList())) &&
                    hybridSolver.abducibles.getClasses().containsAll( axiom.classesInSignature().collect(Collectors.toList()))){
                modelNode.data.add(axiom);
            }
        }
        addModel(modelNode, getComplementOfModel(modelNode));
        return hybridSolver.negModels.get(hybridSolver.lastUsableModelIndex);
    }

    private ModelNode getComplementOfModel(ModelNode modelNode) {
        Set<OWLAxiom> model = modelNode.data;

        ModelNode negModelNode = new ModelNode();
        negModelNode.modelIsValid = modelNode.modelIsValid;

        Set<OWLAxiom> negModel = new HashSet<>();

        for (OWLAxiom axiom : model) {
            //nechana stara funkcia getComplementOfOWLAxiom2, kedze s touto castou kodu som nepracovala a neviem, ci to nieco ovplyvni
            OWLAxiom complement = AxiomManager.getComplementOfOWLAxiom2(loader, axiom);
            negModel.add(complement);
        }
        negModelNode.data = negModel;
        return negModelNode;
    }

}
