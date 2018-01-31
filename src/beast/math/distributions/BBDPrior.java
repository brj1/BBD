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
import beast.core.Description;


/**
 * Blind dating prior.
 * @author Bradley R. Jones
 */
@Description("Blind dating prior")
public class BBDPrior extends MRCAPrior {
    double[] oriDate;

    @Override
    public void initAndValidate() {
        super.dist = super.distInput.get();
        super.tree = super.treeInput.get();
        final List<String> taxaNames = new ArrayList<>();
        for (final String taxon : super.tree.getTaxaNames()) {
            taxaNames.add(taxon);
        }
        // determine nr of taxa in taxon set
        if (super.taxonsetInput.get() != null) {
            List<String> set = super.taxonsetInput.get().asStringList();
            super.nrOfTaxa = set.size();
        } else {
            // assume all taxa
            super.nrOfTaxa = taxaNames.size();
        }
       super.initialised = false;
    }
    
    protected void initialise() {
        List<String> set = null;
        int k = 0;
        if (super.taxonsetInput.get() != null) {
            set = super.taxonsetInput.get().asStringList();
        }
        final List<String> taxaNames = new ArrayList<>();
        for (final String taxon : super.tree.getTaxaNames()) {
            taxaNames.add(taxon);
        }

        super.taxonIndex = new int[super.nrOfTaxa];
        if ( set != null )  {  // m_taxonset.get() != null) {
            super.isInTaxaSet.clear();
            for (final String taxon : set) {
                final int taxonIndex_ = taxaNames.indexOf(taxon);
                if (taxonIndex_ < 0) {
                    throw new RuntimeException("Cannot find taxon " + taxon + " in data");
                }
                if (super.isInTaxaSet.contains(taxon)) {
                    throw new RuntimeException("Taxon " + taxon + " is defined multiple times, while they should be unique");
                }
                super.isInTaxaSet.add(taxon);
                super.taxonIndex[k++] = taxonIndex_;
            }
        } else {
            for (int i = 0; i < super.nrOfTaxa; i++) {
                super.taxonIndex[i] = i;
            }
        }
        
        oriDate = new double[super.nrOfTaxa];
        k = 0;
        for (final int i : super.taxonIndex) {
            oriDate[k++] = super.tree.getNode(i).getDate();
        }
        
        super.initialised = true;
    }
    
    @Override
    public double calculateLogP() {
    	if (!super.initialised) {
    		initialise();
    	}
        super.logP = 0;
        // tip date
        if (super.dist == null) {
            return super.logP;
        }
        int k = 0;
        for (final int i  : taxonIndex) {
            super.logP += super.dist.logDensity(oriDate[k++] - super.tree.getNode(i).getDate());
        }
        return super.logP;
    }
    
    
  /**
     * Loggable interface implementation follows *
     */
    @Override
    public void init(final PrintStream out) {
    	if (!super.initialised) {
    		initialise();
    	}
        if (super.dist != null) {
            out.print("logP(deltaTime(" + getID() + "))\t");
        }
        for (final int i : super.taxonIndex) {
            out.print("deltaTime(" + tree.getTaxaNames()[i] + ")\t");
        }
    }
    
    @Override
    public void log(final int sample, final PrintStream out) {
        if (super.dist != null) {
            out.print(getCurrentLogP() + "\t");
        }
        int k = 0;
        for (final int i : super.taxonIndex) {
            out.print(oriDate[k++] - super.tree.getNode(i).getDate() + "\t");
        }
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
}
