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
public class TipDatesRandomWalkerRecursive extends TipDatesRandomWalkerPadded {
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
                final double range = newValue - maxChildHeight(parent, null);

                depth.add(Math.log(range > padding ? range : 1));
                
                node.setHeight(newValue - padding * depth.size());
                
                return depth;
            // push close parent node down
            } else if (node.getHeight() > newValue && (parentHeight - node.getHeight()) <= padding) {
                final double maxHeight = maxChildHeight(parent, node);
                final double range = Math.max(maxHeight, newValue);
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
        double scale;
        List<Double> depthList;

        if (useGaussian) {
            scale = Randomizer.nextGaussian() * windowSize;
        } else {
            scale = Randomizer.nextDouble() * 2 * windowSize - windowSize;
        }
        
        if (scale == 0) {
            return Double.NEGATIVE_INFINITY;
        }

        if (scaleAll) {
            depthList = new ArrayList<>(0);
            List<Node> nodeList = new ArrayList<>(0);
            for (int i: taxonIndices) {
                nodeList.add(treeInput.get().getNode(i));
            }
            
            nodeList.sort(new NodeHeightComp(scale < 0));
            
            for (Node node: nodeList) {
                double newValue = node.getHeight() + scale;
                
                depthList.addAll(recursiveProposal(newValue, node));
            }
        } else {
            // randomly select leaf node
            final int i = Randomizer.nextInt(taxonIndices.length);
            Node node = treeInput.get().getNode(taxonIndices[i]);

            double newValue = node.getHeight() + scale;

            depthList = recursiveProposal(newValue, node);
        }
        double depth = 0;

        for (double d : depthList) {
            depth += d;
        }

        // To remove
        System.err.println("proposal depth: " + depthList.size() + ", depth: " + depth + ", scale: " + scale);

        return (depth == 0) ? 0 : depth + depthPenalty * depthList.size() * depth / Math.abs(depth);

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
    
    public static void main(String[] args) {
        String newick = "((0:1.0,1:1.0)4:1.0,(2:1.0,3:1.0)5:0.5)6:0.0;";
        TreeParser treeParser = new TreeParser(newick, false, false, true, 0);
        
        Taxon tax0 = new Taxon();
        tax0.setID("0");
        Taxon tax1 = new Taxon();
        tax1.setID("1");
        Taxon tax2 = new Taxon();
        tax2.setID("2");
        
        TaxonSet taxa = new TaxonSet();
        taxa.initByName("taxon", tax0, "taxon", tax1, "taxon", tax2);
                
        TipDatesRandomWalkerRecursive walker = new TipDatesRandomWalkerRecursive();
        walker.initByName("padding", 0.1, "tree", treeParser, "taxonset", taxa, "windowSize", 2.0, "weight", 1.0);
        
        //walker.recursiveProposal(1.5, treeParser.getNode(walker.taxonIndices[0]));
        //walker.recursiveProposal(0.5, treeParser.getNode(walker.taxonIndices[0]));
        
        walker.initByName("scaleAll", true);
        
        walker.proposal();
    }
}
