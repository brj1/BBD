/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bbd.tipdate;

import beast.base.evolution.tree.Node;
import beast.base.util.Randomizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    private static final List<Double> depthInfinity = new ArrayList<Double>() {{add(Double.NEGATIVE_INFINITY);}};

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

    private double getMaxChildHeight(Node node) {
        double height = Double.MIN_VALUE;

        for (Node child : node.getChildren()) {
            if (child.getHeight() > height) {
                height = child.getHeight();
            }
        }

        return height;
    }
    
    private double getMaxNewChildHeight(Node node, Map<Node,NodeHeight> map) {
        double height = Double.MIN_VALUE;

        for (Node child : node.getChildren()) {
            NodeHeight childItem;
            
            if (map.containsKey(child)) {
                childItem = map.get(child);
            } else {
                childItem = new NodeHeight(child);
            }
            
            if (childItem.newHeight > height) {
                height = childItem.newHeight;
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
        Map<Node,NodeHeight> map = new HashMap<>();
        final boolean isIncreasing = (isScale && scale > 1) || (!isScale && scale > 0);

        nodeList.sort(comp);
        
        // iinitalize
        for (Node node : nodeList) {
            NodeHeight item = new NodeHeight(node);
            
            queue.add(item);
            map.put(node, item);
        }

        while (!queue.isEmpty()) {
            final NodeHeight item = queue.get(0);
            queue.remove(0);

            final double nodeHeight = item.node.getHeight();

            // move tip
            if (item.node.isLeaf()) {
                item.newHeight = isScale ? nodeHeight * scale : nodeHeight + scale;
            // move iternal node
            } else {
                final double newMaxChildHeight = getMaxNewChildHeight(item.node, map);
                final double maxChildHeight = getMaxChildHeight(item.node);

                if (isIncreasing) {
                    // move node up
                    if (nodeHeight < newMaxChildHeight) {
                        if (isScale) {
                            item.newHeight = newMaxChildHeight * (Randomizer.nextDouble() * parentRange + 1);
                        } else {
                            item.newHeight = newMaxChildHeight + Randomizer.nextDouble() * parentRange;
                        }
                        
                        if (item.newHeight - newMaxChildHeight < padding)
                            return depthInfinity;

                        depth.add(Math.log(parentRange * moveProb / (newMaxChildHeight - maxChildHeight)));
                    // don't move node
                    } else {
                        if (nodeHeight - newMaxChildHeight < padding)
                            return depthInfinity;
                        
                        depth.add(Math.log(1 - moveProb));

                        continue;
                    }
                } else {
                    final double doMove = Randomizer.nextDouble();

                    // move node down
                    if (doMove < moveProb) {
                        item.newHeight = Randomizer.nextDouble() * (maxChildHeight - newMaxChildHeight) + newMaxChildHeight;
                        
                        if (item.newHeight - newMaxChildHeight < padding)
                            return depthInfinity;
                        
                        depth.add(Math.log((maxChildHeight - newMaxChildHeight) / (moveProb * parentRange)));
                    // don't move node
                    } else {
                        if (item.newHeight - newMaxChildHeight < padding)
                            return depthInfinity;
                        
                        depth.add(-Math.log(1 - moveProb));
                        
                        continue;
                    }
                }
            }

            final Node parent = item.node.getParent();

            if (parent != null) {
                final double parentHeight = parent.getHeight();
                final double checkHeight = isIncreasing ? item.newHeight : nodeHeight;
                
                // add parent node
                if (!map.containsKey(parent) && checkRange(parentHeight, checkHeight)) {
                    int i;

                    for (i = 0; i < queue.size(); i++) {
                        if (parentHeight < queue.get(i).node.getHeight()) {
                            break;
                        }
                    }

                    if (i >= queue.size() || parent != queue.get(i).node) {
                        NodeHeight parentItem = new NodeHeight(parent);
                        
                        queue.add(i, parentItem);
                        map.put(parent, parentItem);
                    }
                }
            }
        }
        
        for (Node node : map.keySet()) {
            node.setHeight(map.get(node).newHeight);
        }

        return depth;
    }

    private class NodeHeight {

        public Node node;
        public double newHeight;

        public NodeHeight(Node node) {
            this.node = node;
            this.newHeight = node.getHeight();
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
