/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beast.evolution.operators;

import beast.evolution.tree.Node;
import beast.util.Randomizer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brad
 */
public class TipDateRecursiveShifter {
    double padding;
    
    public TipDateRecursiveShifter(double padding) {
        this.padding = padding;
    }
    
    private double maxChildHeight(Node node, Node exChild) {
        double height = Double.MIN_VALUE;
        
        for (Node child : node.getChildren()) {
            if (exChild != child && child.getHeight() > height) {
                height = child.getHeight();
            }
        }
        
        return height;
    }
    
    public List<Double> recursiveProposal(double newValue, Node node) {
        List<Double> depth = new ArrayList<>(0);
        final Node parent = node.getParent();
        
        if (parent != null) {
            final double parentHeight = parent.getHeight();
            
            // push parent node up
            if (parentHeight < newValue) {
                depth = recursiveProposal(newValue, parent);
                final double range = newValue - maxChildHeight(parent, null);

                depth.add(range > padding ? Math.log(range) : 0);
                
                node.setHeight(newValue - padding * depth.size());
                
                return depth;
            // push close parent node down (1E-15 included to account for inprecision)
            } else if (node.getHeight() > newValue && ((parentHeight - node.getHeight() - padding) < 1E-15)) {
                final double maxHeight = maxChildHeight(parent, node);
                final double range = parentHeight -  Math.max(maxHeight, newValue);
                final double nextShift = Randomizer.nextDouble() * range;
                
                if (nextShift > padding) {
                    final double newDepth = -Math.log(range);

                    depth = recursiveProposal(parentHeight - nextShift, parent);
                    depth.add(newDepth);
                }
            }
        }
        
        node.setHeight(newValue);
        
        return depth;
    }
    
}
