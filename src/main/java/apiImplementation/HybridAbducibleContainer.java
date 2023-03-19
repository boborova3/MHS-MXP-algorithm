package apiImplementation;

import abductionapi.abducibles.*;
import abductionapi.exception.NotSupportedException;
import common.Configuration;
import models.Abducibles;
import reasoner.ILoader;

/**
 * The type Hybrid abducible container.
 */
public abstract class HybridAbducibleContainer
        implements AbducibleContainer, RoleAbducibleConfigurator, ComplexConceptAbducibleConfigurator {


    /**
     * Create an instance of the models.Abducibles class containing abducibles from this container
     * @param loader instance of reasoner.ILooader needed to construct the Abducibles instance
     */
    public abstract Abducibles exportAbducibles(ILoader loader);

    @Override
    public void allowConceptComplements() throws NotSupportedException {
        Configuration.NEGATION_ALLOWED = true;
    }

    @Override
    public void allowConceptComplements(Boolean allowConceptComplements) throws NotSupportedException {
        Configuration.NEGATION_ALLOWED = allowConceptComplements;
    }

    @Override
    public boolean areConceptComplementsAllowed() throws NotSupportedException {
        return Configuration.NEGATION_ALLOWED;
    }

    @Override
    public void allowRoleAssertions() throws NotSupportedException {
        Configuration.ROLES_IN_EXPLANATIONS_ALLOWED = true;
    }

    @Override
    public void allowRoleAssertions(Boolean allowRoleAssertions) throws NotSupportedException {
        Configuration.ROLES_IN_EXPLANATIONS_ALLOWED = allowRoleAssertions;
    }

    @Override
    public boolean areRoleAssertionsAllowed() throws NotSupportedException {
        return Configuration.ROLES_IN_EXPLANATIONS_ALLOWED;
    }

    @Override
    public void allowLoops() {
        Configuration.LOOPING_ALLOWED = true;
    }

    @Override
    public void allowLoops(Boolean allowLoops) throws NotSupportedException {
        Configuration.LOOPING_ALLOWED = allowLoops;
    }

    @Override
    public boolean areLoopsAllowed() throws NotSupportedException {
        return Configuration.LOOPING_ALLOWED;
    }
}
