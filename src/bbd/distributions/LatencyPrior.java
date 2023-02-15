/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bbd.distributions;

import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Prior for integration dates of HIV sequences entering the latent reservoir.
 * @author Bradley R. Jones
 */
public class LatencyPrior extends Distribution {
    public final Input<Double> collectedDateInput = new Input<>("collectedDate", "the collected date");
    public final Input<Tree> treeInput = new Input<>("tree", "the tree containing the taxon set", Input.Validate.REQUIRED);
    public final Input<TaxonSet> taxonsetInput = new Input<>("taxonset",
            "set of taxa for which prior information is available");
    public final Input<Double> latencyRateInput = new Input<>("latencyRate", 
            "rate that virus goes latent", Input.Validate.REQUIRED);
    public final Input<Double> reactivationRateInput = new Input<>("reactivationRate",
            "rate that latent cells reactivate", Input.Validate.REQUIRED);

    
    double[] oriDate;
    double collectedDate;
    
    double latencyRate;
    double reactivationRate;
    
    Tree tree;
    int nrOfTaxa = -1;
    // array of flags to indicate which taxa are in the set
    Set<String> isInTaxaSet = new LinkedHashSet<>();

    // array of indices of taxa
    int[] taxonIndex;
    
    boolean initialised = false;
   
    @Override
    public void initAndValidate() {
        tree = treeInput.get();
        final List<String> taxaNames = new ArrayList<>();
        for (final String taxon : tree.getTaxaNames()) {
            taxaNames.add(taxon);
        }
        // determine nr of taxa in taxon set
        List<String> set = null;
        if (taxonsetInput.get() != null) {
            set = taxonsetInput.get().asStringList();
            nrOfTaxa = set.size();
        } else {
            // assume all taxa
            nrOfTaxa = taxaNames.size();
        }

        if (collectedDateInput.get() != null) {
            collectedDate = collectedDateInput.get();
        } else {
            collectedDate = Double.NaN;
        }
        
        latencyRate = latencyRateInput.get();
        reactivationRate = reactivationRateInput.get();
                
        initialised = false;
    }
    
    @Override
    public double calculateLogP() {
        if (!initialised) {
            initialise();       
        }
        
        logP = 0;
        
        int k = 0;
        
        final double tMRCA = tree.getRoot().getDate();
        final double rate = reactivationRate - latencyRate;
                
        for (final int i : taxonIndex) {
            double date = tree.getNode(i).getDate();
            
            if (date > tMRCA && date < oriDate[i]) {
                // likelihood of going latent at `date` given sampling date
                logP += Math.exp(rate * date) *
                    rate / 
                    (Math.exp(rate * oriDate[i]) -
                        Math.exp(rate * tMRCA));
            } else {
                // not in range
                return Double.NEGATIVE_INFINITY;
            }
            k++;
        }
        
        return logP;
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
            Node node = tree.getNode(i);
            if (collectedDate == Double.NaN)
                oriDate[k] = node.getDate();
            else
                oriDate[k] = collectedDate;
            k++;
        }
                
        initialised = true;
    }
    
        @Override
    public void init(final PrintStream out) {
    	if (!initialised) {
    		initialise();
    	}
        out.print("logP(MRCATime(" + getID() + "))\t");
        
        for (final int i : taxonIndex) {
            out.print("MRCATime(" + tree.getTaxaNames()[i] + ")\t");
        }
    }
    
    @Override
    public void log(final long sample, final PrintStream out) {
        out.print(getCurrentLogP() + "\t");
        int k = 0;
        for (final int i : taxonIndex) {
            out.print(oriDate[k++] - tree.getNode(i).getDate() + "\t");
        }
    }
    
    @Override
    public int getDimension() {
        return 2;
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
    protected boolean requiresRecalculation() {
        return super.requiresRecalculation();
    }
    
    @Override
    public void close(final PrintStream out) {
        // nothing to do
    }

    @Override
    public List<String> getArguments() {
        return null;
    }

    @Override
    public List<String> getConditions() {
        return null;
    }

    @Override
    public void sample(State state, Random random) {
    }
    
}
