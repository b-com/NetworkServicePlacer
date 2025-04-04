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
package com.bcom.nsplacer.misc;

import com.bcom.nsplacer.placement.NetworkGraph;
import com.bcom.nsplacer.placement.NetworkLink;
import com.bcom.nsplacer.placement.NetworkNode;
import com.bcom.nsplacer.placement.ZooTopologyIO;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class GraphUtils {

    public static int svgHeight = 1000, svgWidth = 1000;

    public static Map<String, Integer> getBetweenness(NetworkGraph graph) {
        Map<String, Integer> btness = new HashMap<>();
        Map<String, Map<String, String>> linkMap = new HashMap<>();
        for (NetworkLink link : graph.getLinks()) {
            if (!link.isLoop()) {
                btness.put(link.getLabel(), 0);
                Map<String, String> map = linkMap.get(link.getSrcNode());
                if (map == null) {
                    map = new HashMap<>();
                }
                map.put(link.getDstNode(), link.getLabel());
                linkMap.put(link.getSrcNode(), map);
            }
        }
        FWAlgorithm fWarshallAlgorithm = new FWAlgorithm(graph);
        for (NetworkNode n1 : graph.getNodes()) {
            for (NetworkNode n2 : graph.getNodes()) {
                if (n1.getLabel().equals(n2.getLabel())) {
                    continue;
                }
                List<String> path = fWarshallAlgorithm.getPathNodes(n1.getLabel(), n2.getLabel());
                for (int i = 0; i < path.size() - 1; i++) {
                    String linkLabel = linkMap.get(path.get(i)).get(path.get(i + 1));
                    btness.put(linkLabel, btness.get(linkLabel) + 1);
                }
            }
        }
        return btness;
    }

    public static Integer maximumRemainingBandwidth(List<NetworkLink> links) {
        int d = 0;
        for (NetworkLink link : links) {
            d = Math.max(d, link.getBandwidth().getAmount());
        }
        return d;
    }

    public static Integer minimumRemainingBandwidth(List<String> links, Map<String, NetworkLink> map) {
        int d = Integer.MAX_VALUE;
        for (String linkLabel : links) {
            d = Math.min(d, map.get(linkLabel).getBandwidth().getAmount());
        }
        return d;
    }

    public static Double averageRemainingBandwidth(List<NetworkLink> links) {
        double d = 0.0;
        for (NetworkLink link : links) {
            d += link.getBandwidth().getAmount();
        }
        d /= links.size();
        return d;
    }

    public static NetworkGraph createGridGraph(int rowCount, int columnCount) {
        NetworkGraph ng = new NetworkGraph();
        int linkIndex = 1;
        for (int i = 0; i < rowCount * columnCount; i++) {
            NetworkNode node = new NetworkNode();
            node.setLabel("" + i);
            ng.getNodes().add(node);

            NetworkLink link = new NetworkLink();
            link.setLabel("L" + linkIndex);
            link.setSrcNode(node.getLabel());
            link.setDstNode(node.getLabel());
            link.getBandwidth().setInitialAmount(Integer.MAX_VALUE);
            link.getBandwidth().setAmount(Integer.MAX_VALUE);
            link.getLatency().setInitialAmount(0);
            link.getLatency().setAmount(0);
            ng.getLinks().add(link);
            linkIndex++;
        }
        for (int i = 0; i < rowCount * columnCount; i++) {
            int row = i / columnCount;
            int column = i % columnCount;
            if (column != columnCount - 1) {
                NetworkLink link = new NetworkLink();
                link.setLabel("L" + linkIndex);
                link.setSrcNode("" + i);
                link.setDstNode("" + (i + 1));
                ng.getLinks().add(link);
                linkIndex++;
                NetworkLink clone = link.clone(false);
                clone.setLabel("L" + linkIndex);
                clone.setSrcNode(link.getDstNode());
                clone.setDstNode(link.getSrcNode());
                ng.getLinks().add(clone);
                linkIndex++;
            }
            if (row != rowCount - 1) {
                NetworkLink link = new NetworkLink();
                link.setLabel("L" + linkIndex);
                link.setSrcNode("" + i);
                link.setDstNode("" + (i + columnCount));
                ng.getLinks().add(link);
                linkIndex++;
                NetworkLink clone = link.clone(false);
                clone.setLabel("L" + linkIndex);
                clone.setSrcNode(link.getDstNode());
                clone.setDstNode(link.getSrcNode());
                ng.getLinks().add(clone);
                linkIndex++;
            }
        }

        return ng;
    }

    public static void main(String[] args) throws IOException {
        String zooTopologyDir = "./zoo-topologies/";
        ParameterInitializer requiredCpu = new ParameterInitializer(false, 0, 0, 1);
        ParameterInitializer requiredStorage = new ParameterInitializer(false, 0, 0, 1);
        ParameterInitializer requiredBandwidth = new ParameterInitializer(false, 0, 0, 1);
        ParameterInitializer requiredLatency = new ParameterInitializer(false, 0, 0, 10);
        ParameterInitializer availableCpu = new ParameterInitializer(false, 0, 0, 1000000);
        ParameterInitializer availableStorage = new ParameterInitializer(false, 0, 0, 1000000);
        ParameterInitializer availableBandwidth = new ParameterInitializer(false, 0, 0, 10);
        ParameterInitializer availableLatency = new ParameterInitializer(false, 0, 0, 1);

        String SNTopology = "BtEurope.graphml.xml";
        NetworkGraph graph = ZooTopologyIO.fromXML(StreamUtils.readString(new File(zooTopologyDir + SNTopology)), availableCpu, availableStorage, availableBandwidth, availableLatency);

        StreamUtils.writeString(ZooTopologyIO.toXML(createGridGraph(7, 6)), new File(zooTopologyDir + "Grid-7x6.xml"));
    }

    public static void improveLayout(NetworkGraph networkGraph) {
        AttractionRepulsionGraphLayoutAlgorithm alg = new AttractionRepulsionGraphLayoutAlgorithm(networkGraph, 1000, svgWidth, svgHeight);
        alg.run();
        scaleLayout(networkGraph);
    }

    public static String getLayout(NetworkGraph networkGraph) {
        StringBuilder sb = new StringBuilder();
        for (NetworkNode node : networkGraph.getNodes()) {
            sb.append(node.getLabel() + "," + node.getX() + "," + node.getY() + "\n");
        }
        return sb.toString();
    }

    public static void setRandomLayout(NetworkGraph networkGraph) {
        Random random = new Random(System.currentTimeMillis());
        for (NetworkNode node : networkGraph.getNodes()) {
            node.setX((int) (Math.abs(random.nextInt()) % (svgWidth * 0.9) + svgWidth * 0.05));
            node.setY((int) (Math.abs(random.nextInt()) % (svgWidth * 0.9) + svgWidth * 0.05));
        }
    }

    public static void setLayout(String layout, NetworkGraph networkGraph) {
        Scanner sc = new Scanner(layout);
        Map<String, NetworkNode> nodeMap = new HashMap<>();
        for (NetworkNode node : networkGraph.getNodes()) {
            nodeMap.put(node.getLabel(), node);
        }
        while (sc.hasNext()) {
            String split[] = sc.nextLine().trim().split(",");
            nodeMap.get(split[0]).setX(Integer.parseInt(split[1]));
            nodeMap.get(split[0]).setY(Integer.parseInt(split[2]));
        }
    }

    public static void scaleLayout(NetworkGraph graph) {
        int xMin = svgWidth, xMax = 0, yMin = svgHeight, yMax = 0;
        for (NetworkNode node : graph.getNodes()) {
            xMin = Math.min(xMin, node.getX());
            xMax = Math.max(xMax, node.getX());
            yMin = Math.min(yMin, node.getY());
            yMax = Math.max(yMax, node.getY());
        }
        double xScale = (double) svgWidth / (xMax - xMin) * 0.9;
        double yScale = (double) svgHeight / (yMax - yMin) * 0.9;
        for (NetworkNode node : graph.getNodes()) {
            Point p = new Point(node.getX(), node.getY());
            node.setX(p.x + (svgWidth / 2 - (xMax + xMin) / 2));
            node.setY(p.y + (svgHeight / 2 - (yMax + yMin) / 2));
            node.setX((int) ((node.getX() - svgWidth / 2) * xScale + svgWidth / 2));
            node.setY((int) ((node.getY() - svgHeight / 2) * yScale + svgHeight / 2));
        }
    }

    public static void rotateLayout(NetworkGraph graph) {
        for (NetworkNode node : graph.getNodes()) {
            Point p = new Point(node.getX(), node.getY());
            Point rotate = GeometryUtils.rotate(p.x, p.y, svgWidth / 2, svgHeight / 2, Math.PI / 8.0);
            node.setX(rotate.x);
            node.setY(rotate.y);
        }
    }

    public static int getSubgraphCount(NetworkGraph graph) {
        int sum = 0;
        Set<String> visited = new HashSet<>();
        Set<String> subgraph = new HashSet<>();
        for (NetworkNode node : graph.getNodes()) {
            if (!visited.contains(node.getLabel())) {
                subgraph.clear();
                subgraph.add(node.getLabel());
                traverseSubgraph(node.getLabel(), graph, subgraph);
                if (subgraph.size() > 1) {
                    visited.addAll(subgraph);
                    sum++;
                }
            }
        }
        return sum;
    }

    private static void traverseSubgraph(String node, NetworkGraph graph, Set<String> subgraph) {
        for (NetworkLink link : graph.getLinks()) {
            if (!link.isLoop() && (link.getBandwidth().getAmount() > 0)
                    && link.getSrcNode().equals(node) && !subgraph.contains(link.getDstNode())) {
                subgraph.add(link.getDstNode());
                traverseSubgraph(link.getDstNode(), graph, subgraph);
            }
        }
    }
}
