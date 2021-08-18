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

import bbd.tipdate.TipDateRecursiveShifter;
import beast.core.Input;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.util.Randomizer;
import beast.util.TreeParser;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * pads TipDatesRandomWalker so that edges will not haver negative branches and
 * allows parent nodes to move
 * @author Bradley R. Jones
 */
public class TipDatesRandomWalkerRecursive extends TipDatesRandomWalkerPadded {
    final public Input<Double> rangeInput = new Input<>("range", "range that parent nodes can be moved back", Input.Validate.REQUIRED);
    final public Input<Double> moveProbInput = new Input<>("moveProb", "probability to move parent node", Input.Validate.REQUIRED);
    
    TipDateRecursiveShifter shifter;
    double range;
    double moveProp;
    
    @Override
    public void initAndValidate() {
        super.initAndValidate();
        
        moveProp = moveProbInput.get();
        
        if (moveProp < 0 || moveProp > 1) {
            throw new IllegalArgumentException("moveProp must between 0 and 1");
        }
        
        range = rangeInput.get();
        
        if (range < 0) {
            throw new IllegalArgumentException("range must be nonegative");
        }
        
        shifter = new TipDateRecursiveShifter(padding, range, moveProp, false);
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
        
        List<Node> nodeList = new ArrayList<>(0);

        if (scaleAll) {
            
            for (int i: taxonIndices) {
                nodeList.add(treeInput.get().getNode(i));
            }
            
            depthList = shifter.recursiveProposalAll(scale, nodeList);
        } else {
            // randomly select leaf node
            final int i = Randomizer.nextInt(taxonIndices.length);
            Node node = treeInput.get().getNode(taxonIndices[i]);
            nodeList.add(node);   
        }
        
        depthList = shifter.recursiveProposalAll(scale, nodeList);
        
        double depth = 0;

        for (double d : depthList) {
            depth += d;
        }

        return depth;
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
        walker.initByName("padding", 0.0, "tree", treeParser, "taxonset", taxa, "windowSize", 2.0, "weight", 1.0, "moveProb", 0.9, "range", 1.0);
        

        List<Node> nodeList = new ArrayList<>(0);
        
        nodeList.add(treeParser.getNode(walker.taxonIndices[0]));
        nodeList.add(treeParser.getNode(walker.taxonIndices[2]));


        List<Double> hastings_rec = walker.shifter.recursiveProposalAll(0.1, nodeList);
        hastings_rec = walker.shifter.recursiveProposalAll(-0.1, nodeList);
        
        hastings_rec = walker.shifter.recursiveProposalAll(1.5, nodeList);
        hastings_rec = walker.shifter.recursiveProposalAll(-1.5, nodeList);

//        walker.initByName("scaleAll", true);
        
        double hastings = walker.proposal();
    }
}
