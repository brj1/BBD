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
import beast.evolution.tree.Tree;
import beast.util.Randomizer;

/**
 * pads TipDatesRandomWalker so that edges will not haver negative branches
 * @author Bradley R. Jones
 */
public class TipDatesScalerPadded extends TipDatesScaler {
    final public Input<Double> paddingInput = new Input<>("padding", "amount of padding to ensure that edges have nonnegative length");
    final public Input<Boolean> scaleAllInput = new Input<>("scaleAll",
            "if true, all tips dates are scaled, otherwise one is randomly selected",
            false);
            
    double padding;
    boolean scaleAll;
    
    @Override
    public void initAndValidate() {
        super.initAndValidate();
        
        if (paddingInput.get() != null) {
            padding = paddingInput.get();
        } else {
            padding = 1E-4;
        }
        
        if (scaleAllInput.get() != null) {
            scaleAll = scaleAllInput.get();
        } else {
            scaleAll = false;
        }
    }
    
    private double scaleNode(int i, double scale) {
        Tree tree = treeInput.get(this);

        // randomly select leaf node
        Node node = tree.getNode(taxonIndices[i]);
        double upper = node.getParent().getHeight();
        //double lower = 0.0;
        //final double newValue = (Randomizer.nextDouble() * (upper -lower)) + lower;

        // scale node
        final double newValue = node.getHeight() * scale;

        // check the tree does not get negative branch lengths
        if (upper - newValue < padding) {
            return Double.NEGATIVE_INFINITY;
        }
        node.setHeight(newValue);

        return -Math.log(scale);
    }
    
    @Override
    public double proposal() {
        final double scale = (scaleFactor + (Randomizer.nextDouble() * ((1.0 / scaleFactor) - scaleFactor)));

        if (scaleAll) {
            double ratio = 0;
            
            for (int i : taxonIndices) {
                ratio += scaleNode(i, scale);
                
                if (Double.isInfinite(ratio)) {
                    return ratio;
                }
            }
            
            return ratio;
        } else {
            // randomly select leaf node
            final int i = Randomizer.nextInt(taxonIndices.length);

            return scaleNode(i, scale);
        }
    }
}
