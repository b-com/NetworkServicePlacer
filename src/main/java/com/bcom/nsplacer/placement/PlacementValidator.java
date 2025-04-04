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

import java.util.*;

public class PlacementValidator {

    public static boolean validate(NetworkGraph networkGraph, ServiceGraph serviceGraph, String placementListDescription) {
        networkGraph = networkGraph.clone(true);
        Map<String, NetworkNode> nodeMap = new HashMap<>();
        for (NetworkNode node : networkGraph.getNodes()) {
            nodeMap.put(node.getLabel(), node);
        }
        Map<String, NetworkLink> linkMap = new HashMap<>();
        for (NetworkLink link : networkGraph.getLinks()) {
            linkMap.put(link.getLabel(), link);
        }
        Scanner sc = new Scanner(placementListDescription);
        while (sc.hasNext()) {
            String line = sc.nextLine().trim();
            if (line.equals("")) {
                continue;
            }
            int n = Integer.parseInt(line.substring(line.indexOf("#") + 1));
            Map<String, String> placementNodesMap = parseNodes(sc.nextLine());
            Map<String, List<String>> placementLinksMap = parseLinks(sc.nextLine());
            if (placementNodesMap.keySet().size() != serviceGraph.getNodes().size()) {
                return false;
            }
            for (String serviceNodeLabel : placementNodesMap.keySet()) {
                ServiceNode serviceNode = serviceGraph.getNodeMap().get(serviceNodeLabel);
                NetworkNode node = nodeMap.get(placementNodesMap.get(serviceNodeLabel));
                node.getCpu().addAmount(-serviceNode.getCpu());
                node.getStorage().addAmount(-serviceNode.getStorage());
                if (!node.getCpu().isFeasible() || !node.getStorage().isFeasible()) {
                    return false;
                }
            }

            if (placementLinksMap.keySet().size() != serviceGraph.getLinks().size()) {
                return false;
            }

            for (String serviceLinkLabel : placementLinksMap.keySet()) {
                ServiceLink serviceLink = serviceGraph.getLinkMap().get(serviceLinkLabel);
                List<String> linksLabelList = placementLinksMap.get(serviceLinkLabel);
                for (int i = 0; i < linksLabelList.size(); i++) {
                    String linkLabel = linksLabelList.get(i);
                    NetworkLink link = linkMap.get(linkLabel);
                    link.getBandwidth().addAmount(-serviceLink.getBandwidth());
                    if (!link.getBandwidth().isFeasible()) {
                        return false;
                    }
                    if (i == 0) {
                        if (!link.getSrcNode().equals(placementNodesMap.get(serviceLink.getSrcNode()))) {
                            return false;
                        }
                    } else {
                        if (!link.getSrcNode().equals(linkMap.get(linksLabelList.get(i - 1)).getDstNode())) {
                            return false;
                        }
                    }
                    if (i == (linksLabelList.size() - 1)) {
                        if (!link.getDstNode().equals(placementNodesMap.get(serviceLink.getDstNode()))) {
                            return false;
                        }
                    } else {
                        if (!link.getDstNode().equals(linkMap.get(linksLabelList.get(i + 1)).getSrcNode())) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private static Map<String, List<String>> parseLinks(String line) {
        line = line.substring(line.indexOf("{") + 1, line.lastIndexOf("}"));
        line = line.replace(" ", "");
        Map<String, List<String>> map = new HashMap<>();

        for (String s : line.split(",")) {
            int ind = s.indexOf("=");
            map.put(s.substring(0, ind), Arrays.asList(s.substring(ind + 1).split("-")));
        }

        return map;
    }

    private static Map<String, String> parseNodes(String line) {
        line = line.substring(line.indexOf("{") + 1, line.lastIndexOf("}"));
        line = line.replace(" ", "");
        Map<String, String> map = new HashMap<>();
        for (String s : line.split(",")) {
            int ind = s.indexOf("=");
            map.put(s.substring(0, ind), s.substring(ind + 1));
        }
        return map;
    }
}
