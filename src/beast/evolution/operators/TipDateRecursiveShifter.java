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
    
    private static List<Double> depthInfinity = new ArrayList<Double>() {{add(Double.NEGATIVE_INFINITY);}};

    public TipDateRecursiveShifter(double padding, double parentRange, double moveProb, boolean isScale) {
        this.padding = padding;
        this.moveProb = moveProb;
        this.isScale = isScale;

        if (isScale) {
            this.parentRange = parentRange - 1;
        } else {
            this.parentRange = parentRange;
        }
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

    private boolean checkRange(double parentHeight, double childHeight) {
        if (isScale) {
            return parentHeight - childHeight < parentRange * childHeight;
        } else {
            return parentHeight - childHeight < parentRange;
        }
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
                        if (isScale) {
                            newHeight = newChildHeight * (Randomizer.nextDouble() * parentRange + 1);
                        } else {
                            newHeight = newChildHeight + Randomizer.nextDouble() * parentRange;
                        }
                        
                        if (newHeight - newChildHeight < padding)
                            return depthInfinity;

                        depth.add(Math.log(parentRange * moveProb / (newChildHeight - item.childHeight)));
                        // don't move node
                    } else {
                        if (nodeHeight - newChildHeight < padding)
                            return depthInfinity;
                        
                        depth.add(Math.log(1 - moveProb));

                        continue;
                    }
                } else {
                    final double doMove = Randomizer.nextDouble();

                    if (doMove < moveProb) {
                        newHeight = Randomizer.nextDouble() * (item.childHeight - newChildHeight) + newChildHeight;
                        
                        if (newHeight - newChildHeight < padding)
                            return depthInfinity;
                        
                        depth.add(Math.log((item.childHeight - newChildHeight) / (moveProb * parentRange)));
                    } else {
                        if (nodeHeight - newChildHeight < padding)
                            return depthInfinity;
                        
                        depth.add(-Math.log(1 - moveProb));
                        
                        continue;
                    }
                }
            }

            final Node parent = item.node.getParent();

            if (parent != null) {
                final double parentHeight = parent.getHeight();
                
                // add parent mode
                if ((isIncreasing && checkRange(parentHeight, newHeight)) || (!isIncreasing && checkRange(parentHeight, nodeHeight))) {
                    int position = -1;

                    for (int i = 0; i < queue.size(); i++) {
                        NodeHeight currentItem = queue.get(i);

                        if (position < 0) {
                            if (parent == currentItem.node) {
                                queue.remove(i);
                                i--;
                            } else if (parentHeight < queue.get(i).node.getHeight()) {
                                position = i;
                            }
                        } else {
                            if (parent == currentItem.node) {
                                position = -2; // parent height better flag
                                                                
                                break;
                            }
                        }
                    }
                    
                    if (position == -1) {
                        position = queue.size();
                    }

                    if (position >= 0) {
                        queue.add(position, new NodeHeight(parent, maxChildHeight(parent), false));
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
