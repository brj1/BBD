package beast.evolution.branchratemodel;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.MultiTypeNode;
import beast.evolution.tree.Node;
import beast.evolution.tree.MultiTypeTree;
import beast.evolution.tree.TypeSet;
import java.util.Arrays;

/**
  * @author Bradley R Jones
 */

@Description("Defines a rate for each branch  in the beast.tree.")
public class MultiStrictClockModel extends BranchRateModel.Base {
    final public Input<MultiTypeTree> treeInput = new Input<>("tree", "the tree this clock is associated with.", Input.Validate.REQUIRED);
    final public Input<TypeSet> typeSetInput = new Input<>("typeSet", "the type set.");
    
    RealParameter muParameter;
    MultiTypeTree tree;
    
    private int nTypes = 1;
    private TypeSet typeSet;
    private boolean recompute = true;
    private int branchCount;
    private double[] rates;
    private double[] storedRates;
    
    @Override
    public void initAndValidate() {
        tree = treeInput.get();
        branchCount = tree.getNodeCount() - 1;        
        muParameter = meanRateInput.get();
        typeSet = typeSetInput.get();
        
        if (typeSet != null) {
            nTypes = typeSet.getNTypes();
        }
                
        if (muParameter != null) {
            muParameter.setBounds(Math.max(0.0, muParameter.getLower()), muParameter.getUpper());
            if (muParameter.getDimension() < nTypes) {
                throw new RuntimeException("Clock Model categories less than number of traits.");
            }
        }
                
        if (rates == null) {
            rates = new double[branchCount];
        }
    }

    @Override
    public double getRateForBranch(Node node) {
        if (recompute) {
            synchronized (this) {
                for (int i = 0; i < branchCount; i++) {
                    rates[i] = computeRateForBranch((MultiTypeNode)tree.getNode(i));
                }
                recompute = false;
            }
        }
        
        return computeRawRates(node);
    }
    
    private double computeRawRates(Node node) {
        int nodeNumber = node.getNr();
        if (nodeNumber == branchCount) {
            nodeNumber = 0;
        }
        
        return rates[nodeNumber];
    }
    
    private double computeRateForBranch(MultiTypeNode multiNode) {
        int changeCount = multiNode.getChangeCount();
        double rate = 0;
        int type = multiNode.getNodeType();
        
        if (changeCount > 0) {
            double changeTime = multiNode.getHeight();
            
            for (int change = 0; change < changeCount; change++) {
                double nextChangeTime = multiNode.getChangeTime(change);
                
                rate += muParameter.getValue(type) * (nextChangeTime - changeTime); // TODO: catch exception
                
                type = multiNode.getChangeType(change);
                changeTime = nextChangeTime;
            }
        
            rate += muParameter.getValue(type) * (multiNode.getHeight() + multiNode.getLength() - changeTime);  // TODO: catch exception
                      
            rate /= multiNode.getLength();
        } else {
            rate = muParameter.getValue(type);  // TODO: catch exception
        }
        
        if (rate < 0) {
            System.out.println("rate less than zero: " + rate + " (" + multiNode.getNr() + ")");
        }
        
        return rate;
    }
    
    @Override
    protected boolean requiresRecalculation() {
        recompute = false;
        
        if (treeInput.get().somethingIsDirty()) {
            recompute = true;
            return true;
        }
        
        if (muParameter.somethingIsDirty()) {
            recompute = true;
            return true;
        }

        return recompute;
    }
    
        @Override
    public void store() {
        if (rates != null) {
            if (storedRates == null)
                storedRates = new double[rates.length];
           System.arraycopy(rates, 0, storedRates, 0, rates.length);
        }
        super.store();
    }

    @Override
    public void restore() {
        if (rates != null) {
            double[] tmp = rates;
            rates = storedRates;
            storedRates = tmp;
        }
        super.restore();
    }
}
