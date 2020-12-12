package beast.app.beauti;

import beast.core.BEASTInterface;
import beast.core.Input;
import beast.evolution.branchratemodel.MultiStrictClockModel;

/**
 *
 * @author Bradley R. Jones
 */

/*
public class MultiStrictClockInputEditor extends ClockModelListInputEditor {    
    public MultiStrictClockInputEditor(BeautiDoc doc) {
        super(doc);
    }

    @Override
    public Class<?> type() {
        return MultiStrictClockModel.class;
    }
    
    @Override
    public Class<?> baseType() {
    	// disable this editor
    	return MultiStrictClockInputEditor.class;
        //return BranchRateModel.Base.class;
    }
    
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, final int listItemNr, ExpandOption isExpandOption,
			boolean addButtons) {
        m_bAddButtons = addButtons;
        m_input = input;        
        m_beastObject = beastObject;
        this.itemNr= listItemNr;
        
        if (!doc.beautiConfig.suppressBEASTObjects.contains("beast.evolution.branchratemodel.MultiStrictClockModel.tree")) {
            doc.beautiConfig.suppressBEASTObjects.add("beast.evolution.branchratemodel.MultiStrictClockModel.tree");
        }        
        if (!doc.beautiConfig.suppressBEASTObjects.add("beast.evolution.branchratemodel.MultiStrictClockModel.typeSet")) {
            doc.beautiConfig.suppressBEASTObjects.add("beast.evolution.branchratemodel.MultiStrictClockModel.typeSet");
        }
        
        MultiStrictClockModel model = (MultiStrictClockModel)beastObject;
    }
}
*/