/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beast.app.beauti;

/**
 *
 * @author Bradley R. Jones
 */
/*
public class MultiClockInputEditor extends ClockModelListInputEditor {
    public MultiClockInputEditor(BeautiDoc doc) {
        super(doc);
    }

    @Override
    public Class<?> type() {
        return MultiClockModel.class;
    }
    
    @Override
    public Class<?> baseType() {
    	// disable this editor
    	return MultiClockInputEditor.class;
        //return BranchRateModel.Base.class;
    }
    
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, final int listItemNr, ExpandOption isExpandOption,
			boolean addButtons) {
        m_bAddButtons = addButtons;
        m_input = input;        
        m_beastObject = beastObject;
        this.itemNr= listItemNr;
        
        if (!doc.beautiConfig.suppressBEASTObjects.contains("beast.evolution.branchratemodel.MultiClockModel.tree")) {
            doc.beautiConfig.suppressBEASTObjects.add("beast.evolution.branchratemodel.MultiClockModel.tree");
            doc.beautiConfig.suppressBEASTObjects.add("beast.evolution.branchratemodel.MultiClockModel.indicators");
            doc.beautiConfig.suppressBEASTObjects.add("beast.evolution.branchratemodel.MultiClockModel.clock.rate");
        }
        
        MultiClockModel model = (MultiClockModel)beastObject;
        
        String text = model.getID();
    }
}
*/