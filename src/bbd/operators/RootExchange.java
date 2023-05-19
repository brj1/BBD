/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bbd.operators;

import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.util.Randomizer;
import beast.base.evolution.operator.Exchange;

/**
 * Exchange operator for re-rooting a tree. Preservers the unrooted topology.
 * 
 * @author brad
 */
public class RootExchange extends Exchange {
    
    @Override
    public double narrow(final Tree tree) {

        final int internalNodes = tree.getInternalNodeCount();
        if (internalNodes <= 1) {
            return Double.NEGATIVE_INFINITY;
        }

        Node grandParent = tree.getRoot();

        Node parentIndex = grandParent.getLeft();
        Node uncle = grandParent.getRight();
        if (parentIndex.getHeight() < uncle.getHeight()) {
            parentIndex = grandParent.getRight();
            uncle = grandParent.getLeft();
        }

        if( parentIndex.isLeaf() ) {
            // tree with dated tips
            return Double.NEGATIVE_INFINITY;
        }

        final Node i = (Randomizer.nextBoolean() ? parentIndex.getLeft() : parentIndex.getRight());
        exchangeNodes(i, uncle, parentIndex, grandParent);

        return 0;
    }
    
    @Override
    public double wide(final Tree tree) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
