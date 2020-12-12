package beast.evolution.branchratemodel;

import beast.core.Input;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.core.util.Log;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

/**
 * Ignores meanRateInput
 * @author Bradley R. Jones
 */
public class DualClockModel extends BranchRateModel.Base {
    public Input<RealParameter> rate2Input = 
            new Input<>("clock.rate2", "the clock rate for the second model.", Input.Validate.REQUIRED);
    public Input<IntegerParameter> indicatorsInput =
            new Input<>("indicators", "the branch model indicators.", Input.Validate.REQUIRED);
    final public Input<Tree> treeInput =
            new Input<>("tree", "the tree this relaxed clock is associated with.", Input.Validate.REQUIRED);
    
    private Tree m_tree;
    private RealParameter rate1;
    private RealParameter rate2;

    @Override
    public void initAndValidate() {
        m_tree = treeInput.get();
        IntegerParameter indicators = indicatorsInput.get();
        
        if (indicators.lowerValueInput.get() == null || indicators.lowerValueInput.get() != 0) {
            indicators.lowerValueInput.set(0);
        }        
        if (indicators.upperValueInput.get() == null || indicators.upperValueInput.get() != 1) {
            indicators.upperValueInput.set(1);
        }
        if (indicators.getDimension() != m_tree.getNodeCount()) {
            Log.warning.println("MuliClockModel::Setting dimension of indicators to " + (m_tree.getNodeCount()));
            indicators.setDimension(m_tree.getNodeCount());
        }
        
        if (meanRateInput.get() == null)
            meanRateInput.set(1);
        
        rate1 = meanRateInput.get();
        rate2 = rate2Input.get();
    }

    @Override
    public double getRateForBranch(Node node) {
        IntegerParameter indicators = indicatorsInput.get();
        int modelIndex = indicators.getValue(getNr(node));
        
        if (modelIndex == 0)
            return rate1.getValue();
        else
            return rate2.getValue();
    }
    
    private int getNr(Node node) {
        int nodeNr = node.getNr();
        if (nodeNr > m_tree.getRoot().getNr()) {
            nodeNr--;
        }
        return nodeNr;
    }
}
