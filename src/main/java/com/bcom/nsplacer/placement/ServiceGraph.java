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
package com.bcom.nsplacer.placement;

import com.bcom.nsplacer.misc.ParameterInitializer;
import com.bcom.nsplacer.placement.enums.TopologyType;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
    private Map<String, ServiceNode> nodeMap = null;
    @JsonIgnore
    private Map<String, ServiceLink> linkMap = null;
    @JsonIgnore
    private List<ServiceNode> traversedNodes = null;
    @JsonIgnore
    private List<ServiceLink> traversedLinks = null;
    @JsonIgnore
    private String users[] = new String[0];

    @JsonIgnore
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

    @JsonIgnore
    public List<ServiceLink> getTraversedLinks() {
        if (traversedLinks == null) {
            traversedLinks = new ArrayList<>();
            List<ServiceNode> bfsList = new ArrayList<>();
            Set<String> bfsBag = new HashSet<>();
            Map<String, ServiceNode> nodeMap = new HashMap<>();
            for (int i = 0; i < nodes.size(); i++) {
                nodeMap.put(nodes.get(i).getLabel(), nodes.get(i));
            }
            bfsList.add(nodeMap.get(srcNode));
            bfsBag.add(srcNode);
            int i = 0;
            while (i < bfsList.size()) {
                String label = bfsList.get(i).getLabel();
                for (ServiceLink l : links) {
                    if (l.getSrcNode().equals(label)) {
                        traversedLinks.add(l);
                        ServiceNode child = nodeMap.get(l.getDstNode());
                        if (!bfsBag.contains(child.getLabel())) {
                            bfsList.add(child);
                            bfsBag.add(child.getLabel());
                        }
                    }
                }
                i++;
            }
        }
        return traversedLinks;
    }

    public ServiceGraph clone() {
        ServiceGraph graph = new ServiceGraph();
        graph.srcNode = srcNode;
        graph.latency = latency;
        for (int i = 0; i < nodes.size(); i++) {
            graph.getNodes().add(nodes.get(i).clone());
        }
        for (int i = 0; i < links.size(); i++) {
            graph.getLinks().add(links.get(i).clone());
        }
        graph.setUsers(new String[users.length]);
        System.arraycopy(users, 0, graph.getUsers(), 0, users.length);
        return graph;
    }

    @JsonIgnore
    public Map<String, ServiceNode> getNodeMap() {
        if (nodeMap == null) {
            nodeMap = new HashMap<>();
            for (ServiceNode node : getNodes()) {
                nodeMap.put(node.getLabel(), node);
            }
        }
        return nodeMap;
    }

    @JsonIgnore
    public Map<String, ServiceLink> getLinkMap() {
        if (linkMap == null) {
            linkMap = new HashMap<>();
            for (ServiceLink link : getLinks()) {
                linkMap.put(link.getLabel(), link);
            }
        }
        return linkMap;
    }

    public void create(TopologyType topologyType, int serviceSize, String usersStr, ParameterInitializer cpu, ParameterInitializer storage, ParameterInitializer bandwidth, ParameterInitializer latency) {
        nodeMap = null;
        traversedLinks = null;
        traversedNodes = null;
        nodes.clear();
        links.clear();

        int userCount = (usersStr != null) && !usersStr.trim().equals("") ? usersStr.trim().split(",").length : 0;
        String users[] = (userCount != 0) ? usersStr.trim().split(",") : new String[0];
        for (int i = 0; i < users.length; i++) {
            users[i] = users[i].trim();
        }
        setUsers(users);
        setSrcNode("N" + (userCount + 1));
        setLatency(latency.get());

        for (int i = 0; i < serviceSize + userCount; i++) {
            ServiceNode v = new ServiceNode();
            v.setLabel("N" + (i + 1));
            getNodes().add(v);
            if (i < userCount) {
                v.setCpu(0);
                v.setStorage(0);
                v.setNetworkNode(users[i]);
            } else {
                v.setCpu(cpu.get());
                v.setStorage(storage.get());
            }
        }

        int linkIndex = 1;
        for (int i = 1; i <= userCount; i++) {
            ServiceLink l = new ServiceLink();
            l.setLabel("L" + linkIndex);
            l.setSrcNode("N" + i);
            l.setDstNode(getSrcNode());
            l.setBandwidth(bandwidth.get());
            getLinks().add(l);
            linkIndex++;

            ServiceLink clone = l.clone();
            clone.setLabel("L" + linkIndex);
            clone.setSrcNode(l.getDstNode());
            clone.setDstNode(l.getSrcNode());
            clone.setBandwidth(bandwidth.get());
            getLinks().add(clone);
            linkIndex++;
        }

        if (TopologyType.DaisyChain.equals(topologyType)) {
            for (int i = userCount + 1; i < serviceSize + userCount; i++) {
                ServiceLink l = new ServiceLink();
                l.setLabel("L" + linkIndex);
                l.setSrcNode("N" + i);
                l.setDstNode("N" + (i + 1));
                l.setBandwidth(bandwidth.get());
                getLinks().add(l);
                linkIndex++;

                ServiceLink clone = l.clone();
                clone.setLabel("L" + linkIndex);
                clone.setSrcNode(l.getDstNode());
                clone.setDstNode(l.getSrcNode());
                clone.setBandwidth(bandwidth.get());
                getLinks().add(clone);
                linkIndex++;
            }
        } else if (TopologyType.Ring.equals(topologyType)) {
            for (int i = userCount + 1; i <= serviceSize + userCount; i++) {
                ServiceLink l = new ServiceLink();
                l.setLabel("L" + linkIndex);
                l.setSrcNode("N" + i);
                l.setDstNode("N" + ((i == serviceSize + userCount) ? (userCount + 1) : (i + 1)));
                l.setBandwidth(bandwidth.get());
                getLinks().add(l);
                linkIndex++;

                ServiceLink clone = l.clone();
                clone.setLabel("L" + linkIndex);
                clone.setSrcNode(l.getDstNode());
                clone.setDstNode(l.getSrcNode());
                clone.setBandwidth(bandwidth.get());
                getLinks().add(clone);
                linkIndex++;
            }
        } else if (TopologyType.Star.equals(topologyType)) {
            for (int i = userCount + 1; i < serviceSize + userCount; i++) {
                ServiceLink l = new ServiceLink();
                l.setLabel("L" + linkIndex);
                l.setSrcNode("N" + (userCount + 1));
                l.setDstNode("N" + (i + 1));
                l.setBandwidth(bandwidth.get());
                getLinks().add(l);
                linkIndex++;

                ServiceLink clone = l.clone();
                clone.setLabel("L" + linkIndex);
                clone.setSrcNode(l.getDstNode());
                clone.setDstNode(l.getSrcNode());
                clone.setBandwidth(bandwidth.get());
                getLinks().add(clone);
                linkIndex++;
            }
        }
    }
}
