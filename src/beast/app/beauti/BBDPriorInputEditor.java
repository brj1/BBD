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
package beast.app.beauti;

import beast.app.draw.BEASTObjectPanel;
import beast.app.draw.InputEditor;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Tree;
import beast.math.distributions.BBDPrior;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.OneOnX;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import beast.app.draw.SmallButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import beast.core.util.Log;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.core.Operator;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Taxon;
import beast.evolution.operators.TipDatesRandomWalker;
import beast.evolution.operators.TipDatesRandomWalkerPadded;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Box;
import javax.swing.JComboBox;

/**
 * Input editor for blind dating prior.
 * @author Bradley R. Jones
 */
public class BBDPriorInputEditor extends InputEditor.Base {
    
    private boolean setDefault = true;
    
    public BBDPriorInputEditor(BeautiDoc doc) {
        super(doc);
    }
    
    @Override
    public Class<?> type() {
        return BBDPrior.class;
    }
    
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, final int listItemNr, ExpandOption isExpandOption,
			boolean addButtons) {
//        super.init(input, beastObject, itemNr, isExpandOption, addButtons);
        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr= listItemNr;
	
        if (!doc.beautiConfig.suppressBEASTObjects.contains("beast.math.distributions.BBDPrior.tree")) {
            doc.beautiConfig.suppressBEASTObjects.add("beast.math.distributions.BBDPrior.tree");
            doc.beautiConfig.suppressBEASTObjects.add("beast.math.distributions.BBDPrior.taxonset");
        }
        
        Box itemBox = Box.createHorizontalBox();

        BBDPrior prior = (BBDPrior) beastObject;
        String text = prior.getID();

        JButton taxonButton = new JButton(text);
//        taxonButton.setMinimumSize(Base.PREFERRED_SIZE);
//        taxonButton.setPreferredSize(Base.PREFERRED_SIZE);
        itemBox.add(taxonButton);
        taxonButton.addActionListener(e -> {
                List<?> list = (List<?>) m_input.get();
                BBDPrior prior2 = (BBDPrior) list.get(itemNr);
                try {
                    TaxonSet taxonset = prior2.taxonsetInput.get();
                    List<Taxon> originalTaxa = new ArrayList<>();
                    originalTaxa.addAll(taxonset.taxonsetInput.get());
                    Set<Taxon> candidates = getTaxonCandidates(prior2);
                    TaxonSetDialog dlg = new TaxonSetDialog(taxonset, candidates, doc);
                    if (dlg.showDialog()) {
        	            if (dlg.taxonSet.taxonsetInput.get().isEmpty()) {
        	            	JOptionPane.showMessageDialog(doc.beauti, "At least one taxon should be included in the taxon set",
        	            			"Error specifying taxon set", JOptionPane.ERROR_MESSAGE);
        	            	taxonset.taxonsetInput.get().addAll(originalTaxa);
        	            	return;
        	            }

                        prior2.taxonsetInput.setValue(dlg.taxonSet, prior2);
                        int i = 1;
                        String id = dlg.taxonSet.getID();
                        while (doc.pluginmap.containsKey(dlg.taxonSet.getID()) && doc.pluginmap.get(dlg.taxonSet.getID()) != dlg.taxonSet) {
                        	dlg.taxonSet.setID(id + i);
                        	i++;
                        }
                        BEASTObjectPanel.addPluginToMap(dlg.taxonSet, doc);
                        prior2.setID(dlg.taxonSet.getID() + ".prior");

                    }
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                refreshPanel();
            });

        if (prior.distInput.getType() == null) {
            try {
                prior.distInput.setValue(new OneOnX(), prior);
//                prior.distInput.setValue(null, prior);
            } catch (Exception e) {
                // TODO: handle exception
            }

        }

        List<BeautiSubTemplate> availableBEASTObjects = doc.getInputEditorFactory().getAvailableTemplates(prior.distInput, prior, null, doc);
        JComboBox<BeautiSubTemplate> comboBox = new JComboBox<>(availableBEASTObjects.toArray(new BeautiSubTemplate[]{}));
        comboBox.setName(text+".distr");

        if (prior.distInput.get() != null) {
            String id = prior.distInput.get().getID();
            //id = BeautiDoc.parsePartition(id);
            if (id == null)
                id = "";
            else
                id = id.substring(0, id.indexOf('.'));
            for (BeautiSubTemplate template : availableBEASTObjects) {
                if (template.classInput.get() != null && template.shortClassName.equals(id)) {
                    comboBox.setSelectedItem(template);
                }
            }
        } else {
            comboBox.setSelectedItem(BeautiConfig.NULL_TEMPLATE);
        }
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                @SuppressWarnings("unchecked")
		JComboBox<BeautiSubTemplate> comboBox = (JComboBox<BeautiSubTemplate>) e.getSource();
                BeautiSubTemplate template = (BeautiSubTemplate) comboBox.getSelectedItem();
                List<?> list = (List<?>) m_input.get();
                BBDPrior prior = (BBDPrior) list.get(itemNr);

//System.err.println("PRIOR" + beastObject2);
//            	try {
//					prior.m_distInput.setValue(beastObject2, prior);
//				} catch (Exception e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
                try {
                    //BEASTObject beastObject2 =
                    template.createSubNet(new PartitionContext(""), prior, prior.distInput, true);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                refreshPanel();
            }
        });
        itemBox.add(comboBox);

/*        JCheckBox isMonophyleticdBox = new JCheckBox(doc.beautiConfig.getInputLabel(prior, prior.isMonophyleticInput.getName()));
        isMonophyleticdBox.setName(text+".isMonophyletic");
        isMonophyleticdBox.setSelected(prior.isMonophyleticInput.get());
        isMonophyleticdBox.setToolTipText(prior.isMonophyleticInput.getHTMLTipText());
        isMonophyleticdBox.addActionListener(new MRCAPriorInputEditor.MRCAPriorActionListener(prior));
        itemBox.add(isMonophyleticdBox);*/

        JButton deleteButton = new SmallButton("-", true);
        deleteButton.setToolTipText("Delete this calibration");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Log.warning.println("Trying to delete a calibration");
                List<?> list = (List<?>) m_input.get();
                BBDPrior prior = (BBDPrior) list.get(itemNr);
                doc.disconnect(prior, "prior", "distribution");
                doc.disconnect(prior, "tracelog", "log");
                TipDatesRandomWalker operator = null;
                TaxonSet taxonset = prior.taxonsetInput.get();
                for (BEASTInterface o : taxonset.getOutputs()) {
                    if (o instanceof TipDatesRandomWalker) {
                        operator = (TipDatesRandomWalker) o;
                    }
                }
                if (operator == null) {
                    // should never happen
                    return;
                }
                
                // remove from list of operators
                Object o = doc.mcmc.get().getInput("operator");
                if (o instanceof Input<?>) {
                    Input<List<Operator>> operatorInput = (Input<List<Operator>>) o;
                    List<Operator> operators = operatorInput.get();
                    operators.remove(operator);
                }
                
                doc.unregisterPlugin(prior);
                refreshPanel();
            }
        });
        
        itemBox.add(Box.createGlue());
        itemBox.add(deleteButton);
        add(itemBox);

        if (prior.startingDateProbInput.get() == null)
            prior.startingDateProbInput.setValue(new RealParameter("0"), prior);
        
        // add operator
        TipDatesRandomWalker operator = null;
    	TaxonSet taxonset = prior.taxonsetInput.get();
    	taxonset.initAndValidate();
        
    	for (BEASTInterface o : taxonset.getOutputs()) {
            if (o instanceof TipDatesRandomWalker) {
                operator = (TipDatesRandomWalker) o;
            }
        }
    	
    	if (operator == null) {
            operator = new TipDatesRandomWalkerPadded();
            operator.initByName("tree", prior.treeInput.get(), "taxonset", taxonset, "windowSize", 1.0, "weight", 1.0);
            operator.setID("tipDatesSampler." + taxonset.getID());
            doc.mcmc.get().setInputValue("operator", operator);
    	}
        
        if (setDefault) {
            prior.isMonophyleticInput.setValue(false, prior);
            prior.onlyUseTipsInput.setValue(true, prior);
            prior.useOriginateInput.setValue(false, prior);
            setDefault = false;
        }
    }
    
    Set<Taxon> getTaxonCandidates(MRCAPrior prior) {
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
}
