package apiImplementation;

import abductionapi.abducibles.SymbolAbducibleContainer;
import abductionapi.exception.SymbolAbducibleException;
import models.Abducibles;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import reasoner.ILoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HybridSymbolAbducibleContainer extends HybridAbducibleContainer
        implements SymbolAbducibleContainer{

    HybridSymbolAbducibleContainer(){}

    Set<OWLClass> classes = new HashSet<>();
    Set<OWLNamedIndividual> individuals = new HashSet<>();
    Set<OWLObjectProperty> roles = new HashSet<>();

    @Override
    public void addSymbol(OWLEntity symbol) throws SymbolAbducibleException {
        if (symbol instanceof OWLClass){
            classes.add((OWLClass)symbol);
        }
        else if (symbol instanceof OWLNamedIndividual){
            individuals.add((OWLNamedIndividual)symbol);
        }
        else if (symbol instanceof OWLObjectProperty){
            roles.add((OWLObjectProperty)symbol);
        }
        else throw new SymbolAbducibleException(symbol.getEntityType().toString());
    }

    @Override
    public void addSymbols(Set<OWLEntity> symbols) throws SymbolAbducibleException {
        symbols.forEach(this::addSymbol);
    }

    @Override
    public void addSymbols(List<OWLEntity> symbols) throws SymbolAbducibleException {
        new HashSet<>(symbols).forEach(this::addSymbol);
    }

    @Override
    public Abducibles exportAbducibles(ILoader loader) {
        return new Abducibles(loader, individuals, classes, roles);
    }
}
