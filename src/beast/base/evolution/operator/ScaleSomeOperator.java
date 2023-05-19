/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beast.base.evolution.operator;

import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.util.Randomizer;

/**
 * Scale operator that 
 * @author Bradley R. Jones
 */
public class ScaleSomeOperator extends ScaleOperator {
     /**
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double proposal() {

        try {

            double hastingsRatio;
            final double scale = getScaler();

            if (m_bIsTreeScaler) {

                final Tree tree = treeInput.get();
                if (rootOnlyInput.get()) {
                    final Node root = tree.getRoot();
                    final double newHeight = root.getHeight() * scale;

                    if (newHeight < Math.max(root.getLeft().getHeight(), root.getRight().getHeight())) {
                        return Double.NEGATIVE_INFINITY;
                    }
                    root.setHeight(newHeight);
                    return -Math.log(scale);
                } else {
                    // scale the beast.tree
                    final int internalNodes = tree.scale(scale);
                    return Math.log(scale) * (internalNodes - 2);
                }
            }

            // not a tree scaler, so scale a parameter
            final boolean scaleAll = scaleAllInput.get();
            final int specifiedDoF = degreesOfFreedomInput.get();
            final boolean scaleAllIndependently = scaleAllIndependentlyInput.get();

            final RealParameter param = parameterInput.get();

            assert param.getLower() != null && param.getUpper() != null;

            final int dim = param.getDimension();

            if (scaleAllIndependently) {
                // update all dimensions independently.
                hastingsRatio = 0;
                final BooleanParameter indicators = indicatorInput.get();
                if (indicators != null) {
                    final int dimCount = indicators.getDimension();
                    final Boolean[] indicator = indicators.getValues();
                    final boolean impliedOne = dimCount == (dim - 1);
                    for (int i = 0; i < dim; i++) {
                        if( (impliedOne && (i == 0 || indicator[i-1])) || (!impliedOne && indicator[i]) )  {
                            final double scaleOne = getScaler();
                            final double newValue = scaleOne * param.getValue(i);

                            hastingsRatio -= Math.log(scaleOne);

                            if (outsideBounds(newValue, param)) {
                                return Double.NEGATIVE_INFINITY;
                            }

                            param.setValue(i, newValue);
                        }
                    }
                }  else {

                    for (int i = 0; i < dim; i++) {

                        final double scaleOne = getScaler();
                        final double newValue = scaleOne * param.getValue(i);

                        hastingsRatio -= Math.log(scaleOne);

                        if( outsideBounds(newValue, param) ) {
                            return Double.NEGATIVE_INFINITY;
                        }

                        param.setValue(i, newValue);
                    }
                }
            } else if (scaleAll) {
                // update all dimensions
                // hasting ratio is dim-2 times of 1dim case. would be nice to have a reference here
                // for the proof. It is supposed to be somewhere in an Alexei/Nicholes article.

                // all Values assumed independent!
                int computedDoF = 0;
                final BooleanParameter indicators = indicatorInput.get();
                if (indicators != null) {
                    final int dimCount = indicators.getDimension();
                    final Boolean[] indicator = indicators.getValues();
                    final boolean impliedOne = dimCount == (dim - 1);
                    for (int i = 0; i < dim; i++) {
                        if( ((impliedOne && (i == 0 || indicator[i-1])) || (!impliedOne && indicator[i])) )  {
                            final double newValue = scale * param.getValue(i);

                            computedDoF += 1;

                            if (outsideBounds(newValue, param)) {
                                return Double.NEGATIVE_INFINITY;
                            }

                            param.setValue(i, newValue);
                        }
                    }
                } else {
                    computedDoF = param.scale(scale);
                } 
                final int usedDoF = (specifiedDoF > 0) ? specifiedDoF : computedDoF ;
                hastingsRatio = (usedDoF - 2) * Math.log(scale);
            } else {
                hastingsRatio = -Math.log(scale);

                // which position to scale
                final int index;
                final BooleanParameter indicators = indicatorInput.get();
                if (indicators != null) {
                    final int dimCount = indicators.getDimension();
                    final Boolean[] indicator = indicators.getValues();
                    final boolean impliedOne = dimCount == (dim - 1);

                    // available bit locations. there can be hundreds of them. scan list only once.
                    final int[] loc = new int[dimCount + 1];
                    int locIndex = 0;

                    if (impliedOne) {
                        loc[locIndex] = 0;
                        ++locIndex;
                    }
                    for (int i = 0; i < dimCount; i++) {
                        if (indicator[i]) {
                            loc[locIndex] = i + (impliedOne ? 1 : 0);
                            ++locIndex;
                        }
                    }

                    if (locIndex > 0) {
                        final int rand = Randomizer.nextInt(locIndex);
                        index = loc[rand];
                    } else {
                        return Double.NEGATIVE_INFINITY; // no active indicators
                    }

                } else {
                    // any is good
                    index = Randomizer.nextInt(dim);
                }

                final double oldValue = param.getValue(index);

                if (oldValue == 0) {
                    // Error: parameter has value 0 and cannot be scaled
                    return Double.NEGATIVE_INFINITY;
                }

                final double newValue = scale * oldValue;

                if (outsideBounds(newValue, param)) {
                    // reject out of bounds scales
                    return Double.NEGATIVE_INFINITY;
                }

                param.setValue(index, newValue);
                // provides a hook for subclasses
                //cleanupOperation(newValue, oldValue);
            }

            return hastingsRatio;

        } catch (Exception e) {
            // whatever went wrong, we want to abort this operation...
            return Double.NEGATIVE_INFINITY;
        }
    }
}
