/*
* File BBDPrior.java
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
package beast.math.distributions;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Tree;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Blind dating prior.
 * @author Bradley R. Jones
 */
@Description("Blind dating prior")
public class BBDPrior extends Distribution {
    public final Input<Tree> treeInput = new Input<>("tree", "the tree containing the taxon set", Input.Validate.REQUIRED);
    public final Input<ParametricDistribution> distInput = new Input<>("distr",
            "distribution used to calculate prior over MRCA time, "
                    + "e.g. normal, beta, gamma.", Input.Validate.REQUIRED);
    public final Input<TaxonSet> taxonsetInput = new Input<>("taxonset",
            "set of taxa to date");
    
    double[] oriDate;
    ParametricDistribution dist;
    Tree tree;
    // number of taxa in taxon set
    int nrOfTaxa = -1;
    // array of flags to indicate which taxa are in the set
    Set<String> isInTaxaSet = new LinkedHashSet<>();

    // array of indices of taxa
    int[] taxonIndex;
    
    boolean initialised = false;
    
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
        initialised = false;
    }
    
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
            oriDate[k++] = tree.getNode(i).getDate();
        }
        
        initialised = true;
    }
    
    @Override
    public double calculateLogP() {
    	if (!initialised) {
    		initialise();
    	}
        logP = 0;
        // tip date
        if (dist == null) {
            return logP;
        }
        int k = 0;
        for (final int i  : taxonIndex) {
            logP += dist.logDensity(oriDate[k++] - tree.getNode(i).getDate());
        }
        return logP;
    }
    
    @Override
    public void store() {
        super.store();
    }

    @Override
    public void restore() {
        super.restore();
    }

    @Override
    protected boolean requiresRecalculation() {
        return super.requiresRecalculation();
    }
    
  /**
     * Loggable interface implementation follows *
     */
    public void init(final PrintStream out) {
    	if (!initialised) {
    		initialise();
    	}
        if (dist != null) {
            out.print("logP(deltaTime(" + getID() + "))\t");
        }
        for (final int i : taxonIndex) {
            out.print("deltaTime(" + tree.getTaxaNames()[i] + ")\t");
        }
    }
    
    @Override
    public void log(final int sample, final PrintStream out) {
        if (dist != null) {
            out.print(getCurrentLogP() + "\t");
        }
        int k = 0;
        for (final int i : taxonIndex) {
            out.print(oriDate[k++] - tree.getNode(i).getDate() + "\t");
        }
    }
    
    @Override
    public void close(final PrintStream out) {
        // nothing to do
    }

    /**
     * Valuable interface implementation follows, first dimension is log likelihood, second the time *
     */
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

    @Override
    public void sample(final State state, final Random random) {
    }

    @Override
    public List<String> getArguments() {
        return null;
    }

    @Override
    public List<String> getConditions() {
        return null;
    }
}
