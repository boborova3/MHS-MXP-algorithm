package algorithms.hybrid;

import common.Configuration;
import models.AxiomPair;
import models.Explanation;
import models.Literals;
import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.*;

public class SetDivider {

    HybridSolver hybridSolver;
    private Map<AxiomPair, Integer> tableOfAxiomPairOccurance;
    private List<Integer> numberOfAxiomPairOccurance;
    private double median = 0;
    public Set<Integer> notUsedExplanations;
    private int lastUsedIndex;

    public SetDivider(HybridSolver hybridSolver){
        this.hybridSolver = hybridSolver;
        tableOfAxiomPairOccurance = new HashMap<>();
        numberOfAxiomPairOccurance = new ArrayList<>();
        notUsedExplanations = new HashSet<>();
        lastUsedIndex = -1;
    }

    public void setIndexesOfExplanations(int sizeOfCollection){
        for(int i = 0; i < sizeOfCollection; i++){
            notUsedExplanations.add(i);
        }
    }

    public void addIndexToIndexesOfExplanations(int index){
        if(index != -1){
            notUsedExplanations.add(index);
        }
    }

    public List<Literals> divideIntoSets(Literals literals) {
        if(Configuration.CACHED_CONFLICTS_LONGEST_CONFLICT && hybridSolver.explanations.size() > 0 && lastUsedIndex != -1){
            return divideIntoSetsAccordingTheLongestConflict(literals);
        } else if (Configuration.CACHED_CONFLICTS_TABLE_OF_OCCURRENCE && hybridSolver.explanations.size() > 0){
            return divideIntoSetsAccordingTableOfLiteralsPairOccurrence(literals);
        }
        return divideIntoSetsWithoutCondition(literals);
    }

    private List<Literals> divideIntoSetsWithoutCondition(Literals literals){
        List<Literals> dividedLiterals = new ArrayList<>();

        dividedLiterals.add(new Literals());
        dividedLiterals.add(new Literals());

        int count = 0;

        for (OWLAxiom owlAxiom : literals.getOwlAxioms()) {
            dividedLiterals.get(count % 2).getOwlAxioms().add(owlAxiom);
            count++;
        }
        return dividedLiterals;
    }

    private List<Literals> divideIntoSetsAccordingTheLongestConflict(Literals literals){
        Explanation theLongestExplanation = hybridSolver.explanations.get(lastUsedIndex);
        Set<OWLAxiom> axiomsFromExplanation = new HashSet<>(theLongestExplanation.getOwlAxioms());

        List<Literals> dividedLiterals = new ArrayList<>();
        dividedLiterals.add(new Literals());
        dividedLiterals.add(new Literals());

        int count = 0;
        for(OWLAxiom owlAxiom : axiomsFromExplanation){
            if(literals.getOwlAxioms().contains(owlAxiom)){
                dividedLiterals.get(count % 2).getOwlAxioms().add(owlAxiom);
                count++;
            }
        }

        for(OWLAxiom owlAxiom : literals.getOwlAxioms()) {
            if(!axiomsFromExplanation.contains(owlAxiom)){
                dividedLiterals.get(count % 2).getOwlAxioms().add(owlAxiom);
                count++;
            }
        }
        return dividedLiterals;
    }

    public int getIndexOfTheLongestAndNotUsedConflict(){
        int indexOfLongestExp = -1;
        int length = 0;

        for(Integer i : notUsedExplanations){
            if(hybridSolver.explanations.get(i).getDepth() > length){
                indexOfLongestExp = i;
            }
        }

        lastUsedIndex = indexOfLongestExp;
        if(indexOfLongestExp == -1){
            return -1;
        }
        notUsedExplanations.remove(indexOfLongestExp);
        return indexOfLongestExp;
    }

    private List<Literals> divideIntoSetsAccordingTableOfLiteralsPairOccurrence(Literals literals){
        Set<OWLAxiom> axiomsFromLiterals = new HashSet<>(literals.getOwlAxioms());
        List<Literals> dividedLiterals = new ArrayList<>();
        dividedLiterals.add(new Literals());
        dividedLiterals.add(new Literals());

        for(AxiomPair key : tableOfAxiomPairOccurance.keySet()){
            if(axiomsFromLiterals.contains(key.first) && axiomsFromLiterals.contains(key.second)){
                if(tableOfAxiomPairOccurance.get(key) > median){
                    dividedLiterals.get(0).getOwlAxioms().add(key.first);
                    dividedLiterals.get(1).getOwlAxioms().add(key.second);
                    axiomsFromLiterals.remove(key.first);
                    axiomsFromLiterals.remove(key.second);
                }
            }
        }

        int count = 0;
        for (OWLAxiom owlAxiom : axiomsFromLiterals) {
            dividedLiterals.get(count % 2).getOwlAxioms().add(owlAxiom);
            count++;
        }
        return dividedLiterals;
    }

    public void addPairsOfLiteralsToTable(Explanation explanation){
        LinkedList<OWLAxiom> expAxioms;
        if (explanation.getOwlAxioms() instanceof List)
            expAxioms = (LinkedList<OWLAxiom>) explanation.getOwlAxioms();
        else
            expAxioms = new LinkedList<>(explanation.getOwlAxioms());

        for(int i = 0; i < expAxioms.size(); i++){
            for(int j = i + 1; j < expAxioms.size(); j++){
                AxiomPair axiomPair = new AxiomPair(expAxioms.get(i), expAxioms.get(j));
                Integer value = tableOfAxiomPairOccurance.getOrDefault(axiomPair, 0) + 1;
                tableOfAxiomPairOccurance.put(axiomPair, value);
                addToListOfAxiomPairOccurance(value);
            }
        }
        setMedianFromListOfAxiomPairOccurance();
    }

    public void addToListOfAxiomPairOccurance(Integer value){
        int index = 0;
        for(int i = 0; i < numberOfAxiomPairOccurance.size(); i++){
            if(numberOfAxiomPairOccurance.get(i) > value){
                break;
            }
            index++;
        }
        numberOfAxiomPairOccurance.add(index, value);
    }

    private void setMedianFromListOfAxiomPairOccurance(){
        if(numberOfAxiomPairOccurance.size() == 0){
            return;
        }
        if(numberOfAxiomPairOccurance.size() % 2 == 0){
            int index = numberOfAxiomPairOccurance.size()/2;
            median = (numberOfAxiomPairOccurance.get(index - 1) + numberOfAxiomPairOccurance.get(index)) / 2.0;
        } else {
            int index = (numberOfAxiomPairOccurance.size() - 1)/2;
            median = numberOfAxiomPairOccurance.get(index);
        }
    }

}
