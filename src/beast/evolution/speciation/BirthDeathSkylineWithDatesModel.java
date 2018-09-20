/*
* File BBDPriorInputEditor.java
*
* Copyright (C) 2018 Bradley R. Jones bjones@cfenet.ubc.ca
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
package beast.evolution.speciation;

import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.TreeInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Blind dating tree prior.
 * @author Bradley R. Jones
 */
public class BirthDeathSkylineWithDatesModel extends BirthDeathSkylineModel {    

    // the times for rho sampling
    public Input<RealParameter> rhoSamplingDatesInput =
            new Input<RealParameter>("rhoSamplingDates", "The dates t_i specifying when rho-sampling occurs", (RealParameter) null);

    RealParameter rhoSamplingDates;
    
    @Override
    public void initAndValidate() {
        rhoSamplingDates = rhoSamplingDatesInput.get();
        
        if (rhoSamplingDates != null) {
            TreeInterface tree = treeInput.get();
            if (tree != null) {
                Node root = tree.getRoot();
                double maxdate = root.getDate() + root.getHeight();
                RealParameter samplingTimes = new RealParameter(rhoSamplingDates.getValues());
                
                for (int i = 0; i < rhoSamplingDates.getDimension(); i++) {
                    samplingTimes.setValue(i, maxdate - rhoSamplingDates.getValue(i));
                }
                
                rhoSamplingTimes.setValue(samplingTimes, this);
            }
        }
        
        super.initAndValidate();
    }
    
    @Override
    public Double preCalculation(TreeInterface tree) {
        if (rhoSamplingDates != null) {
            Node root = tree.getRoot();
            double maxdate = root.getDate() + root.getHeight();
            RealParameter samplingTimes = new RealParameter(rhoSamplingDates.getValues());
            
            for (int i = 0; i < rhoSamplingDates.getDimension(); i++) {
                samplingTimes.setValue(i, maxdate - rhoSamplingDates.getValue(i));
            }
                
            rhoSamplingTimes.set(samplingTimes);
        }
        
        return super.preCalculation(tree);
    }
    
    @Override
    public void getChangeTimes(List<Double> changeTimes, RealParameter intervalTimes, int numChanges, boolean relative, boolean reverse) {
        changeTimes.clear();

        if (printTempResults) System.out.println("relative = " + relative);

        double maxTime;

        if (origin.get() != null) {
            maxTime = originIsRootEdge.get()? treeInput.get().getRoot().getHeight() + origin.get().getValue() :origin.get().getValue();
        } else {
            maxTime = treeInput.get().getRoot().getHeight();
        }

        if (intervalTimes == null) { //equidistant

            double intervalWidth = maxTime / (numChanges + 1);

            double end;
            for (int i = 1; i <= numChanges; i++) {
                end = (intervalWidth) * i;
                changeTimes.add(end);
            }
            end = maxTime;
            changeTimes.add(end);

        } else {

            if ((!isBDSIR()) && numChanges > 0 && intervalTimes.getDimension() != numChanges + 1) {
                throw new RuntimeException("The time interval parameter should be numChanges + 1 long (" + (numChanges + 1) + ").");
            }

            int dim = intervalTimes.getDimension();

            ArrayList<Double> sortedIntervalTimes = new ArrayList<>();
            for (int i=0; i< dim; i++) {
                sortedIntervalTimes.add(intervalTimes.getValue(i));
            }
            Collections.sort(sortedIntervalTimes);

//            if (!reverse && sortedIntervalTimes.get(0) != 0.0) {
//                throw new RuntimeException("First time in interval times parameter should always be zero.");
//            }

//            if(intervalTimes.getValue(dim-1)==maxTime) changeTimes.add(0.); //rhoSampling

            double end;
            for (int i = (reverse?0:1); i < dim; i++) {
                end = reverse? (relative?1.0:maxTime) - sortedIntervalTimes.get(dim - i - 1) :sortedIntervalTimes.get(i);
                if (relative) end *= maxTime;
                if (end != maxTime) changeTimes.add(end);
            }
            end = maxTime;
            changeTimes.add(end);
        }
    }
}
