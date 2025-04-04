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

import com.bcom.nsplacer.misc.CollectionUtils;
import com.bcom.nsplacer.placement.*;

import java.util.*;

public class DijkstraRoutingAlgorithm extends RoutingAlgorithm {

    private Map<String, Map<String, String>> linkNodeMap;
    private List<RoutingPath> foundPaths;

    public DijkstraRoutingAlgorithm(NetworkGraph graph) {
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
        Map<String, Long> distMap = new HashMap<>();
        Map<String, String> prevMap = new HashMap<>();
        List<String> queue = new ArrayList<>();
        for (NetworkNode node : state.getNetworkGraph().getNodes()) {
            distMap.put(node.getLabel(), (long) Integer.MAX_VALUE);
            queue.add(node.getLabel());
        }
        Comparator<String> comp = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return -Long.compare(distMap.get(o1), distMap.get(o2));
            }
        };
        distMap.put(srcNode, 0l);
        while (!queue.isEmpty()) {
            Collections.sort(queue, comp);
            String nodeLabel = queue.remove(queue.size() - 1);
            if (linkNodeMap.get(nodeLabel) == null) {
                // We have an unconnected graph
                continue;
            }
            for (String dstNodeLabel : linkNodeMap.get(nodeLabel).keySet()) {
                NetworkLink link = state.getNetworkGraph().getLink(linkNodeMap.get(nodeLabel).get(dstNodeLabel));
                if (link.getBandwidth().getAmount() >= vl.getBandwidth()) {
                    long alt = distMap.get(nodeLabel) + (long) link.getLatency().getAmount();
                    if (alt < distMap.get(link.getDstNode())) {
                        distMap.put(link.getDstNode(), alt);
                        prevMap.put(link.getDstNode(), nodeLabel);
                    }
                }
            }
        }
        if (prevMap.get(dstNode) == null) {
            return new ArrayList<>();
        }
        foundPaths = new ArrayList<>();
        foundPaths.add(new RoutingPath(getPath(prevMap, srcNode, dstNode)));
        return foundPaths;
    }

    private List<String> getPath(Map<String, String> prevMap, String srcNode, String dstNode) {
        List<String> nodeList = new ArrayList<>();
        nodeList.add(dstNode);
        while (!dstNode.equals(srcNode)) {
            nodeList.add(prevMap.get(dstNode));
            dstNode = prevMap.get(dstNode);
        }
        Collections.reverse(nodeList);
        List<String> linkList = new ArrayList<>();
        for (int i = 0; i < nodeList.size() - 1; i++) {
            linkList.add((linkNodeMap.get(nodeList.get(i))).get(nodeList.get(i + 1)));
        }
        return linkList;
    }
}
