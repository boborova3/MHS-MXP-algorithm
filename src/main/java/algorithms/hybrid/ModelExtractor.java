package algorithms.hybrid;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.search.EntitySearcher;
import reasoner.AxiomManager;
import reasoner.ILoader;
import reasoner.IReasonerManager;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

public class ModelExtractor {

    private ILoader loader;
    private IReasonerManager reasonerManager;
    private HybridSolver hybridSolver;

    public ModelExtractor(ILoader loader, IReasonerManager reasonerManager, HybridSolver hybridSolver){
        this.loader = loader;
        this.reasonerManager = reasonerManager;
        this.hybridSolver = hybridSolver;
    }

    public ModelNode getNegModelByOntology(){  // mrozek
        OWLDataFactory dfactory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
        ModelNode negModelNode = new ModelNode();
        ModelNode modelNode = new ModelNode();
        modelNode.data = new LinkedList<>();
        Set<OWLAxiom> negModelSet = new HashSet<>();
        Set<OWLAxiom> modelSet = new HashSet<>();

        if(!isOntologyConsistentWithPath()){
            return modelNode;
        }

        ArrayList<OWLNamedIndividual> individualArray = new ArrayList<>(hybridSolver.abducibles.getIndividuals());

        for (OWLNamedIndividual ind : individualArray) {
            assignTypesToIndividual(dfactory, ind, negModelSet, modelSet);
        }

        deletePathFromOntology();

        modelNode.data = new LinkedList<>(modelSet);
        negModelNode.data = new LinkedList<>(negModelSet);
        hybridSolver.lastUsableModelIndex = hybridSolver.models.indexOf(modelNode);
        System.out.println("MODEL NODE");
        hybridSolver.printAxioms(modelNode.data);
        System.out.println("NEG MODEL NODE");
        hybridSolver.printAxioms(negModelNode.data);

        if (!modelNode.data.isEmpty() && hybridSolver.lastUsableModelIndex == -1) {
            hybridSolver.lastUsableModelIndex = hybridSolver.models.size();
            addModel(modelNode, negModelNode);
        }
        return negModelNode;
    }

    public boolean isOntologyConsistentWithPath(){
        if(hybridSolver.checkingMinimalityWithQXP) {
            return isOntologyWithPathConsistent(hybridSolver.pathDuringCheckingMinimality);
        }
        else {
            return isOntologyWithPathConsistent(hybridSolver.path);
        }
    }

    public boolean isOntologyWithPathConsistent(List<OWLAxiom> path){
        if (path != null) {
            if(loader.isMultipleObservationOnInput()){
                for(OWLAxiom axiom : loader.getObservation().getAxiomsInMultipleObservations()){
                    path.remove(AxiomManager.getComplementOfOWLAxiom(loader, axiom));
                }
            } else {
                path.remove(hybridSolver.negObservation);
            }
            System.out.println("PATHX " + path);
            reasonerManager.addAxiomsToOntology(path);
            if (!reasonerManager.isOntologyConsistent()){
                hybridSolver.removeAxiomsFromOntology(path);
                return false;
            }
        }
        return true;
    }

    public void assignTypesToIndividual(OWLDataFactory dfactory, OWLNamedIndividual ind, Set<OWLAxiom> negModelSet, Set<OWLAxiom> modelSet){
        //Set<OWLClassExpression> ontologyTypes = EntitySearcher.getTypes(ind, hybridSolver.ontology).collect(toSet());
        Set<OWLClassExpression> ontologyTypes = EntitySearcher.getTypes(ind, loader.getOntology()).collect(toSet());
        Set<OWLClassExpression> knownTypes = new HashSet<>();
        Set<OWLClassExpression> knownNotTypes = new HashSet<>();
        divideTypesAccordingOntology(ontologyTypes, knownTypes, knownNotTypes);
        System.out.println("KNOWN TYPES");
        System.out.println(knownTypes);
        System.out.println("KNOWN NOT TYPER");
        System.out.println(knownNotTypes);

        Set<OWLClassExpression> newNotTypes = classSet2classExpSet(hybridSolver.ontology.classesInSignature().collect(toSet()));
        newNotTypes.remove(dfactory.getOWLThing());
        //newNotTypes.removeAll(knownNotTypes);

        Set<OWLClassExpression> foundTypes = nodeClassSet2classExpSet(loader.getReasoner().getTypes(ind, false).getNodes());
        System.out.println("FOUND TYPES " + foundTypes);
        newNotTypes.removeAll(foundTypes);
        //foundTypes.removeAll(knownTypes);

        addAxiomsToModelsAccordingTypes(dfactory, negModelSet, modelSet, foundTypes, newNotTypes, ind);
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
            if (!hybridSolver.abducibles.getClasses().contains(classExpression)){
                continue;
            }
            OWLClassExpression negClassExp = classExpression.getComplementNNF();
            OWLAxiom axiom = dfactory.getOWLClassAssertionAxiom(negClassExp, ind);
            negModelSet.add(axiom);
            modelSet.add(dfactory.getOWLClassAssertionAxiom(classExpression, ind));
        }

        for (OWLClassExpression classExpression : newNotTypes) {
            if (!hybridSolver.abducibles.getClasses().contains(classExpression)){
                continue;
            }
            OWLClassExpression negClassExp = classExpression.getComplementNNF();
            OWLAxiom axiom = dfactory.getOWLClassAssertionAxiom(classExpression, ind);
            negModelSet.add(axiom);
            modelSet.add(dfactory.getOWLClassAssertionAxiom(negClassExp, ind));
        }
    }

    public void deletePathFromOntology(){
        if(hybridSolver.checkingMinimalityWithQXP){
            hybridSolver.removeAxiomsFromOntology(hybridSolver.pathDuringCheckingMinimality);
        } else {
            System.out.println("ON " + loader.getOntology());
            hybridSolver.removeAxiomsFromOntology(hybridSolver.path);
            System.out.println("ON " + loader.getOntology());
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

    public ModelNode getNegModelByReasoner() {
        ModelNode modelNode = new ModelNode();
        List<OWLAxiom> model = new LinkedList<>();

        if (hybridSolver.path != null) {
            hybridSolver.path.remove(hybridSolver.negObservation);
            reasonerManager.addAxiomsToOntology(hybridSolver.path);
            if (!reasonerManager.isOntologyConsistent()){
                hybridSolver.removeAxiomsFromOntology(hybridSolver.path);
                modelNode.data = model;
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
        modelNode.data = new LinkedList<>();
        for (OWLAxiom axiom: model){
            if (hybridSolver.abducibles.getIndividuals().containsAll(axiom.individualsInSignature().collect(Collectors.toList())) &&
                    hybridSolver.abducibles.getClasses().containsAll( axiom.classesInSignature().collect(Collectors.toList()))){
                modelNode.data.add(axiom);
            }
        }
        addModel(modelNode, getComplementOfModel(modelNode.data));
        return hybridSolver.negModels.get(hybridSolver.lastUsableModelIndex);
    }

    private ModelNode getComplementOfModel(List<OWLAxiom> model) {
        ModelNode negModelNode = new ModelNode();
        List<OWLAxiom> negModel = new LinkedList<>();
        for (OWLAxiom axiom : model) {
            //nechana stara funkcia getComplementOfOWLAxiom2, kedze s touto castou kodu som nepracovala a neviem, ci to nieco ovplyvni
            OWLAxiom complement = AxiomManager.getComplementOfOWLAxiom2(loader, axiom);
            negModel.add(complement);
        }
        negModelNode.data = negModel;
        return negModelNode;
    }

}
