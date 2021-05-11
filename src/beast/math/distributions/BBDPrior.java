/*
* File BBDPrior.java
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
package beast.math.distributions;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;


/**
 * Blind dating prior.
 * @author Bradley R. Jones
 */
@Description("Blind dating prior")
public class BBDPrior extends MRCAPrior {
    public final Input<RealParameter> startingDateProbInput = new Input<>("collectedDateProbability", "the probability that the collected date is correct");
//    public final Input<Double> startingDateDifferenceInput = new Input<>("startDateDifference", "the starting value");
    public final Input<Double> collectedDateInput = new Input<>("collectedDate", "the collected date");
    
    double[] oriDate;
    double startingDateProb;
    double collectedDate;
//    double startingDateDifference;
    
    public BBDPrior() {
        onlyUseTipsInput.defaultValue = true;
    }

    @Override
    public void initAndValidate() {
        dist = distInput.get();
        tree = treeInput.get();
        final List<String> taxaNames = new ArrayList<>();
        for (final String taxon : tree.getTaxaNames()) {
            taxaNames.add(taxon);
        }
        // determine nr of taxa in taxon set
        if (taxonsetInput.get() != null) {
            List<String> set = taxonsetInput.get().asStringList();
            nrOfTaxa = set.size();
        } else {
            // assume all taxa
            nrOfTaxa = taxaNames.size();
        }
        
        if (startingDateProbInput.get() == null)
            startingDateProb = 0;
        else
            startingDateProb = startingDateProbInput.get().getValue();
        
        if (collectedDateInput.get() == null)
            if (onlyUseTips) {
                collectedDate = Double.NaN;
            } else {
                throw new IllegalArgumentException("Must specify collectedDate (offset) when using MRCA or root");
            }
        else
            collectedDate = collectedDateInput.get();
        
//        if (startingDateDifferenceInput.get() == null)
//            startingDateDifference = 0;
//        else
//            startingDateDifference = startingDateDifferenceInput.get();
       
        if (startingDateProb > 1 || startingDateProb < 0)
            throw new IllegalArgumentException("Starting Date Probability must be betweem 0 and 1");
        
        initialised = false;
    }
    
    @Override
    protected void initialise() {
        List<String> set = null;
        int k = 0;
        if (taxonsetInput.get() != null) {
            set = taxonsetInput.get().asStringList();
        }
        final List<String> taxaNames = new ArrayList<>();
        for (final String taxon : tree.getTaxaNames()) {
            taxaNames.add(taxon);
        }

        taxonIndex = new int[nrOfTaxa];
        if ( set != null )  {  // m_taxonset.get() != null) {
            isInTaxaSet.clear();
            for (final String taxon : set) {
                final int taxonIndex_ = taxaNames.indexOf(taxon);
                if (taxonIndex_ < 0) {
                    throw new RuntimeException("Cannot find taxon " + taxon + " in data");
                }
                if (isInTaxaSet.contains(taxon)) {
                    throw new RuntimeException("Taxon " + taxon + " is defined multiple times, while they should be unique");
                }
                isInTaxaSet.add(taxon);
                taxonIndex[k++] = taxonIndex_;
            }
        } else {
            for (int i = 0; i < nrOfTaxa; i++) {
                taxonIndex[i] = i;
            }
        }
        
        oriDate = new double[nrOfTaxa];
        k = 0;
        for (final int i : taxonIndex) {
            Node node = tree.getNode(i);
            if (collectedDate == Double.NaN)
                oriDate[k] = node.getDate();
            else
                oriDate[k] = collectedDate;
            
//            if (startingDateDifference != 0)
//                node.setHeight(node.getHeight() + startingDateDifference);
            k++;
        }
                
        initialised = true;
    }
    
    @Override
    public double calculateLogP() {
        if (!initialised) {
            initialise();

            // recompute tree
//            if (startingDateDifference != 0)
//                return Double.NEGATIVE_INFINITY;
        }
        logP = 0;
        if (onlyUseTips) {
            // tip date
            if (dist == null) {
                return logP;
            }
            int k = 0;

            for (final int i : taxonIndex) {
                if (startingDateProb > 0 && oriDate[k] - tree.getNode(i).getDate() == 0) {
                    logP += java.lang.Math.log(startingDateProb);
                } else {
                    logP += dist.logDensity(oriDate[k] - tree.getNode(i).getDate()) + java.lang.Math.log(1 - startingDateProb);
                }
                k++;
            }
            
            return logP;
        } else if (useRoot) {
            // root
            if (dist != null) {
                MRCATime = tree.getRoot().getDate();
                if (startingDateProb > 0 && collectedDate - MRCATime == 0) {
                    logP += java.lang.Math.log(startingDateProb);
                } else {
                    logP += dist.logDensity(collectedDate - MRCATime)  + java.lang.Math.log(1 - startingDateProb);
                }
            }
            return logP;
        } else {
            // internal node
            Node m;
            if (taxonIndex.length == 1) {
                isMonophyletic = true;
                m = tree.getNode(taxonIndex[0]);
            } else {
                nseen = 0;
                m = getCommonAncestor();
                isMonophyletic = (nseen == 2 * taxonIndex.length - 1);
            }
            if (useOriginate) {
                if (!m.isRoot()) {
                    MRCATime = m.getParent().getDate();
                } else {
                    MRCATime = m.getDate();
                }
            } else {
                MRCATime = m.getDate();
            }
        }
        if (isMonophyleticInput.get() && !isMonophyletic) {
            logP = Double.NEGATIVE_INFINITY;
            return Double.NEGATIVE_INFINITY;
        }
        if (dist != null) {
            if (startingDateProb > 0 && collectedDate - MRCATime == 0) {
                logP += java.lang.Math.log(startingDateProb);
            } else {
                logP += dist.logDensity(collectedDate - MRCATime) + java.lang.Math.log(1 - startingDateProb);
            }
        }

        return logP;
    }
    
    
    @Override
    public void init(final PrintStream out) {
    	if (!initialised) {
    		initialise();
    	}
        if (dist != null) {
            out.print("logP(deltaTime(" + getID() + "))\t");
        }
        if (onlyUseTips) {
            for (final int i : taxonIndex) {
                out.print("deltaTime(" + tree.getTaxaNames()[i] + ")\t");
            }
        } else {
            out.print("deltaTime(" + getID() + ")\t");
        }
    }
    
    @Override
    public void log(final long sample, final PrintStream out) {
        if (dist != null) {
            out.print(getCurrentLogP() + "\t");
        }
        if (onlyUseTips) {
            int k = 0;
            for (final int i : taxonIndex) {
                out.print(oriDate[k++] - tree.getNode(i).getDate() + "\t");
            }
        } else if (useRoot) {
            out.print(collectedDate - tree.getRoot().getDate() + "\t");
        } else {
             // internal node
            Node m;
            if (taxonIndex.length == 1) {
                isMonophyletic = true;
                m = tree.getNode(taxonIndex[0]);
            } else {
                nseen = 0;
                m = getCommonAncestor();
                isMonophyletic = (nseen == 2 * taxonIndex.length - 1);
            }
            if (useOriginate) {
                if (!m.isRoot()) {
                    MRCATime = m.getParent().getDate();
                } else {
                    MRCATime = m.getDate();
                }
            } else {
                MRCATime = m.getDate();
            }
            
            out.print(collectedDate - MRCATime + "\t");
        }
    }
    
    @Override
    public int getDimension() {
        return 1;
    }
    
    @Override
    public double getArrayValue() {
    	if (Double.isNaN(logP)) {
    		try {
    			calculateLogP();
    		}catch (Exception e) {
    			logP  = Double.NaN;
    		}
    	}
        return logP;
    }

    @Override
    public double getArrayValue(final int dim) {
    	if (Double.isNaN(logP)) {
    		try {
    			calculateLogP();
    		}catch (Exception e) {
    			logP  = Double.NaN;
    		}
    	}
        switch (dim) {
            case 0:
                return logP;
            default:
                return 0;
        }
    }
}
