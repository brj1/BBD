/*
* File TipDatesMultiTreeOperator.java
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

import beast.base.core.Input;
import beast.base.inference.Operator;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.Tree;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author bjones
 */
public abstract class TipDatesMultiTreeOperator extends Operator {
    final public Input<List<Tree>> treesInput = new Input<>("trees", "list of beast.tree on which this operation is performed", new ArrayList<>());    
    final public Input<Boolean> scaleAllInput = new Input<>("scaleAll",
            "if true, all tips dates are scaled, otherwise one is randomly selected",
            false);
    final public Input<TaxonSet> m_taxonsetInput = new Input<>("taxonset", "limit scaling to a subset of taxa.", Input.Validate.REQUIRED);
 
    boolean scaleAll;
    
    /**
     * node indices of taxa to choose from *
     */
    int[][] taxonIndices;
    
    List<Tree> trees;
    
    @Override
    public void initAndValidate() {
        trees = treesInput.get();
        
        if (trees == null || trees.isEmpty()) {
            throw new IllegalArgumentException("Trees must be specificed.");
        }

        // determine taxon set to choose from
        if (m_taxonsetInput.get() != null) {
            List<String> taxaNames = new ArrayList<>();
            for (String taxon : trees.get(0).getTaxaNames()) {
                taxaNames.add(taxon);
            }

            List<String> set = m_taxonsetInput.get().asStringList();
            int nrOfTaxa = set.size();
            taxonIndices = new int[nrOfTaxa][trees.size()];
            int k = 0;
            for (String taxon : set) {
                int i = 0;
                for (Tree tree : trees) {
                    int taxonIndex = taxaNames.indexOf(taxon);
                    if (taxonIndex < 0) {
                        throw new IllegalArgumentException("Cannot find taxon " + taxon + " in tree " + tree.getID());
                    }
                    taxonIndices[k][i++] = taxonIndex;
                }
                
                k++;
            }
        } else {
            throw new IllegalArgumentException("Taxon set must be specificed.");
//            taxonIndices = new int[treeInput.get().getTaxaNames().length];
//            for (int i = 0; i < taxonIndices.length; i++) {
//                taxonIndices[k++][i] = i;
//            }
        }   
        
        if (scaleAllInput.get() != null) {
            scaleAll = scaleAllInput.get();
        } else {
            scaleAll = false;
        }
    }
    
}
