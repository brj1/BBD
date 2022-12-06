/*
* File TipDatesMultiTreeScaler.java
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
package bbd.tipdate;

import java.util.List;
import beast.core.Input;
import beast.evolution.tree.Node;
import beast.util.Randomizer;
import beast.evolution.tree.Tree;
import java.text.DecimalFormat;

/**
 * pads TipDatesRandomWalker so that edges will not haver negative branches
 * @author Bradley R. Jones
 */
public class TipDatesMultiTreeScaler extends TipDatesMultiTreeOperator {
    final public Input<Double> scaleFactorInput = new Input<>("scaleFactor", "scaling factor: larger means more bold proposals", 1.0);
 
    double scaleFactor = 1;
 
    @Override
    public void initAndValidate() {
        super.initAndValidate();
        
        scaleFactor = scaleFactorInput.get();
    }
    
    private double scaleNode(int i, double scale) {
        int treeID = 0;
        for (Tree tree : trees) {
            // randomly select leaf node
            Node node = tree.getNode(taxonIndices[i][treeID++]);
            double upper = node.getParent().getHeight();
            
            // scale node
            final double newValue = node.getHeight() * scale;

            // check the tree does not get negative branch lengths
            if (upper < newValue) {
                return Double.NEGATIVE_INFINITY;
            }
            node.setHeight(newValue);
        }
        
        return -Math.log(scale);
    }
    
    @Override
    public double proposal() {
        final double scale = (scaleFactor + (Randomizer.nextDouble() * ((1.0 / scaleFactor) - scaleFactor)));

        if (scaleAll) {
            double ratio = 0;
            
            for (int i = 0; i < taxonIndices.length; i++) {
                ratio += scaleNode(i, scale);
                
                if (Double.isInfinite(ratio)) {
                    return ratio;
                }
            }
            
            return ratio;
        } else {
            // randomly select leaf node
            int i = Randomizer.nextInt(taxonIndices.length);

            return scaleNode(i, scale);
        }
    }
    
    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(double value) {
        scaleFactor = value;
    }


    @Override
    public void optimize(double logAlpha) {
        double delta = calcDelta(logAlpha);
        delta += Math.log(1.0 / scaleFactor - 1.0);
        scaleFactor = 1.0 / (Math.exp(delta) + 1.0);
    }

    @Override
    public String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double sf = Math.pow(scaleFactor, ratio);

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else if (prob > 0.40) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else return "";
    }
}
