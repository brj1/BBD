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

/**
 * pads TipDatesRandomWalker so that edges will not haver negative branches
 * @author Bradley R. Jones
 */
public class TipDatesRandomWalkerPadded extends TipDatesRandomWalker {
    final public Input<Double> paddingInput = new Input<>("padding", "amount of padding to ensure that edges have nonnegative length");
            
    double padding;
    
    @Override
    public void initAndValidate() {
        super.reflectValue = false;
        
        super.initAndValidate();
        
        if (paddingInput.get() != null) {
            padding = paddingInput.get();
        } else {
            padding = 1E-4;
        }
    }
    
    @Override
    public double proposal() {
       // randomly select leaf node
        int i = Randomizer.nextInt(taxonIndices.length);
        Node node = treeInput.get().getNode(taxonIndices[i]);

        double value = node.getHeight();
        double newValue = value;
        if (useGaussian) {
            newValue += Randomizer.nextGaussian() * windowSize;
        } else {
            newValue += Randomizer.nextDouble() * 2 * windowSize - windowSize;
        }

        if (newValue > node.getParent().getHeight() - padding) { // || newValue < 0.0) {
            if (reflectValue) {
                newValue = reflectValue(newValue, 0.0, node.getParent().getHeight());
            } else {
                return Double.NEGATIVE_INFINITY;
            }
        }
        if (newValue == value) {
            // this saves calculating the posterior
            return Double.NEGATIVE_INFINITY;
        }
        node.setHeight(newValue);

        return 0.0;
    }
}