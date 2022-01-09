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

    public boolean isOntologyWithPathConsistent(List<OWLAxiom> path){
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

    public ModelNode getNegModelByOntology(){  // mrozek
        OWLDataFactory dfactory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
        ModelNode negModelNode = new ModelNode();
        ModelNode modelNode = new ModelNode();
        modelNode.data = new LinkedList<>();

        if(checkConsistencyOfOntologyWithPath() != null){
            return modelNode;
        }

        ArrayList<OWLNamedIndividual> individualArray = new ArrayList<>(hybridSolver.abducibles.getIndividuals());
        Set<OWLAxiom> negModelSet = new HashSet<>();
        Set<OWLAxiom> modelSet = new HashSet<>();

        for (OWLNamedIndividual ind : individualArray) {
            Set<OWLClassExpression> ontologyTypes = EntitySearcher.getTypes(ind, hybridSolver.ontology).collect(toSet());
            Set<OWLClassExpression> knownTypes = new HashSet<>();
            Set<OWLClassExpression> knownNotTypes = new HashSet<>();
            divideTypesAccordingOntology(ontologyTypes, knownTypes, knownNotTypes);

            Set<OWLClassExpression> newNotTypes = classSet2classExpSet(hybridSolver.ontology.classesInSignature().collect(toSet()));
            newNotTypes.remove(dfactory.getOWLThing());
            newNotTypes.removeAll(knownNotTypes);

            Set<OWLClassExpression> foundTypes = nodeClassSet2classExpSet(loader.getReasoner().getTypes(ind, false).getNodes());
            newNotTypes.removeAll(foundTypes);
            foundTypes.removeAll(knownTypes);

            addAxiomsToModelsAccordingTypes(dfactory, negModelSet, modelSet, foundTypes, newNotTypes, ind);
        }

        deletePathFromOntology();

        modelNode.data = new LinkedList<>(modelSet);
        negModelNode.data = new LinkedList<>(negModelSet);
        hybridSolver.lastUsableModelIndex = hybridSolver.models.indexOf(modelNode);
        if (!modelNode.data.isEmpty() && hybridSolver.lastUsableModelIndex == -1) {
            hybridSolver.lastUsableModelIndex = hybridSolver.models.size();
            addModel(modelNode, negModelNode);
        }
        return negModelNode;
    }

    public ModelNode checkConsistencyOfOntologyWithPath(){
        if(hybridSolver.checkingMinimalityWithQXP) {
            if(!isOntologyWithPathConsistent(hybridSolver.pathDuringCheckingMinimality)){
                return new ModelNode();
            }
        }
        else {
            if(!isOntologyWithPathConsistent(hybridSolver.path)){
                return new ModelNode();
            }
        }
        return null;
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

    public void deletePathFromOntology(){
        if(hybridSolver.checkingMinimalityWithQXP){
            hybridSolver.removeAxiomsFromOntology(hybridSolver.pathDuringCheckingMinimality);
        } else {
            hybridSolver.removeAxiomsFromOntology(hybridSolver.path);
        }
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
