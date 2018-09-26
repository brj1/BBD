package beast.evolution.branchratemodel;

import beast.core.Input;
import beast.core.parameter.IntegerParameter;
import beast.core.util.Log;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import java.util.ArrayList;

/**
 * Ignores meanRateInput
 * @author Bradley R. Jones
 */
public class MultiClockModel implements BranchRateModel {
    public Input<BranchRateModel> model1Input =
            new Input<>("clockModel1", "the first clock model.", Input.Validate.REQUIRED);
    public Input<BranchRateModel> model2Input =
            new Input<>("clockModel2", "the second clock model.", Input.Validate.REQUIRED);            
    public Input<IntegerParameter> indicatorsInput =
            new Input<>("indicators", "the branch model indicators.", Input.Validate.REQUIRED);
    final public Input<Tree> treeInput =
            new Input<>("tree", "the tree this relaxed clock is associated with.", Input.Validate.REQUIRED);
    
    private ArrayList<BranchRateModel> models = new ArrayList<>();
    private Tree m_tree;

    public void initAndValidate() {
        m_tree = treeInput.get();
        IntegerParameter indicators = indicatorsInput.get();
        
        if (indicators.lowerValueInput.get() == null || indicators.lowerValueInput.get() != 0) {
            indicators.lowerValueInput.set(0);
        }        
        if (indicators.upperValueInput.get() == null || indicators.upperValueInput.get() != 1) {
            indicators.upperValueInput.set(1);
        }
        if (indicators.getDimension() != m_tree.getNodeCount() - 1) {
            Log.warning.println("MuliClockModel::Setting dimension of indicators to " + (m_tree.getNodeCount() - 1));
            indicators.setDimension(m_tree.getNodeCount() - 1);
        }
        
        if (model1Input.get() == null) {
            model1Input.set(new StrictClockModel());
        }
        models.add(model1Input.get());
                
        if (model2Input.get() != null) {
            model2Input.set(new StrictClockModel());
        }        
        models.add(model2Input.get());
    }

    @Override
    public double getRateForBranch(Node node) {
        IntegerParameter indicators = indicatorsInput.get();
        int modelIndex = indicators.getValue(getNr(node));
        BranchRateModel model = models.get(modelIndex);
        
        return model.getRateForBranch(node);
    }
    
    private int getNr(Node node) {
        int nodeNr = node.getNr();
        if (nodeNr > m_tree.getRoot().getNr()) {
            nodeNr--;
        }
        return nodeNr;
    }
}
