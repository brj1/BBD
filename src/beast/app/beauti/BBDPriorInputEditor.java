/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beast.app.beauti;

import beast.app.draw.BEASTObjectPanel;
import beast.app.draw.BEASTObjectInputEditor;
import beast.core.Distribution;
import beast.core.Logger;
import beast.core.State;
import beast.core.StateNode;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Tree;
import beast.math.distributions.BBDPrior;
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
import beast.evolution.alignment.Taxon;
import beast.math.distributions.ParametricDistribution;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author bjones
 */
public class BBDPriorInputEditor extends BEASTObjectInputEditor implements PriorProvider {
    @Override
    public Class<?> type() {
        return BBDPriorInputEditor.class;
    }
	

    public BBDPriorInputEditor() {
        super(null);
    }
    
    public BBDPriorInputEditor(BeautiDoc doc) {
        super(doc);
    }
    
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption,
			boolean addButtons) {
        super.init(input, beastObject, itemNr, isExpandOption, addButtons);
        
        System.out.println("please run this");
        
        JButton deleteButton = new SmallButton("-", true);
        deleteButton.setToolTipText("Delete this BBD Prior");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Log.warning.println("Trying to delete a BBD Prior");
                List<?> list = (List<?>) m_input.get();
                BBDPrior prior = (BBDPrior) list.get(itemNr);
                doc.disconnect(prior, "prior", "distribution");
                doc.unregisterPlugin(prior);
                refreshPanel();
            }        	
        });
        add(deleteButton);
    }
    
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
            getDoc().scrubAll(true, false);
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
            prior.distInput.setValue((ParametricDistribution)(new OneOnX()), prior);
                        
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
