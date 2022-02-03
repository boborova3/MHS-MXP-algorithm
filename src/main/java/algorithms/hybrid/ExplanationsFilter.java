package algorithms.hybrid;

import common.Configuration;
import common.DLSyntax;
import common.Printer;
import fileLogger.FileLogger;
import models.Explanation;
import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.*;
import reasoner.ILoader;
import reasoner.IReasonerManager;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ExplanationsFilter {

    private List<Explanation> minimalExplanations;
    private HybridSolver hybridSolver;
    private ILoader loader;
    private IReasonerManager reasonerManager;
    private ICheckRules checkRules;

    public ExplanationsFilter(ILoader loader, IReasonerManager reasonerManager, HybridSolver hybridSolver){
        this.hybridSolver = hybridSolver;
        this.loader = loader;
        this.reasonerManager = reasonerManager;
        this.checkRules = new CheckRules(loader, reasonerManager);
    }

    public void showExplanations() throws OWLOntologyStorageException, OWLOntologyCreationException {
        List<Explanation> filteredExplanations = new ArrayList<>();
        if(Configuration.MHS_MODE){
            filteredExplanations.addAll(hybridSolver.explanations);
        } else {
            filteredExplanations = getConsistentExplanations();
        }

        hybridSolver.path.clear();
        minimalExplanations = new LinkedList<>();

        StringBuilder result = showExplanationsAccordingToLength(filteredExplanations);
        FileLogger.appendToFile(FileLogger.HYBRID_LOG_FILE__PREFIX, hybridSolver.currentTimeMillis, result.toString());

        log_explanations_times(minimalExplanations);

        if(!Configuration.MHS_MODE){
            StringBuilder resultLevel = showExplanationsAccordingToLevel(new ArrayList<>(minimalExplanations));
            FileLogger.appendToFile(FileLogger.HYBRID_LEVEL_LOG_FILE__PREFIX, hybridSolver.currentTimeMillis, resultLevel.toString());
        }
    }

    private StringBuilder showExplanationsAccordingToLength(List<Explanation> filteredExplanations) throws OWLOntologyCreationException {
        StringBuilder result = new StringBuilder();
        int depth = 1;
        while (filteredExplanations.size() > 0) {
            List<Explanation> currentExplanations = removeExplanationsWithDepth(filteredExplanations, depth);
            if(!Configuration.MHS_MODE){
                if(!Configuration.CHECKING_MINIMALITY_BY_QXP){
                    filterIfNotMinimal(currentExplanations);
                }
                //filterIfNotRelevant(currentExplanations);
            }
            if (currentExplanations.isEmpty()) {
                depth++;
                continue;
            }
            if (!hybridSolver.level_times.containsKey(depth)){
                hybridSolver.level_times.put(depth, find_level_time(currentExplanations));
            }
            minimalExplanations.addAll(currentExplanations);
            String currentExplanationsFormat = StringUtils.join(currentExplanations, ",");
            String line = String.format("%d;%d;%.2f;{%s}\n", depth, currentExplanations.size(), hybridSolver.level_times.get(depth), currentExplanationsFormat);
            System.out.print(line);
            result.append(line);
            depth++;
        }

        String line = String.format("%.2f\n", hybridSolver.threadTimes.getTotalUserTimeInSec());
        System.out.print(line);
        result.append(line);
        return result;
    }

    private StringBuilder showExplanationsAccordingToLevel(List<Explanation> filteredExplanations){
        StringBuilder result = new StringBuilder();
        int level = 0;
        while (filteredExplanations.size() > 0) {
            List<Explanation> currentExplanations = removeExplanationsWithLevel(filteredExplanations, level);
            if (!hybridSolver.level_times.containsKey(level)){
                hybridSolver.level_times.put(level, find_level_time(currentExplanations));
            }
            String currentExplanationsFormat = StringUtils.join(currentExplanations, ",");
            String line = String.format("%d;%d;%.2f;{%s}\n", level, currentExplanations.size(), hybridSolver.level_times.get(level), currentExplanationsFormat);
            result.append(line);
            level++;
        }
        String line = String.format("%.2f\n", hybridSolver.threadTimes.getTotalUserTimeInSec());
        result.append(line);
        return result;
    }

    private void filterIfNotMinimal(List<Explanation> explanations){
        List<Explanation> notMinimalExplanations = new LinkedList<>();
        for (Explanation e: explanations){
            for (Explanation m: minimalExplanations){
                if (e.getOwlAxioms().containsAll(m.getOwlAxioms())){
                    notMinimalExplanations.add(e);
                }
            }
        }
        explanations.removeAll(notMinimalExplanations);
    }

    private void filterIfNotRelevant(List<Explanation> explanations) throws OWLOntologyCreationException {
        List<Explanation> notRelevantExplanations = new LinkedList<>();
        for(Explanation e : explanations){
            if(!checkRules.isRelevant(e)){
                notRelevantExplanations.add(e);
            }
        }
        explanations.removeAll(notRelevantExplanations);
    }

    private List<Explanation> removeExplanationsWithDepth(List<Explanation> filteredExplanations, Integer depth) {
        List<Explanation> currentExplanations = filteredExplanations.stream().filter(explanation -> explanation.getDepth().equals(depth)).collect(Collectors.toList());
        filteredExplanations.removeAll(currentExplanations);
        return currentExplanations;
    }

    private List<Explanation> removeExplanationsWithLevel(List<Explanation> filteredExplanations, Integer level) {
        List<Explanation> currentExplanations = filteredExplanations.stream().filter(explanation -> explanation.getLevel().equals(level)).collect(Collectors.toList());
        filteredExplanations.removeAll(currentExplanations);
        return currentExplanations;
    }

    private double find_level_time(List<Explanation> explanations){
        double time = 0;
        for (Explanation exp: explanations){
            if (exp.getAcquireTime() > time){
                time = exp.getAcquireTime();
            }
        }
        return time;
    }

    private void log_explanations_times(List<Explanation> explanations){
        StringBuilder result = new StringBuilder();
        for (Explanation exp: explanations){
            String line = String.format("%.2f;%s\n", exp.getAcquireTime(), exp);
            result.append(line);
        }
        FileLogger.appendToFile(FileLogger.HYBRID_EXP_TIMES_LOG_FILE__PREFIX, hybridSolver.currentTimeMillis, result.toString());
    }

    private List<Explanation> getConsistentExplanations() throws OWLOntologyStorageException {
        loader.getOntologyManager().removeAxiom(hybridSolver.ontology, loader.getNegObservation().getOwlAxiom());

        /*pridane kvoli tomu, ze vzdy PRVE vysvetlenie pri pouziti hermitu odignorovalo*/
        reasonerManager.resetOntology(hybridSolver.ontology.axioms());

        List<Explanation> filteredExplanations = new ArrayList<>();
        for (Explanation explanation : hybridSolver.explanations) {
            if (isExplanation(explanation)) {
                if (reasonerManager.isOntologyWithLiteralsConsistent(explanation.getOwlAxioms(), hybridSolver.ontology)) {
                    filteredExplanations.add(explanation);
                }
            }
        }

        return filteredExplanations;
    }

    private boolean isExplanation(Explanation explanation) {

        /**ROLY - bude to containsNegation fungovat??**/

        if (explanation.getOwlAxioms().size() == 1) {
            return true;
        }

        for (OWLAxiom axiom1 : explanation.getOwlAxioms()) {
            String name1 = getClassName(axiom1);
            boolean negated1 = containsNegation(name1);
            if (negated1) {
                name1 = name1.substring(1);
            }

            for (OWLAxiom axiom2 : explanation.getOwlAxioms()) {
                if (!axiom1.equals(axiom2) && axiom1.getIndividualsInSignature().equals(axiom2.getIndividualsInSignature())) {
                    String name2 = getClassName(axiom2);

                    boolean negated2 = containsNegation(name2);
                    if (negated2) {
                        name2 = name2.substring(1);
                    }

                    if (name1.equals(name2) && ((!negated1 && negated2) || (negated1 && !negated2))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private String getClassName(OWLAxiom axiom) {
        return Printer.print(axiom).split("\\" + DLSyntax.LEFT_PARENTHESES)[0];
    }

    private boolean containsNegation(String name) {
        return name.contains(DLSyntax.DISPLAY_NEGATION);
    }

    public void showExplanationsWithDepth(Integer depth, boolean timeout, Double time) {
        List<Explanation> currentExplanations = hybridSolver.explanations.stream().filter(explanation -> explanation.getDepth().equals(depth)).collect(Collectors.toList());
        String currentExplanationsFormat = StringUtils.join(currentExplanations, ",");
        String line = String.format("%d;%d;%.2f%s;{%s}\n", depth, currentExplanations.size(), time, timeout ? "-TIMEOUT" : "", currentExplanationsFormat);
        System.out.print(line);
        FileLogger.appendToFile(FileLogger.HYBRID_PARTIAL_EXPLANATIONS_LOG_FILE__PREFIX, hybridSolver.currentTimeMillis, line);
    }

    public void showExplanationsWithLevel(Integer level, boolean timeout, Double time){
        List<Explanation> currentExplanations = hybridSolver.explanations.stream().filter(explanation -> explanation.getLevel().equals(level)).collect(Collectors.toList());
        String currentExplanationsFormat = StringUtils.join(currentExplanations, ",");
        String line = String.format("%d;%d;%.2f%s;{%s}\n", level, currentExplanations.size(), time, timeout ? "-TIMEOUT" : "", currentExplanationsFormat);
        //System.out.print(line);
        FileLogger.appendToFile(FileLogger.HYBRID_PARTIAL_EXPLANATIONS_ACCORDING_TO_LEVELS_LOG_FILE__PREFIX, hybridSolver.currentTimeMillis, line);
    }

}
