package algorithms.hybrid;

import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.Collection;

abstract class TreeNode {

    Collection<OWLAxiom> label;
    Integer depth;
}
