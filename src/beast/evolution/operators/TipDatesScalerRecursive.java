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
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.util.Randomizer;
import beast.util.TreeParser;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ListIterator;

/**
 * pads TipDatesRandomWalker so that edges will not haver negative branches
 * @author Bradley R. Jones
 */
public class TipDatesScalerRecursive extends TipDatesScalerPadded {
    final public Input<Double> depthPenaltyInput = new Input<>("depthPenalty", "penalty for shifting parent nodes");
                        
    double depthPenalty;
    
    @Override
    public void initAndValidate() {
        super.initAndValidate();
        
        if (depthPenaltyInput.get() != null) {
            depthPenalty = depthPenaltyInput.get();
        } else {
            depthPenalty = 0;
        }
    }
    
    private double maxChildHeight(Node node, Node exChild) {
        double height = Double.MIN_VALUE;
        
        for (Node child : node.getChildren()) {
            if (exChild != child && child.getHeight() < height) {
                height = child.getHeight();
            }
        }
        
        return height;
    }
    
    private List<Double> recursiveProposal(double newValue, Node node) {
        List<Double> depth = new ArrayList<>(0);
        final Node parent = node.getParent();
        
        if (parent != null) {
            final double parentHeight = parent.getHeight();
            
            // push parent node up
            if (parentHeight < newValue) {
                depth = recursiveProposal(newValue, parent);
                double range = newValue - maxChildHeight(parent, null);

                depth.add(Math.log(range > padding ? range : 1));
                
                node.setHeight(newValue - padding * depth.size());
                
                return depth;
            // push close parent node down
            } else if (node.getHeight() > newValue && (parentHeight - node.getHeight()) <= padding) {
                final double maxHeight = maxChildHeight(parent, node);
                final double range = Math.min(parentHeight - maxHeight, newValue);
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
    
    @Override
    public double proposal() {
        final double scale = (scaleFactor + (Randomizer.nextDouble() * ((1.0 / scaleFactor) - scaleFactor)));
        List<Double> depthList;

        
        if (scaleAll) {
            depthList = new ArrayList<>(0);
            List<Node> nodeList = new ArrayList<>(0);
            for (int i: taxonIndices) {
                nodeList.add(treeInput.get().getNode(i));
            }
            
            nodeList.sort(new NodeHeightComp(scale < 0));
            
            for (Node node: nodeList) {
                double newValue = node.getHeight() * scale;
                
                depthList.addAll(recursiveProposal(newValue, node));
            }
        } else {
            // randomly select leaf node
            final int i = Randomizer.nextInt(taxonIndices.length);
            Node node = treeInput.get().getNode(taxonIndices[i]);

            double newValue = node.getHeight() * scale;

            depthList = recursiveProposal(newValue, node);
        }
        double depth = 0;

        for (double d : depthList) {
            depth += d;
        }

        // To remove
        System.err.println("proposal depth: " + depthList.size());

        return (depth == 0) ? -Math.log(scale) : -Math.log(scale) + depth + depthPenalty * depthList.size() * depth / Math.abs(depth);
    }
    
    private class NodeHeightComp implements Comparator<Node> {
        
        final private int ascendingFactor;
        
        public NodeHeightComp(boolean ascending) {
            ascendingFactor = ascending ? 1 : -1;
        }

        @Override
        public int compare(Node node1, Node node2) {
            return Double.compare(node1.getHeight(), node2.getHeight()) * ascendingFactor;
        }
    }
}
