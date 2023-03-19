package apiImplementation;

import abductionapi.abducibles.AxiomAbducibleContainer;
import abductionapi.abducibles.SymbolAbducibleContainer;
import abductionapi.factories.AbductionFactory;
import abductionapi.manager.AbductionManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.Set;

public class HybridAbductionFactory implements AbductionFactory<
        HybridAbductionManager,
        HybridSymbolAbducibleContainer,
        HybridAxiomAbducibleContainer
        > {

    private static final HybridAbductionFactory instance = new HybridAbductionFactory();

    private HybridAbductionFactory(){}

    public static HybridAbductionFactory getFactory(){
        return instance;
    }

    @Override
    public HybridAbductionManager getSolverSpecificAbductionManager() {
        return new HybridAbductionManager();
    }

    @Override
    public AbductionManager getAbductionManager() {
        return new HybridAbductionManager();
    }

    @Override
    public AbductionManager getAbductionManagerWithInput(OWLOntology owlOntology, OWLAxiom owlAxiom) {
        AbductionManager manager = new HybridAbductionManager();
        manager.setKnowledgeBase(owlOntology);
        manager.setObservation(owlAxiom);
        return manager;
    }

    @Override
    public AbductionManager getAbductionManagerWithSymbolAbducibles(Set<OWLEntity> symbols) {
        AbductionManager manager = new HybridAbductionManager();
        SymbolAbducibleContainer container = new HybridSymbolAbducibleContainer();
        container.addSymbols(symbols);
        manager.setAbducibles(container);
        return manager;
    }

    @Override
    public AbductionManager getAbductionManagerWithAxiomAbducibles(Set<OWLAxiom> axioms) {
        AbductionManager manager = new HybridAbductionManager();
        AxiomAbducibleContainer container = new HybridAxiomAbducibleContainer();
        container.addAxioms(axioms);
        manager.setAbducibles(container);
        return manager;
    }

    @Override
    public AxiomAbducibleContainer getAxiomAbducibleContainer() {
        return new HybridAxiomAbducibleContainer();
    }

    @Override
    public SymbolAbducibleContainer getSymbolAbducibleContainer() {
        return new HybridSymbolAbducibleContainer();
    }

    @Override
    public HybridAxiomAbducibleContainer getSolverSpecificAxiomAbducibleContainer() {
        return new HybridAxiomAbducibleContainer();
    }

    @Override
    public HybridSymbolAbducibleContainer getSolverSpecificSymbolAbducibleContainer() {
        return new HybridSymbolAbducibleContainer();
    }
}
