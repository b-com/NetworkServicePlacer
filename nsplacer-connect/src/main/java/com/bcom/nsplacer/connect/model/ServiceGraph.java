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
package com.bcom.nsplacer.connect.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class ServiceGraph {

    private String srcNode;

    private List<ServiceNode> nodes = new ArrayList<>();

    private List<ServiceLink> links = new ArrayList<>();

    private int latency = 0;

    private Map<String, ServiceNode> nodeMap = null;
    private Map<String, ServiceLink> linkMap = null;
    private List<ServiceNode> traversedNodes = null;

    public ServiceGraph clone() {
        ServiceGraph graph = new ServiceGraph();
        graph.srcNode = srcNode;
        for (int i = 0; i < nodes.size(); i++) {
            graph.getNodes().add(nodes.get(i).clone());
        }
        for (int i = 0; i < links.size(); i++) {
            graph.getLinks().add(links.get(i).clone());
        }
        return graph;
    }

    public List<ServiceNode> getTraversedNodes() {
        if (traversedNodes == null) {
            traversedNodes = new ArrayList<>();
            Set<String> bfsBag = new HashSet<>();
            Map<String, ServiceNode> map = new HashMap<>();
            for (int i = 0; i < nodes.size(); i++) {
                map.put(nodes.get(i).getLabel(), nodes.get(i));
            }
            traversedNodes.add(map.get(srcNode));
            bfsBag.add(srcNode);
            int i = 0;
            while (i < traversedNodes.size()) {
                String label = traversedNodes.get(i).getLabel();
                for (ServiceLink l : links) {
                    if (l.getSrcNode().equals(label)) {
                        ServiceNode child = map.get(l.getDstNode());
                        if (!bfsBag.contains(child.getLabel())) {
                            traversedNodes.add(child);
                            bfsBag.add(child.getLabel());
                        }
                    }
                }
                i++;
            }
        }
        return traversedNodes;
    }

    public Map<String, ServiceNode> getNodeMap() {
        if (nodeMap == null) {
            nodeMap = new HashMap<>();
            for (ServiceNode node : getNodes()) {
                nodeMap.put(node.getLabel(), node);
            }
        }
        return nodeMap;
    }

    public Map<String, ServiceLink> getLinkMap() {
        if (linkMap == null) {
            linkMap = new HashMap<>();
            for (ServiceLink link : getLinks()) {
                linkMap.put(link.getLabel(), link);
            }
        }
        return linkMap;
    }

}
