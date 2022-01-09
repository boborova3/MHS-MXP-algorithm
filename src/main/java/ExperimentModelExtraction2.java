import algorithms.ISolver;
import algorithms.hybrid.HybridSolver;
import algorithms.hybrid.ModelNode;
import common.ArgumentParser;
import common.Configuration;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import reasoner.*;
import timer.ThreadTimes;

import javax.lang.model.type.UnionType;
import java.util.*;

import static java.util.stream.Collectors.toSet;

public class ExperimentModelExtraction2 {

    private ILoader loader;
    private IReasonerManager reasonerManager;
    private OWLOntology ontology;

    public ExperimentModelExtraction2(ILoader loader, IReasonerManager reasonerManager){
        this.loader = loader;
        this.reasonerManager = reasonerManager;
        this.ontology = this.loader.getOriginalOntology();

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

    private void removeAxiomsFromOntology(List<OWLAxiom> axioms){
        reasonerManager.resetOntology(loader.getOriginalOntology().axioms());
    }

    public ModelNode getNegModelByOntology(){  // mrozek
        ModelNode negModelNode = new ModelNode();
        ModelNode modelNode = new ModelNode();
        modelNode.data = new LinkedList<>();

        List<OWLAxiom> path = null;

        if (path != null) {
            /**Je potrebne robit tieto remove veci, ak sa to tam realne uz nema ako dostat???**/
            /*if(loader.isMultipleObservationOnInput()){
                for(OWLAxiom axiom : loader.getMultipleObservations()){
                    //axiom
                    path.remove(AxiomManager.getComplementOfOWLAxiom(loader, axiom));
                }
            } else {
                path.remove(negObservation);
            }*/
            //path.remove(negObservation);
            reasonerManager.addAxiomsToOntology(path);
            if (!reasonerManager.isOntologyConsistent()){
                removeAxiomsFromOntology(path);
                return modelNode;
            }
        }

        reasonerManager.addAxiomToOntology(loader.getNegObservation().getOwlAxiom());

        OWLDataFactory dfactory = OWLManager.createOWLOntologyManager().getOWLDataFactory();

        ArrayList<OWLNamedIndividual> individualArray = new ArrayList<>(ontology.getIndividualsInSignature());
        Set<OWLAxiom> negModelSet = new HashSet<>();
        Set<OWLAxiom> modelSet = new HashSet<>();

        for (OWLNamedIndividual ind : individualArray) {
            System.out.println("INDIVIDUAL " + ind);;
            /** "VSETKO" JE V ABDUCIBLES **/
            /*
            if (!abducibles.getIndividuals().contains(ind)){
                continue;
            }*/

            /*Set<OWLClassExpression> ontologyTypes = EntitySearcher.getTypes(ind, ontology).collect(toSet());
            System.out.println("ONTOLOGY TYPES " + ontologyTypes);
            Set<OWLClassExpression> knownTypes = new HashSet<>();
            Set<OWLClassExpression> knownNotTypes = new HashSet<>();

            System.out.println();
            System.out.println("FOR CYKLUS CEZ VSETKY ONONTOLOGY TYPES");
            for (OWLClassExpression exp : ontologyTypes) {
                System.out.println("CLASS " + exp);
                assert (exp.isClassExpressionLiteral());
                System.out.println("EXPR TYPE " + exp.getClassExpressionType());
                /** kontroly **/
           /*     System.out.println(exp.getClassExpressionType() == ClassExpressionType.OBJECT_UNION_OF);
                System.out.println(exp.getClassExpressionType() == ClassExpressionType.OWL_CLASS);

                if (exp.isOWLClass()) {
                    System.out.println("IS OWL CLASS");
                    knownTypes.add((exp));
                } else {
                    System.out.println("IS NOT OWL CLASS " + exp.getComplementNNF());
                    knownNotTypes.add(exp.getComplementNNF());
                }
            }

            System.out.println("KNOWN TYPES " + knownTypes);
            System.out.println("KNOWN NOT TYPES " + knownNotTypes);

            System.out.println();*/

            Set<OWLClassExpression> newNotTypes = classSet2classExpSet(ontology.classesInSignature().collect(toSet()));
            System.out.println("NEW NOT TYPES " + newNotTypes);

            newNotTypes.remove(dfactory.getOWLThing());
            //newNotTypes.removeAll(knownNotTypes);
            //System.out.println("NEW NOT TYPES AFTER REMOVING KNOWN NOT TYPES " + newNotTypes);

            Set<OWLClassExpression> foundTypes = nodeClassSet2classExpSet(loader.getReasoner().getTypes(ind, false).getNodes());

            System.out.println("FOUND TYPES " + foundTypes);
            newNotTypes.removeAll(foundTypes);
            //System.out.println("NEW NOT TYPES AFTER REMOVING FOUND TYPES " + newNotTypes);
            //foundTypes.removeAll(knownTypes);
            System.out.println("FOUND TYPES AFTER REMOVING KNOWN TYPES " + foundTypes);

            // vo found types ostanu tie, do ktorych be reasoner zaradil individual (odstrania sa z nich tie, o ktorych vieme uz z knownTypes, ze tam patria naisto)
            for (OWLClassExpression classExpression : foundTypes) {
                /** "VSETKO" JE V ABDUCIBLES **/
                /*if (!abducibles.getClasses().contains(classExpression)){
                    continue;
                }*/
                System.out.println("DO TEJTO BY MAL PATRIT " + classExpression);
                OWLClassExpression negClassExp = classExpression.getComplementNNF();
                OWLAxiom axiom = dfactory.getOWLClassAssertionAxiom(negClassExp, ind);
                System.out.println("DO NEG MODELA PRIDAVAME " + axiom);
                negModelSet.add(axiom);
                System.out.println("DO NORMALNEHO MODELA PRIDAVAME " + dfactory.getOWLClassAssertionAxiom(classExpression, ind));
                modelSet.add(dfactory.getOWLClassAssertionAxiom(classExpression, ind));
            }
            // v newNotTypes nechame vsetky triedy, okrem foundTypes(este predtym ako z nich boli odstranene knownTypes) a knownNotTypes(teda tried, ktore naisto vieme, ze individual tam nepatri)
            //to vytvori mnozinu v ktorej su vsetky triedy, do ktorych individual nechceme zaradiť (teda chceme ho zaradit do ich komplementu)
            for (OWLClassExpression classExpression : newNotTypes) {
                /** "VSETKO" JE V ABDUCIBLES **/
                /*if (!abducibles.getClasses().contains(classExpression)){
                    continue;
                }*/
                System.out.println("DO TEJTO BY NEMAL PATRIT " + classExpression);
                OWLClassExpression negClassExp = classExpression.getComplementNNF();
                OWLAxiom axiom = dfactory.getOWLClassAssertionAxiom(classExpression, ind);
                System.out.println("DO NEG MODELA PRIDAVAME  " + axiom);
                negModelSet.add(axiom);
                System.out.println(dfactory.getOWLClassAssertionAxiom(negClassExp, ind));
                modelSet.add(dfactory.getOWLClassAssertionAxiom(negClassExp, ind));
            }
        }

        removeAxiomsFromOntology(path);
        modelNode.data = new LinkedList<>(modelSet);
        negModelNode.data = new LinkedList<>(negModelSet);

        System.out.println("MODEL: ");
        System.out.println(modelNode.data);
        System.out.println("NEG MODEL: ");
        System.out.println(negModelNode.data);
        /*lastUsableModelIndex = models.indexOf(modelNode);
        if (!modelNode.data.isEmpty() && lastUsableModelIndex == -1) {
            lastUsableModelIndex = models.size();
            addModel(modelNode, negModelNode);
        }*/
        return negModelNode;
    }

    public static void main(String[] args) throws Exception {

        String[] x = new String[1];
        // not A or not B -> vrati model -A(a), -B(A)
        x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/testExtractingModels/pokus.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/testExtractingModels/pokus6.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/testExtractingModels/pokus7.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/testExtractingModels/pokus8.in";
        // A or B -> vrati model -A(a), -B(A)
        x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/testExtractingModels/pokus0.in";
        //individual nezaradeny
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/testExtractingModels/pokus5.in";

        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/nove_in/lubm-0_2_0_MXP_notNegation.in";
        x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/testExtractingModels/posledny.in";


        Logger.getRootLogger().setLevel(Level.OFF);
        BasicConfigurator.configure();

        ArgumentParser argumentParser = new ArgumentParser();
        //argumentParser.parse(args);
        argumentParser.parse(x);

        ILoader loader = new Loader();
        loader.initialize(Configuration.REASONER);

        ThreadTimes threadTimes = new ThreadTimes(100);
        threadTimes.start();

        IReasonerManager reasonerManager = new ReasonerManager(loader);
        //ISolver solver = createSolver(threadTimes);

        ExperimentModelExtraction2 experimentModelExtraction2 = new ExperimentModelExtraction2(loader, reasonerManager);
        experimentModelExtraction2.getNegModelByOntology();
        threadTimes.interrupt();

    }

    private static ISolver createSolver(ThreadTimes threadTimes) {
        long currentTimeMillis = System.currentTimeMillis();
        return new HybridSolver(threadTimes, currentTimeMillis);
    }

}
