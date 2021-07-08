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
import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;


/**
 * Blind dating prior.
 * @author Bradley R. Jones
 */
@Description("Blind dating prior")
public class BBDPrior extends Distribution {
    public final Input<RealParameter> startingDateProbInput = new Input<>("collectedDateProbability", "the probability that the collected date is correct");
//    public final Input<Double> startingDateDifferenceInput = new Input<>("startDateDifference", "the starting value");
    public final Input<Double> collectedDateInput = new Input<>("collectedDate", "the collected date");
    public final Input<Tree> treeInput = new Input<>("tree", "the tree containing the taxon set", Input.Validate.REQUIRED);
    public final Input<TaxonSet> taxonsetInput = new Input<>("taxonset",
            "set of taxa for which prior information is available");
    public final Input<Boolean> isMonophyleticInput = new Input<>("monophyletic",
            "whether the taxon set is monophyletic (forms a clade without other taxa) or nor. Default is false.", false);
    public final Input<ParametricDistribution> distInput = new Input<>("distr",
            "distribution used to calculate prior over MRCA time, "
                    + "e.g. normal, beta, gamma. If not specified, monophyletic must be true");
    public final Input<Boolean> onlyUseTipsInput = new Input<>("tipsonly",
            "flag to indicate tip dates are to be used instead of the MRCA node. " +
                    "If set to true, the prior is applied to the height of all tips in the taxonset " +
                    "and the monophyletic flag is ignored. Default is false.", true);
    public final Input<Boolean> useOriginateInput = new Input<>("useOriginate", "Use parent of clade instead of clade. Cannot be used with tipsonly, or on the root.", false);
    
    
    double[] oriDate;
    double startingDateProb;
    double collectedDate;
//    double startingDateDifference;

        /**
     * shadow members *
     */
    ParametricDistribution dist;
    Tree tree;
    // number of taxa in taxon set
    int nrOfTaxa = -1;
    // array of flags to indicate which taxa are in the set
    Set<String> isInTaxaSet = new LinkedHashSet<>();

    // array of indices of taxa
    int[] taxonIndex;
    // stores time to be calculated
    double MRCATime = -1;
    double storedMRCATime = -1;
    // flag indicating taxon set is monophyletic
    boolean isMonophyletic = false;
    boolean onlyUseTips = false;
    boolean useRoot = false;
    boolean useOriginate = false;
    
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
        List<String> set = null;
        if (taxonsetInput.get() != null) {
            set = taxonsetInput.get().asStringList();
            nrOfTaxa = set.size();
        } else {
            // assume all taxa
            nrOfTaxa = taxaNames.size();
        }

        onlyUseTips = onlyUseTipsInput.get();
        useOriginate = useOriginateInput.get();
        if (nrOfTaxa == 1) {
            // ignore test for Monophyletic when it only involves a tree tip
        	if (!useOriginate && !onlyUseTips) {
        		onlyUseTips = true;
        	}
        }
        if (!onlyUseTips && !useOriginate && nrOfTaxa < 2) {
            throw new IllegalArgumentException("At least two taxa are required in a taxon set");
        }
        if (!onlyUseTips && taxonsetInput.get() == null) {
            throw new IllegalArgumentException("Taxonset must be specified OR tipsonly be set to true");
        }
        
       
        if (useOriginate && onlyUseTips) {
        	throw new IllegalArgumentException("'useOriginate' and 'tipsOnly' cannot be both true");
        }
        useRoot = nrOfTaxa == tree.getLeafNodeCount();
        if (useOriginate && useRoot) {
        	throw new IllegalArgumentException("Cannot use originate of root. You can set useOriginate to false to fix this");
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
       
        if (startingDateProb > 1 || startingDateProb < 0)
            throw new IllegalArgumentException("Starting Date Probability must be betweem 0 and 1");
        
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
            case 1:
                return MRCATime;
            default:
                return 0;
        }
    }
    
    boolean [] nodesTraversed;
    int nseen;

    protected Node getCommonAncestor(Node n1, Node n2) {
        // assert n1.getTree() == n2.getTree();
        if( ! nodesTraversed[n1.getNr()] ) {
            nodesTraversed[n1.getNr()] = true;
            nseen += 1;
        }
        if( ! nodesTraversed[n2.getNr()] ) {
            nodesTraversed[n2.getNr()] = true;
            nseen += 1;
        }
        while (n1 != n2) {
	        double h1 = n1.getHeight();
	        double h2 = n2.getHeight();
	        if ( h1 < h2 ) {
	            n1 = n1.getParent();
	            if( ! nodesTraversed[n1.getNr()] ) {
	                nodesTraversed[n1.getNr()] = true;
	                nseen += 1;
	            }
	        } else if( h2 < h1 ) {
	            n2 = n2.getParent();
	            if( ! nodesTraversed[n2.getNr()] ) {
	                nodesTraversed[n2.getNr()] = true;
	                nseen += 1;
	            }
	        } else {
	            //zero length branches hell
	            Node n;
	            double b1 = n1.getLength();
	            double b2 = n2.getLength();
	            if( b1 > 0 ) {
	                n = n2;
	            } else { // b1 == 0
	                if( b2 > 0 ) {
	                    n = n1;
	                } else {
	                    // both 0
	                    n = n1;
	                    while( n != null && n != n2 ) {
	                        n = n.getParent();
	                    }
	                    if( n == n2 ) {
	                        // n2 is an ancestor of n1
	                        n = n1;
	                    } else {
	                        // always safe to advance n2
	                        n = n2;
	                    }
	                }
	            }
	            if( n == n1 ) {
                    n = n1 = n.getParent();
                } else {
                    n = n2 = n.getParent();
                }
	            if( ! nodesTraversed[n.getNr()] ) {
	                nodesTraversed[n.getNr()] = true;
	                nseen += 1;
	            } 
	        }
        }
        return n1;
    }
    
    public Node getCommonAncestor() {
        if (!initialised) {
            initialise();
        }
        nodesTraversed = new boolean[tree.getNodeCount()];
        Node n = getCommonAncestorInternal();
        assert ! (useRoot && !n.isRoot() ) ;
        return n;
    }

    private Node getCommonAncestorInternal() {
        Node cur = tree.getNode(taxonIndex[0]);

        for (int k = 1; k < taxonIndex.length; ++k) {
            cur = getCommonAncestor(cur, tree.getNode(taxonIndex[k]));
        }
        return cur;
    }
    
    @Override
    public void store() {
        storedMRCATime = MRCATime;
        // don't need to store m_bIsMonophyletic since it is never reported
        // explicitly, only logP and MRCA time are (re)stored
        super.store();
    }

    @Override
    public void restore() {
        MRCATime = storedMRCATime;
        super.restore();
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
