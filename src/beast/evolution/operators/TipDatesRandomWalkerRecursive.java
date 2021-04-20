/*
* File TipDatesRandomWalkerPadded.java
*
* Copyright (C) 2017 Bradley R. Jones bjones@cfenet.ubc.ca
*
* This file is part of BBD.
* See the NOTICE file distributed with this work for additional
* information regarding copyright ownership and licensing.
*
* BBD is free software; you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
*  BBD is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with BBD; if not, write to the
* Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
* Boston, MA  02110-1301  USA
*/
package beast.evolution.operators;

import beast.core.Input;
import beast.evolution.tree.Node;
import beast.util.Randomizer;
import java.util.List;
import java.util.ArrayList;

/**
 * pads TipDatesRandomWalker so that edges will not haver negative branches
 * @author Bradley R. Jones
 */
public class TipDatesRandomWalkerRecursive extends TipDatesRandomWalker {
    final public Input<Double> depthPenaltyInput = new Input<>("depthPenalty", "penalty for shifting parent nodes");
    final public Input<Double> paddingInput = new Input<>("padding", "amount of padding to ensure that edges have nonnegative length");
            
    double padding;            
    double depthPenalty;
    
    @Override
    public void initAndValidate() {
        super.reflectValue = false;
        
        super.initAndValidate();
        
        if (depthPenaltyInput.get() != null) {
            depthPenalty = depthPenaltyInput.get();
        } else {
            depthPenalty = 0;
        }
        
        if (paddingInput.get() != null) {
            padding = paddingInput.get();
        } else {
            padding = 1E-4;
        }
    }
    
    private double minChildHeight(Node node, Node exChild) {
        double height = Double.MAX_VALUE;
        
        for (Node child : node.getChildren()) {
            if (exChild != child && child.getHeight() < height) {
                height = child.getHeight();
            }
        }
        
        return height;
    }
    
    public List<Double> recursiveProposal(double newValue, Node node) {
        List<Double> depth = new ArrayList<>(0);
        
         if (node.getParent() != null && newValue > node.getParent().getHeight()) { // || newValue < 0.0) {
            depth = recursiveProposal(newValue, node.getParent());
            
            depth.add(Math.log(minChildHeight(node.getParent(), null) - newValue));
        }
         
        if (node.getParent() != null && (newValue - node.getParent().getHeight()) < padding) {
            final double parentHeight = node.getParent().getHeight();
            final double maxHeight = minChildHeight(node.getParent(), node);
            final double range = (maxHeight < newValue ? maxHeight : newValue) - parentHeight;
            final double nextShift = Randomizer.nextDouble() * range;
            
            if (nextShift > padding) {
                final double newDepth = -Math.log(range);
                
                depth = recursiveProposal(parentHeight - nextShift, node.getParent());
                depth.add(newDepth);
            }
        }
        
        node.setHeight(newValue - padding * depth.size());
        
        return depth;
    }
    
    @Override
    public double proposal() {
       // randomly select leaf node
        final int i = Randomizer.nextInt(taxonIndices.length);
        Node node = treeInput.get().getNode(taxonIndices[i]);

        double newValue = node.getHeight();
        if (useGaussian) {
            newValue += Randomizer.nextGaussian() * windowSize;
        } else {
            newValue += Randomizer.nextDouble() * 2 * windowSize - windowSize;
        }
        
        if (newValue == node.getHeight())
            return Double.NEGATIVE_INFINITY;
        
        List<Double> depthList = recursiveProposal(newValue, node);
        double depth = 0;
        
        for (double d : depthList) {
            depth += d;
        }
        
        //To remove
        if (depth == 0) {
            System.err.println("proposal depth: " + depth);
        }
        
        return (depth == 0) ? 0 : depth +  depthPenalty * depthList.size() * depth / Math.abs(depth);
    }
}
