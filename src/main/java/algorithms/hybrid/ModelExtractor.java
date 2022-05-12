package algorithms.hybrid;
import com.google.inject.internal.asm.$ClassReader;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.knowledgeexploration.OWLKnowledgeExplorerReasoner;
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
        Set<OWLAxiom> negModelSet = new HashSet<>();
        Set<OWLAxiom> modelSet = new HashSet<>();

        if(!isOntologyConsistentWithPath()){
            modelNode.data = new HashSet<>();
            return modelNode;
        }

        ArrayList<OWLNamedIndividual> individualArray;
        if(loader.isAxiomBasedAbduciblesOnInput()){
            individualArray = new ArrayList<>(loader.getOntology().getIndividualsInSignature());
        } else {
            individualArray = new ArrayList<>(hybridSolver.abducibles.getIndividuals());
        }

        for (OWLNamedIndividual ind : individualArray) {
            assignTypesToIndividual(dfactory, ind, negModelSet, modelSet);
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
        //System.out.println(ontologyTypes);
        //Set<OWLClassExpression> ontologyTypes = EntitySearcher.getTypes(ind, loader.getOntology()).collect(toSet());
        Set<OWLClassExpression> knownTypes = new HashSet<>();
        Set<OWLClassExpression> knownNotTypes = new HashSet<>();
        divideTypesAccordingOntology(ontologyTypes, knownTypes, knownNotTypes);

        Set<OWLClassExpression> newNotTypes = classSet2classExpSet(hybridSolver.ontology.classesInSignature().collect(toSet()));
        //System.out.println("NEW NOT TYPES " + newNotTypes);
        newNotTypes.remove(dfactory.getOWLThing());
        newNotTypes.removeAll(knownNotTypes);

        Set<OWLClassExpression> foundTypes = nodeClassSet2classExpSet(loader.getReasoner().getTypes(ind, false).getNodes());
        //System.out.println("FOUND TYPES " + foundTypes);
        newNotTypes.removeAll(foundTypes);
        foundTypes.removeAll(knownTypes);

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
        //System.out.println("KNOWN TYPES "  + knownTypes);
        //System.out.println("KNOWN NOT TYPES "  + knownTypes);
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

    public ModelNode getNegModelByReasoner() {
        ModelNode modelNode = new ModelNode();
        Set<OWLAxiom> model = new HashSet<>();

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
        modelNode.data = new HashSet<>();
        for (OWLAxiom axiom: model){
            if (hybridSolver.abducibles.getIndividuals().containsAll(axiom.individualsInSignature().collect(Collectors.toList())) &&
                    hybridSolver.abducibles.getClasses().containsAll( axiom.classesInSignature().collect(Collectors.toList()))){
                modelNode.data.add(axiom);
            }
        }
        addModel(modelNode, getComplementOfModel(modelNode.data));
        return hybridSolver.negModels.get(hybridSolver.lastUsableModelIndex);
    }

    private ModelNode getComplementOfModel(Set<OWLAxiom> model) {
        ModelNode negModelNode = new ModelNode();
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
