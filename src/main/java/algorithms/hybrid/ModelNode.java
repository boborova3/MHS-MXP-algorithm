package algorithms.hybrid;

import models.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ModelNode extends TreeNode {
    List<OWLAxiom> data;
    Set<OWLAxiom> lenght_one_explanations = new HashSet<>();

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ModelNode) {
            ModelNode node = (ModelNode) obj;
            return data.containsAll(node.data) && node.data.containsAll(data);
        }
        return false;
    }

    public void add_to_explanations(List<OWLAxiom> explanations){
        lenght_one_explanations.addAll(explanations);
    }

    public void add_node_explanations(ModelNode node){
        lenght_one_explanations.addAll(node.lenght_one_explanations);
    }

    public Set<OWLAxiom> get_explanations(){
        return lenght_one_explanations;
    }
}
