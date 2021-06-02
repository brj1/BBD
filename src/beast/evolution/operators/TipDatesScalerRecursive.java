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
 * pads TipDatesRandomScaler so that edges will not have negative branches and
 * allows parent nodes to move
 * @author Bradley R. Jones
 */
public class TipDatesScalerRecursive extends TipDatesScalerPadded {
    TipDateRecursiveShifter shifter;
    
    @Override
    public void initAndValidate() {
        super.initAndValidate();
                
        shifter = new TipDateRecursiveShifter(padding);
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
                
                depthList.addAll(shifter.recursiveProposal(newValue, node));
            }
        } else {
            // randomly select leaf node
            final int i = Randomizer.nextInt(taxonIndices.length);
            Node node = treeInput.get().getNode(taxonIndices[i]);

            double newValue = node.getHeight() * scale;

            depthList = shifter.recursiveProposal(newValue, node);
        }
        double depth = 0;

        for (double d : depthList) {
            depth += d;
        }

        // To remove
        System.err.println("proposal depth: " + depthList.size());

        return (depth == 0) ? -Math.log(scale) : -Math.log(scale) + depth;
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
