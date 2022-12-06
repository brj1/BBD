/*
* File TipDatesMultiTreeRandomWalker.java
*
* Copyright (C) 2017-2022 Bradley R. Jones bjones@cfenet.ubc.ca
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

import beast.core.Input;
import beast.evolution.tree.Node;
import beast.util.Randomizer;
import beast.evolution.tree.Tree;
import java.text.DecimalFormat;

/**
 * pads TipDatesRandomWalker so that edges will not haver negative branches
 * @author Bradley R. Jones
 */
public class TipDatesMultiTreeRandomWalker extends TipDatesMultiTreeOperator {
    final public Input<Double> windowSizeInput =
            new Input<>("windowSize", "the size of the window both up and down when using uniform interval OR standard deviation when using Gaussian", Input.Validate.REQUIRED);
    final public Input<Boolean> useGaussianInput =
            new Input<>("useGaussian", "Use Gaussian to move instead of uniform interval. Default false.", false);

    double windowSize = 1;
    boolean useGaussian;
    @Override
    public void initAndValidate() {
        super.initAndValidate();
        
        windowSize = windowSizeInput.get();
        useGaussian = useGaussianInput.get();
    }
    
    private double scaleNode(int i, double scale) {
        int treeID = 0;
        for (Tree tree : trees) {
            Node node = tree.getNode(taxonIndices[i][treeID++]);
            double value = node.getHeight();
            double newValue = value + scale;
            
            if (node.getParent().getHeight() < newValue) {
                return Double.NEGATIVE_INFINITY;
            }
            
            node.setHeight(newValue);
        }
        
        return 0.0;
    }
    
    @Override
    public double proposal() {
        double scale;

        if (useGaussian) {
            scale = Randomizer.nextGaussian() * windowSize;
        } else {
            scale = Randomizer.nextDouble() * 2 * windowSize - windowSize;
        }
                
        if (scale == 0) {
            return Double.NEGATIVE_INFINITY;
        }

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
        return windowSize;
    }

    @Override
    public void setCoercableParameterValue(double value) {
        windowSize = value;
    }

    @Override
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
        double delta = calcDelta(logAlpha);
        delta += Math.log(windowSize);
        windowSize = Math.exp(delta);
    }
    
        @Override
    public final String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newWindowSize = windowSize * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting window size to about " + formatter.format(newWindowSize);
        } else if (prob > 0.40) {
            return "Try setting window size to about " + formatter.format(newWindowSize);
        } else return "";
    }
}
