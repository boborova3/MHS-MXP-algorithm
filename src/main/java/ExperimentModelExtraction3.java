import algorithms.ISolver;
import algorithms.hybrid.HybridSolver;
import algorithms.hybrid.ModelNode;
import com.google.common.collect.Multimap;
import parser.ArgumentParser;
import common.Configuration;
import common.Printer;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.knowledgeexploration.OWLKnowledgeExplorerReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import reasoner.ILoader;
import reasoner.IReasonerManager;
import reasoner.Loader;
import reasoner.ReasonerManager;
import timer.ThreadTimes;
import uk.ac.manchester.cs.jfact.JFactFactory;

import java.util.*;

import static java.util.stream.Collectors.toSet;

public class ExperimentModelExtraction3 {

    private ILoader loader;
    private IReasonerManager reasonerManager;
    private OWLOntology ontology;
    private Set<OWLClassExpression> notClasses = new HashSet<>();
    private Set<OWLClassExpression> classes = new HashSet<>();

    public ExperimentModelExtraction3(ILoader loader, IReasonerManager reasonerManager){
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

        //new KnowledgeExplorer(loader.getReasoner().get, null);
        OWLReasonerFactory t = new JFactFactory();
        OWLKnowledgeExplorerReasoner c = (OWLKnowledgeExplorerReasoner) t.createReasoner(ontology);
        OWLDataFactory dfactory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
        System.out.println("UVIDIME");
        /*OWLKnowledgeExplorerReasoner.RootNode c = loader.getReasoner().getTopClassNode();
        getDataLabel();
        System.out.println(.getEntities());*/
        ArrayList<OWLNamedIndividual> individualArray = new ArrayList<>(ontology.getIndividualsInSignature());
        Set<OWLAxiom> negModelSet = new HashSet<>();
        Set<OWLAxiom> modelSet = new HashSet<>();
        Set<OWLAxiom> as = loader.getReasoner().getPendingAxiomAdditions();
        System.out.println("PENDING A " + as);
        //Set<OWLClassExpression> FOO = nodeClassSet2classExpSet(loader.getReasoner().);

        for (OWLNamedIndividual ind : individualArray) {
            //System.out.println("INDIVIDUAL " + ind);;
            /*if (!abducibles.getIndividuals().contains(ind)){
                continue;
            }*/

            Set<OWLClassExpression> ontologyTypes = EntitySearcher.getTypes(ind, ontology).collect(toSet());
            Multimap<OWLObjectPropertyExpression, OWLIndividual> ontologyObjectProperty = EntitySearcher.getObjectPropertyValues(ind, ontology);
            Multimap<OWLObjectPropertyExpression, OWLIndividual> ontologyNegativeObjectProperty = EntitySearcher.getNegativeObjectPropertyValues(ind, ontology);

            //System.out.println("ONTOLOGY TYPES " + ontologyTypes);
            Set<OWLClassExpression> knownTypes = new HashSet<>();
            Set<OWLClassExpression> knownNotTypes = new HashSet<>();

            Set<OWLObjectPropertyExpression> knownProperties = new HashSet<>();
            Set<OWLObjectPropertyExpression> knownNotProperties = new HashSet<>();

            //System.out.println();
            System.out.println("FOR CYKLUS CEZ VSETKY ONONTOLOGY TYPES");
            for (OWLClassExpression exp : ontologyTypes) {
                System.out.println("CLASS " + exp);
                //System.out.println("EXPR TYPE " + exp.getClassExpressionType());
                assert (exp.isClassExpressionLiteral());
                if (exp.isOWLClass()) {
                    //System.out.println("IS OWL CLASS");
                    knownTypes.add((exp));
                } else {
                    //System.out.println("IS NOT OWL CLASS " + exp.getComplementNNF());
                    knownNotTypes.add(exp.getComplementNNF());
                }
                OWLKnowledgeExplorerReasoner.RootNode ou = c.getRoot(exp);
            }

            //System.out.println("KNOWN TYPES " + knownTypes);
            //System.out.println("KNOWN NOT TYPES " + knownNotTypes);

            Set<OWLClassExpression> newNotTypes = classSet2classExpSet(ontology.classesInSignature().collect(toSet()));
            //System.out.println("NEW NOT TYPES " + newNotTypes);

            newNotTypes.remove(dfactory.getOWLThing());
            newNotTypes.removeAll(knownNotTypes);
            //System.out.println("NEW NOT TYPES AFTER REMOVING KNOWN NOT TYPES " + newNotTypes);

            //loader.getReasoner().precomputeInferences(InferenceType.DISJOINT_CLASSES);

            System.out.println(c);
            Set<OWLClassExpression> foundTypes = nodeClassSet2classExpSet(loader.getReasoner().getTypes(ind, false).getNodes());
            System.out.println("FOUND TYPES " + foundTypes);

            newNotTypes.removeAll(foundTypes);
            //System.out.println("NEW NOT TYPES AFTER REMOVING FOUND TYPES " + newNotTypes);

            foundTypes.removeAll(knownTypes);
            //System.out.println("FOUND TYPES AFTER REMOVING KNOWN TYPES " + foundTypes);

            /*-------PROPERTIES-------*/
            Set<OWLObjectPropertyExpression> newNotProperties = ontology.objectPropertiesInSignature().collect(toSet());
            newNotProperties.removeAll(knownNotProperties);

           /* Set<OWLObjectPropertyExpression> foundProperties = loader.getReasoner();
                    nodeClassSet2classExpSet(loader.getReasoner().getTypes(ind, false).getNodes());
            newNotProperties.removeAll(foundProperties);
            foundProperties.removeAll(knownProperties);*/


            for (OWLClassExpression classExpression : foundTypes) {
                /*if (!abducibles.getClasses().contains(classExpression)){
                    continue;
                }*/
                OWLClassExpression negClassExp = classExpression.getComplementNNF();
                OWLAxiom axiom = dfactory.getOWLClassAssertionAxiom(negClassExp, ind);
                negModelSet.add(axiom);
                modelSet.add(dfactory.getOWLClassAssertionAxiom(classExpression, ind));
            }
            for (OWLClassExpression classExpression : newNotTypes) {
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
        x[0] = "/home/iveta/Plocha/skola/diplomovka/MHS-MXP-algorithm/in/mhs_mod/pokus9_1_P.in";

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

        ExperimentModelExtraction3 experimentModelExtraction = new ExperimentModelExtraction3(loader, reasonerManager);
        experimentModelExtraction.getNegModelByOntology();
        threadTimes.interrupt();

    }

    private static ISolver createSolver(ThreadTimes threadTimes) {
        long currentTimeMillis = System.currentTimeMillis();
        return new HybridSolver(threadTimes, currentTimeMillis);
    }

}
