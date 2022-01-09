import algorithms.ISolver;
import algorithms.hybrid.HybridSolver;
import algorithms.hybrid.ModelNode;
import common.ArgumentParser;
import common.Configuration;
import common.Printer;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.search.EntitySearcher;
import reasoner.*;
import timer.ThreadTimes;
import uk.ac.manchester.cs.jfact.kernel.Axiom;

import javax.lang.model.type.UnionType;
import java.beans.Expression;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

public class ExperimentModelExtraction {

    private ILoader loader;
    private IReasonerManager reasonerManager;
    private OWLOntology ontology;
    private Set<OWLClassExpression> notClasses = new HashSet<>();
    private Set<OWLClassExpression> classes = new HashSet<>();

    public ExperimentModelExtraction(ILoader loader, IReasonerManager reasonerManager){
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

    private List<OWLAxiom> pokus(Set<OWLClassExpression> complexConcepts, Set<OWLClassExpression> alreadyIn, Set<OWLClassExpression> alreadyNotIn, OWLNamedIndividual individual){
        List<OWLClassExpression> notKnownClasses = new ArrayList<>();
        Set<OWLClassExpression> allClasses = new HashSet<>();
        List<OWLAxiom> alreadyInAxioms = new ArrayList<>();
        List<OWLAxiom> alreadyNotInAxioms = new ArrayList<>();

        //System.out.println("All classes");
        for(OWLClassExpression classExpression : complexConcepts){
            allClasses.addAll(classExpression.getClassesInSignature());
            //System.out.println(classExpression);
        }

        /*for(OWLClassExpression e : allClasses){
            System.out.println(e);
        }*/

        for(OWLClassExpression expression : allClasses){
            if(alreadyIn.contains(expression)){
                //triedy.add(expression);
                alreadyInAxioms.add(loader.getDataFactory().getOWLClassAssertionAxiom(expression, individual));
            } else if(alreadyNotIn.contains(expression)){
                alreadyNotInAxioms.add(loader.getDataFactory().getOWLClassAssertionAxiom(expression.getComplementNNF(), individual));
            } else {
                notKnownClasses.add(expression);
            }
        }

        /*System.out.println("Not known classes");
        for(OWLClassExpression e : notKnownClasses){
            System.out.println(e);
        }

        System.out.println("Not classes");
        for(OWLClassExpression e : alreadyNotIn){
            System.out.println(e);
        }*/

        reasonerManager.addAxiomsToOntology(alreadyInAxioms);
        reasonerManager.addAxiomsToOntology(alreadyNotInAxioms);

        int n = notKnownClasses.size();
        for(int i = (2^n) - 1; i >= 0 ; i--){
            /*List<OWLAxiom> temp = new ArrayList<>();
            triedy2 = new HashSet<>();*/

            List<OWLAxiom> alreadyKnownAxioms = new ArrayList<>();
            classes = new HashSet<>();
            notClasses = new HashSet<>();

            String combination = String.format("%0" + n + "d" , Integer.parseInt(Integer.toBinaryString(i)));
            for(int j = 0; j < combination.length(); j++){
                if(combination.charAt(j) == '1'){
                    alreadyKnownAxioms.add(loader.getDataFactory().getOWLClassAssertionAxiom(notKnownClasses.get(j), individual));
                    classes.add(notKnownClasses.get(j));
                    //triedy2.add(notKnownClasses.get(j));
                } else {
                    alreadyKnownAxioms.add(loader.getDataFactory().getOWLClassAssertionAxiom(notKnownClasses.get(j).getComplementNNF(), individual));
                    notClasses.add(notKnownClasses.get(j));
                    //triedy2.add(notKnownClasses.get(j).getComplementNNF());
                }
            }
            reasonerManager.addAxiomsToOntology(alreadyKnownAxioms);
            System.out.println("Added alreadyKnownAxioms");
            for(OWLAxiom a : alreadyKnownAxioms){
                System.out.println(a);
            }
            System.out.println("Added alreadyInAxioms");
            for(OWLAxiom a : alreadyInAxioms){
                System.out.println(a);
            }
            System.out.println("Added alreadyNotInAxioms");
            for(OWLAxiom a : alreadyNotInAxioms){
                System.out.println(a);
            }
            //System.out.println(loader.getReasoner().getRootOntology());
            if (reasonerManager.isOntologyConsistent()){
                removeAxiomsFromOntology(alreadyKnownAxioms);
                removeAxiomsFromOntology(alreadyInAxioms);
                removeAxiomsFromOntology(alreadyNotInAxioms);
                return alreadyKnownAxioms;
            }
            removeAxiomsFromOntology(alreadyKnownAxioms);
        }

        /*if(complexConcept.getClassExpressionType() == ClassExpressionType.OWL_CLASS){
            triedy.add(complexConcept);
            return;
        }
        if(complexConcept.getClassExpressionType() == ClassExpressionType.OBJECT_COMPLEMENT_OF){
            if(complexConcept.getNNF().getClassExpressionType() == ClassExpressionType.OWL_CLASS){
                triedy.add(complexConcept);
                return;
            }
        }
        if(complexConcept.getClassExpressionType() == ClassExpressionType.OBJECT_UNION_OF){
            List<OWLClassExpression> temp = complexConcept.disjunctSet().collect(Collectors.toList());
            for(OWLClassExpression e : temp){
                pokus(e);
            }
        }*/
        return new ArrayList<>();
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

            Set<OWLClassExpression> ontologyTypes = EntitySearcher.getTypes(ind, ontology).collect(toSet());
            System.out.println("ONTOLOGY TYPES " + ontologyTypes);
            Set<OWLClassExpression> knownTypes = new HashSet<>();
            Set<OWLClassExpression> knownNotTypes = new HashSet<>();

            Set<OWLClassExpression> foundTypes = nodeClassSet2classExpSet(loader.getReasoner().getTypes(ind, false).getNodes());

            System.out.println();
            System.out.println("FOR CYKLUS CEZ VSETKY ONONTOLOGY TYPES");

            Set<OWLClassExpression> unionsIn = new HashSet<>();
            Set<OWLClassExpression> alreadyIn = new HashSet<>();

            alreadyIn.addAll(foundTypes);

            for (OWLClassExpression exp : ontologyTypes) {
                System.out.println("CLASS " + exp);
                assert (exp.isClassExpressionLiteral());
                //System.out.println("EXPR TYPE " + exp.getClassExpressionType());
                /** kontroly **/
               // System.out.println(exp.getClassExpressionType() == ClassExpressionType.OBJECT_UNION_OF);
                //System.out.println(exp.getClassExpressionType() == ClassExpressionType.OWL_CLASS);
                
                if (exp.getClassExpressionType() == ClassExpressionType.OWL_CLASS) {
                    System.out.println("IS OWL CLASS");
                    knownTypes.add((exp));
                } else if(exp.getClassExpressionType() == ClassExpressionType.OBJECT_COMPLEMENT_OF){
                    System.out.println("IS NOT OWL CLASS " + exp.getComplementNNF());
                    knownNotTypes.add(exp.getComplementNNF());
                } else if(exp.getClassExpressionType() == ClassExpressionType.OBJECT_UNION_OF){
                    System.out.println("IS OWL UNION");
                    unionsIn.add(exp);
                    //knownTypes.addAll(triedy);
                }
            }
            alreadyIn.addAll(knownTypes);

            List<OWLAxiom> allAlreadyKnownClasses = pokus(unionsIn, alreadyIn, knownNotTypes, ind);

            /*
            List<OWLAxiom> l = new ArrayList<>();
            for(OWLClassExpression e : triedy){
                l.add(loader.getDataFactory().getOWLClassAssertionAxiom(e, ind));
            }
            for(OWLClassExpression e : triedy2){
                l.add(loader.getDataFactory().getOWLClassAssertionAxiom(e, ind));
            }
            reasonerManager.addAxiomsToOntology(l);*/

            reasonerManager.addAxiomsToOntology(allAlreadyKnownClasses);

            foundTypes = nodeClassSet2classExpSet(loader.getReasoner().getTypes(ind, false).getNodes());

            knownNotTypes.addAll(notClasses);
            //foundTypes.addAll(classes);

            removeAxiomsFromOntology(allAlreadyKnownClasses);

            System.out.println("KNOWN TYPES " + knownTypes);
            System.out.println("KNOWN NOT TYPES " + knownNotTypes);

            System.out.println();

            Set<OWLClassExpression> newNotTypes = classSet2classExpSet(ontology.classesInSignature().collect(toSet()));
            System.out.println("NEW NOT TYPES " + newNotTypes);

            newNotTypes.remove(dfactory.getOWLThing());
            //newNotTypes.removeAll(knownNotTypes);
            //System.out.println("NEW NOT TYPES AFTER REMOVING KNOWN NOT TYPES " + newNotTypes);

            /*foundTypes.addAll(triedy);
            triedy = new HashSet<>();*/

            System.out.println("FOUND TYPES " + foundTypes);
            newNotTypes.removeAll(foundTypes);
            System.out.println("NEW NOT TYPES AFTER REMOVING FOUND TYPES " + newNotTypes);
            //foundTypes.removeAll(knownTypes);
            //System.out.println("FOUND TYPES AFTER REMOVING KNOWN TYPES " + foundTypes);

            // vo found types ostanu tie, do ktorych be reasoner zaradil individual (odstrania sa z nich tie, o ktorych vieme uz z knownTypes, ze tam patria naisto)
            for (OWLClassExpression classExpression : foundTypes) {
                /** "VSETKO" JE V ABDUCIBLES **/
                /*if (!abducibles.getClasses().contains(classExpression)){
                    continue;
                }*/
                OWLClassExpression negClassExp = classExpression.getComplementNNF();
                OWLAxiom axiom = dfactory.getOWLClassAssertionAxiom(negClassExp, ind);
                negModelSet.add(axiom);
                modelSet.add(dfactory.getOWLClassAssertionAxiom(classExpression, ind));
            }
            // v newNotTypes nechame vsetky triedy, okrem foundTypes(este predtym ako z nich boli odstranene knownTypes) a knownNotTypes(teda tried, ktore naisto vieme, ze individual tam nepatri)
            //to vytvori mnozinu v ktorej su vsetky triedy, do ktorych individual nechceme zaradiť (teda chceme ho zaradit do ich komplementu)
            for (OWLClassExpression classExpression : newNotTypes) {
                /** "VSETKO" JE V ABDUCIBLES **/
                /*if (!abducibles.getClasses().contains(classExpression)){
                    continue;
                }*/
                OWLClassExpression negClassExp = classExpression.getComplementNNF();
                OWLAxiom axiom = dfactory.getOWLClassAssertionAxiom(classExpression, ind);
                negModelSet.add(axiom);
                modelSet.add(dfactory.getOWLClassAssertionAxiom(negClassExp, ind));
            }
        }

        removeAxiomsFromOntology(path);
        modelNode.data = new LinkedList<>(modelSet);
        negModelNode.data = new LinkedList<>(negModelSet);

        System.out.println("MODEL: ");
        for(OWLAxiom a : modelNode.data){
            System.out.print(Printer.print(a) + " ");
        }
        System.out.println();
        //System.out.println(modelNode.data);
        //System.out.println(negModelNode.data);
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
        x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/testExtractingModels/pokus9_2.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/testExtractingModels/pokus6.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/testExtractingModels/pokus7.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/testExtractingModels/pokus8.in";
        // A or B -> vrati model -A(a), -B(A)
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/testExtractingModels/pokus0.in";
        //individual nezaradeny
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/testExtractingModels/pokus5.in";

        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/nove_in/lubm-0_2_0_MXP_notNegation.in";
        //x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/input_fam.txt";

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

        ExperimentModelExtraction experimentModelExtraction = new ExperimentModelExtraction(loader, reasonerManager);
        experimentModelExtraction.getNegModelByOntology();
        threadTimes.interrupt();

    }

    private static ISolver createSolver(ThreadTimes threadTimes) {
        long currentTimeMillis = System.currentTimeMillis();
        return new HybridSolver(threadTimes, currentTimeMillis);
    }

}
