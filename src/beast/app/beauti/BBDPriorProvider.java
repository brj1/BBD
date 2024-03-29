/*
* File BBDPriorProvider.java
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
package beast.app.beauti;

import beast.app.draw.BEASTObjectPanel;
import beast.core.Distribution;
import beast.core.Logger;
import beast.core.State;
import beast.core.StateNode;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Tree;
import bbd.distributions.BBDPrior;
import beast.math.distributions.OneOnX;
import beast.math.distributions.ParametricDistribution;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;

/**
 * Tells BEAST 2 that the blind dating prior exists.
 * @author Bradley R. Jones
 */
public class BBDPriorProvider implements PriorProvider {
    BeautiDoc doc;
    
    Set<Taxon> getTaxonCandidates(BBDPrior prior) {
        Set<Taxon> candidates = new HashSet<>();
        Tree tree = prior.treeInput.get();
        String [] taxa = null;
        if (tree.m_taxonset.get() != null) {
            try {
                TaxonSet set = tree.m_taxonset.get();
                set.initAndValidate();
                taxa = set.asStringList().toArray(new String[0]);
            } catch (Exception e) {
                taxa = prior.treeInput.get().getTaxaNames();
            }
        } else {
            taxa = prior.treeInput.get().getTaxaNames();
        }
        
        for (String taxon : taxa) {
            candidates.add(doc.getTaxon(taxon));
        }       
        return candidates;
    }
    
    @Override
    public List<Distribution> createDistribution(BeautiDoc doc) {
        this.doc = doc;
        BBDPrior prior = new BBDPrior();
        try {
            List<Tree> trees = new ArrayList<>();
            this.doc.scrubAll(true, false);
            State state = (State) doc.pluginmap.get("state");
            for (StateNode node : state.stateNodeInput.get()) {
                if (node instanceof Tree) { // && ((Tree) node).m_initial.get() != null) {
                    trees.add((Tree) node);
                }
            }
            int treeIndex = 0;
            if (trees.size() > 1) {
                String[] treeIDs = new String[trees.size()];
                for (int j = 0; j < treeIDs.length; j++) {
                    treeIDs[j] = trees.get(j).getID();
                }
                treeIndex = JOptionPane.showOptionDialog(null, "Select a tree", " selector",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        treeIDs, trees.get(0));
            }
            if (treeIndex < 0) {
                return null;
            }
            prior.treeInput.setValue(trees.get(treeIndex), prior);
            TaxonSet taxonSet = new TaxonSet();
            TaxonSetDialog dlg = new TaxonSetDialog(taxonSet, getTaxonCandidates(prior), doc);
            if (!dlg.showDialog() || dlg.taxonSet.getID() == null || dlg.taxonSet.getID().trim().equals("")) {
                return null;
            }
            taxonSet = dlg.taxonSet;
            if (taxonSet.taxonsetInput.get().isEmpty()) {
            	JOptionPane.showMessageDialog(doc.beauti, "At least one taxon should be included in the taxon set",
            			"Error specifying taxon set", JOptionPane.ERROR_MESSAGE);
            	return null;
            }
            int i = 1;
            String id = taxonSet.getID();
            while (doc.pluginmap.containsKey(taxonSet.getID()) && doc.pluginmap.get(taxonSet.getID()) != taxonSet) {
            	taxonSet.setID(id + i);
            	i++;
            }
            BEASTObjectPanel.addPluginToMap(taxonSet, doc);
            prior.taxonsetInput.setValue(taxonSet, prior);
            prior.setID(taxonSet.getID() + ".prior");
            // this sets up the type
//            ParametricDistribution tmpDistr = (ParametricDistribution)(new OneOnX());
//            prior.distInput.setValue(tmpDistr, prior);
                        
            Logger logger = (Logger) doc.pluginmap.get("tracelog");
            logger.loggersInput.setValue(prior, logger);
        } catch (Exception e) {
            System.err.println(e);
	}
	List<Distribution> selectedPlugins = new ArrayList<>();
	selectedPlugins.add(prior);
        
//	g_collapsedIDs.add(prior.getID());
	return selectedPlugins;
    }
    
    @Override
    public String getDescription() {
	return "BBD prior";
    }
}
