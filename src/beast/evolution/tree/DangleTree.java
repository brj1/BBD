package beast.evolution.tree;

import beast.core.StateNode;
import java.io.PrintStream;
import beast.core.Input;
import beast.evolution.alignment.Taxon;

/**
 * Ensures Trait values are properly stored and restored when resuming BEAST
 * runs.
 * @author Bradley R. Jones
 */
public class DangleTree extends StateNode {
    public final Input<Tree> treeInput = new Input<>("tree", "the tree", Input.Validate.REQUIRED);
    public final Input<Taxon> taxonInput = new Input<>("taxon", "taxon that has a fixed date", Input.Validate.REQUIRED);
   
    @Override
    public void initAndValidate() {
    }
            
    @Override
    public void setEverythingDirty(boolean isDirty) {
    }

    @Override
    public StateNode copy() {       
        return this;
    }

    @Override
    public void assignTo(StateNode other) {
    }

    @Override
    public void assignFrom(StateNode other) {
    }

    @Override
    public void assignFromFragile(StateNode other) {
    }

    @Override
    public void fromXML(org.w3c.dom.Node node) {
        Tree tree = treeInput.get();
        
        if (tree.hasDateTrait()) {
            double offset = 0;
            String taxonID = taxonInput.get().getID();
            
            for (Node taxonNode : tree.getExternalNodes()) {
                if (taxonID.equals(tree.getTaxonId(taxonNode))) {
                    offset = tree.getDateTrait().getValue(taxonID) - taxonNode.getHeight();
                    
                    break;
                }
            }
            
            if (Math.abs(offset) != 0)
                shiftTreeHeight(tree.getRoot(), offset);
        }
    }
    
    private void shiftTreeHeight(Node node, double offset) {
        node.setHeightDA(node.getHeight() + offset);
        
        for (Node child:  node.getChildren()) {
            shiftTreeHeight(child, offset);
        }
    }

    @Override
    public int scale(double d) {
        return 0;
    }

    @Override
    protected void store() {
        
    }

    @Override
    public void restore() {
        
    }

    @Override
    public void init(PrintStream stream) {
    }

    @Override
    public void close(PrintStream stream) {
    }

    @Override
    public int getDimension() {
        return 0;
    }

    @Override
    public double getArrayValue(int i) {
        return 0;
    }
    
    @Override
    public String toString() {
//        return tree.toString();
        return "";
    }
}
