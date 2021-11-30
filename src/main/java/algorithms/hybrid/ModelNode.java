package algorithms.hybrid;

import models.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModelNode extends TreeNode {
    public List<OWLAxiom> data;
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
        /*System.out.println("TADAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAaaa");
        for(OWLAxiom a : lenght_one_explanations){
            System.out.println(a);
        }
        System.out.println("KONIEEEEEEEEEEEEEEEEEEEEEEEEEEEEC");
        System.out.println("TADAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAaaa    DATA");
        for(OWLAxiom a : data){
            System.out.println(a);
        }
        System.out.println("KONIEEEEEEEEEEEEEEEEEEEEEEEEEEEEC");
        System.out.println("TADAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAaaa    LABEL");
        for(OWLAxiom a : label){
            System.out.println(a);
        }
        System.out.println("KONIEEEEEEEEEEEEEEEEEEEEEEEEEEEEC");*/
        lenght_one_explanations.addAll(explanations);
    }

    public void add_node_explanations(ModelNode node){
        lenght_one_explanations.addAll(node.lenght_one_explanations);
    }

    public Set<OWLAxiom> get_explanations(){
        return lenght_one_explanations;
    }
}
