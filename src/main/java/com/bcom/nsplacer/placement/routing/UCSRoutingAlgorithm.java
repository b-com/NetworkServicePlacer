/*
 * ===============================================================================
 * This file is part of Network Service Placer.
 *
 * Copyright 2021-2022 b<>com. All rights reserved.
 *
 * Network Service Placer is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Network Service Placer is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Network Service Placer. If not, see <https://www.gnu.org/licenses/>.
 * ===============================================================================
 */
package com.bcom.nsplacer.placement.routing;

import com.bcom.nsplacer.NsPlacerApplication;
import com.bcom.nsplacer.misc.CollectionUtils;
import com.bcom.nsplacer.misc.StreamUtils;
import com.bcom.nsplacer.placement.*;
import com.bcom.nsplacer.placement.enums.TopologyType;
import com.bcom.nsplacer.placement.evaluation.Evaluation;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.*;

public class UCSRoutingAlgorithm extends RoutingAlgorithm {

    private Map<String, Map<String, String>> linkNodeMap;

    public UCSRoutingAlgorithm(NetworkGraph graph) {
        linkNodeMap = new HashMap<>();
        for (NetworkLink link : graph.getLinks()) {
            if (!link.isLoop()) {
                Map<String, String> map = linkNodeMap.get(link.getSrcNode());
                if (map == null) {
                    map = new HashMap<>();
                }
                map.put(link.getDstNode(), link.getLabel());
                linkNodeMap.put(link.getSrcNode(), map);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String SNTopology = "BtEurope.graphml.xml";
        NetworkGraph networkGraph = ZooTopologyIO.fromXML(StreamUtils.readString(new File(NsPlacerApplication.zooTopologiesDir + "/" + SNTopology)), Evaluation.availableCpu, Evaluation.availableStorage, Evaluation.availableBandwidth, Evaluation.availableLatency);
        UCSRoutingAlgorithm routingAlgorithm = new UCSRoutingAlgorithm(networkGraph);
        ServiceGraph serviceGraph = new ServiceGraph();
        serviceGraph.create(TopologyType.DaisyChain, 3, null, Evaluation.requiredCpu, Evaluation.requiredStorage, Evaluation.requiredBandwidth, Evaluation.requiredLatency);
        SearchState state = new SearchState(networkGraph, serviceGraph, 0, null, null, null);
        ServiceLink vl = new ServiceLink();
        System.out.println(routingAlgorithm.route(state, "11", "4", vl));
    }

    @Override
    public List<RoutingPath> route(SearchState state, String srcNode, String dstNode, ServiceLink vl) {
        if (srcNode.equals(dstNode)) {
            for (NetworkLink link : state.getNetworkGraph().getLinks()) {
                if ((link.getBandwidth().getAmount() >= vl.getBandwidth())
                        && link.isLoop() && link.getSrcNode().equals(srcNode)) {
                    List<RoutingPath> paths = new ArrayList<>();
                    paths.add(new RoutingPath(CollectionUtils.concat(new ArrayList<>(), link.getLabel())));
                    return paths;
                }
            }
            return new ArrayList<>();
        }
        Set<String> visitedNodes = new HashSet<>();
        List<UCSNode> queue = new ArrayList<>();
        UCSNode root = new UCSNode();
        root.setNodeLabel(srcNode);
        queue.add(root);
        List<RoutingPath> foundPaths = new ArrayList<>();
        int foundDepth = -1;
        while (!queue.isEmpty()) {
            Collections.sort(queue);
            UCSNode node = queue.remove(queue.size() - 1);
            int nodeDepth = node.getDepth();
            if ((foundDepth != -1) && (nodeDepth > foundDepth)) {
                break;
            }
            if (node.getNodeLabel().equals(dstNode)) {
                if (foundDepth == -1) {
                    foundDepth = node.getDepth();
                }
                foundPaths.add(new RoutingPath(getLinks(node)));
            } else {
                if (visitedNodes.contains(node.nodeLabel)) {
                    continue;
                }
                visitedNodes.add(node.getNodeLabel());
                // Expansion
                if (linkNodeMap.get(node.getNodeLabel()) == null) {
                    // We have an unconnected graph
                    continue;
                }
                for (String dstNodeLabel : linkNodeMap.get(node.getNodeLabel()).keySet()) {
                    NetworkLink link = state.getNetworkGraph().getLink(linkNodeMap.get(node.getNodeLabel()).get(dstNodeLabel));
                    if (link.getBandwidth().getAmount() >= vl.getBandwidth()) {
                        UCSNode child = new UCSNode();
                        child.setNodeLabel(link.getDstNode());
                        child.setParent(node);
                        node.children.add(child);
                        child.setCost(node.getCost() + link.getLatency().getAmount());
                        if (!child.hasLoop()) {
                            queue.add(child);
                        }
                    }
                }
            }
        }
        return foundPaths;
    }

    public List<String> getLinks(UCSNode node) {
        List<String> path = new ArrayList<>();
        List<UCSNode> pathNodes = node.getPathFromRoot();
        for (int i = 0; i < pathNodes.size() - 1; i++) {
            path.add(linkNodeMap.get(pathNodes.get(i).getNodeLabel()).get(pathNodes.get(i + 1).getNodeLabel()));
        }
        return path;
    }

    @Setter
    @Getter
    private class UCSNode implements Comparable<UCSNode> {
        private UCSNode parent = null;
        private List<UCSNode> children = new ArrayList<>();
        private String nodeLabel = null;
        private Integer cost = 0;

        public List<UCSNode> getPathFromRoot() {
            List<UCSNode> path = getPathToRoot();
            Collections.reverse(path);
            return path;
        }

        public List<UCSNode> getPathToRoot() {
            List<UCSNode> path = new ArrayList<>();
            UCSNode node = this;
            path.add(node);
            while (node.getParent() != null) {
                path.add(node.getParent());
                node = node.getParent();
            }
            return path;
        }

        @Override
        public int compareTo(UCSNode o) {
            return -cost.compareTo(o.cost);
        }

        public boolean hasLoop() {
            List<UCSNode> pathToRoot = getPathToRoot();
            for (int i = 1; i < pathToRoot.size(); i++) {
                if (pathToRoot.get(i).getNodeLabel().equals(nodeLabel)) {
                    return true;
                }
            }
            return false;
        }

        public int getDepth() {
            return getPathToRoot().size() - 1;
        }
    }
}
