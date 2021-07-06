/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beast.evolution.operators;

import beast.evolution.tree.Node;
import beast.util.Randomizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author brad
 */
public class TipDateRecursiveShifter {

    double padding;
    double parentRange;
    double moveProb;
    double logMoveProp;
    boolean isScale;

    public TipDateRecursiveShifter(double padding, double parentRange, double moveProb, boolean isScale) {
        this.padding = padding;
        this.parentRange = parentRange;
        this.moveProb = moveProb;
        this.isScale = isScale;
    }

    private double maxChildHeight(Node node) {
        double height = Double.MIN_VALUE;

        for (Node child : node.getChildren()) {
            if (child.getHeight() > height) {
                height = child.getHeight();
            }
        }

        return height;
    }

    public List<Double> recursiveProposal(double newValue, Node node) {
        List<Double> depth = new ArrayList<>(0);
        final Node parent = node.getParent();

        if (parent != null) {
            final double parentHeight = parent.getHeight();

            // push parent node up
            if (parentHeight < newValue) {
                final double lower = maxChildHeight(parent);
                node.setHeight(newValue);
                final double upper = maxChildHeight(parent);

                final double shift = Randomizer.nextDouble() * parentRange;
                depth = recursiveProposal(newValue + shift, parent);

                depth.add(Math.log(parentRange * moveProb / (upper - lower)));
                // check if next parent is close
            } else if (node.getHeight() < newValue && (parentHeight - newValue < parentRange)) {
                final double lower = maxChildHeight(parent);
                node.setHeight(newValue);
                final double upper = maxChildHeight(parent);

                // edge case
                if (upper > lower) {
                    depth.add(Math.log(1 - moveProb));
                }

                // push close parent node down
            } else if (node.getHeight() > newValue && (parentHeight - node.getHeight() < parentRange)) {
                final double upper = maxChildHeight(parent);
                node.setHeight(newValue);
                final double lower = maxChildHeight(parent);

                // check edge case
                if (upper > lower) {
                    final double doMove = Randomizer.nextDouble();

                    if (doMove < moveProb) {
                        final double nextShift = Randomizer.nextDouble() * (upper - lower) + lower;

                        depth = recursiveProposal(nextShift, parent);

                        depth.add(Math.log((upper - lower) / (moveProb * parentRange)));
                    } else {
                        depth.add(-Math.log(1 - moveProb));
                    }
                }
            }
        } else {
            node.setHeight(newValue);
        }

        return depth;
    }

    public List<Double> recursiveProposalAll(double scale, List<Node> nodeList) {
        List<Double> depth = new ArrayList<>(0);
        final NodeHeightComp comp = new NodeHeightComp(true);
        List<NodeHeight> queue = new ArrayList<>(0);
        final boolean isIncreasing = (isScale && scale > 1) || (!isScale && scale > 0);

        nodeList.sort(comp);

        // iinitalize
        for (Node node : nodeList) {
            queue.add(new NodeHeight(node, 0, true));
        }

        while (!queue.isEmpty()) {
            final NodeHeight item = queue.get(0);
            queue.remove(0);

            final double nodeHeight = item.node.getHeight();
            double newHeight;

            // move tip
            if (item.isTip) {
                newHeight = isScale ? nodeHeight * scale : nodeHeight + scale;
                // move iternal node
            } else {
                final double newChildHeight = maxChildHeight(item.node);

                if (isIncreasing) {
                    // move node up
                    if (nodeHeight < newChildHeight) {
                        newHeight = newChildHeight + Randomizer.nextDouble() * parentRange;

                        depth.add(Math.log(parentRange * moveProb / (newChildHeight - item.childHeight)));
                        // don't move node
                    } else {
                        depth.add(Math.log(1 - moveProb));

                        continue;
                    }
                } else {
                    final double doMove = Randomizer.nextDouble();
                    
                    if (doMove < moveProb) {
                        newHeight = Randomizer.nextDouble() * (item.childHeight - newChildHeight) + newChildHeight;

                        depth.add(Math.log((item.childHeight - newChildHeight) / (moveProb * parentRange)));
                    } else {
                        depth.add(-Math.log(1 - moveProb));
                        
                        continue;
                    }
                }
            }

            final Node parent = item.node.getParent();

            if (parent != null) {
                final double parentHeight = parent.getHeight();
                
                // add parent mode
                if ((isIncreasing && parentHeight < newHeight + parentRange) || (!isIncreasing && parentHeight < nodeHeight + parentRange)) {
                    int i;

                    for (i = 0; i < queue.size(); i++) {
                        NodeHeight currentItem = queue.get(i);

                        if (parent == currentItem.node) {
                            break;
                        }
                        if (parentHeight < queue.get(i).node.getHeight()) {
                            break;
                        }
                    }

                    if (i >= queue.size() || parent != queue.get(i).node) {
                        queue.add(i, new NodeHeight(parent, maxChildHeight(parent), false));
                    }
                }
            }

            item.node.setHeight(newHeight);
        }

        return depth;
    }

    private class NodeHeight {

        public Node node;
        public double childHeight;
        public boolean isTip;

        public NodeHeight(Node node, double childHeight, boolean isTip) {
            this.node = node;
            this.childHeight = childHeight;
            this.isTip = isTip;
        }
    }

    private class NodeHeightComp implements Comparator<Node> {

        final private int ascendingFactor;

        public NodeHeightComp(boolean ascending) {
            ascendingFactor = ascending ? 1 : -1;
        }

        @Override
        public int compare(Node node1, Node node2) {
            return Double.compare(node1.getHeight(), node2.getHeight()) * ascendingFactor;
        }
    }
}
