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

import java.util.*;

/**
 * Implementation of the Floyd-Warshall Algorithm.
 *
 * @author <a href="sven@happycoders.eu">Sven Woltmann</a>
 */
public class FWAlgorithm {

    public static final int INFINITY = 1000000000;
    private int maxCost;
    private int[][] costs;
    private int[][] successors;
    private int num;
    private String[] nodes;
    private Map<String, Integer> nodeNameToIndex;
    private Map<String, NetworkLink> linkMap;
    private Map<String, Map<String, String>> linkNodeMap;
    private NetworkGraph graph;

    public FWAlgorithm(NetworkGraph graph) {
        this.graph = graph;
        init();
        update();
    }

    public int getMaxCost() {
        return maxCost;
    }

    public NetworkGraph getGraph() {
        return graph;
    }

    private void init() {
        String[] nodesLabels = graph.getNodes().stream().map(x -> x.getLabel()).toArray(String[]::new);
        num = nodesLabels.length;
        nodes = nodesLabels;

        // Create lookup map from node name to node index
        Map<String, Integer> temp = new HashMap<>();
        for (int i = 0; i < num; i++) {
            temp.put(nodes[i], i);
        }
        this.nodeNameToIndex = Collections.unmodifiableMap(temp);

        // Create cost and successor matrix
        this.costs = new int[num][num];
        this.successors = new int[num][num];

        linkMap = new HashMap<>();
        for (NetworkLink link : graph.getLinks()) {
            linkMap.put(link.getLabel(), link);
        }
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

    private void update() {
        // Preparation
        for (NetworkLink link : graph.getLinks()) {
            linkMap.put(link.getLabel(), link);
        }
        for (int i = 0; i < num; i++) {
            for (int j = 0; j < num; j++) {
                if (i == j) {
                    costs[i][j] = 0;
                    successors[i][j] = -1;
                } else {
                    NetworkLink targetLink = null;
                    try {
                        targetLink = linkMap.get(linkNodeMap.get(nodes[i]).get(nodes[j]));
                    } catch (NullPointerException np) {
                    }
                    if (targetLink == null) {
                        costs[i][j] = INFINITY;
                        successors[i][j] = -1;
                    } else {
                        costs[i][j] = 1;
                        successors[i][j] = j;
                    }
                }
            }
        }
        // Iterations
        maxCost = 0;
        for (int k = 0; k < num; k++) {
            for (int i = 0; i < num; i++) {
                for (int j = 0; j < num; j++) {
                    int costViaNodeK = addCosts(costs[i][k], costs[k][j]);
                    if (costViaNodeK < costs[i][j]) {
                        costs[i][j] = costViaNodeK;
                        successors[i][j] = successors[i][k];
                    }
                    if (k == num - 1) {
                        maxCost = (costs[i][j] != INFINITY) ? Math.max(maxCost, costs[i][j]) : maxCost;
                    }
                }
            }
        }
        // Detect negative cycles
        for (int i = 0; i < num; i++) {
            if (costs[i][i] < 0) {
                throw new IllegalArgumentException("Graph has a negative cycle");
            }
        }
    }

    private int addCosts(int a, int b) {
        if (a == INFINITY || b == INFINITY) {
            return INFINITY;
        }
        return a + b;
    }

    public int getDist(String source, String dest) {
        return costs[nodeNameToIndex.get(source)][nodeNameToIndex.get(dest)];
    }

    public List<String> getPathNodes(String source, String dest) {
        int i = nodeNameToIndex.get(source);
        int j = nodeNameToIndex.get(dest);
        if (successors[i][j] == -1) {
            return new ArrayList<>();
        }
        List<String> path = new ArrayList<>();
        path.add(nodes[i]);
        while (i != j) {
            i = successors[i][j];
            path.add(nodes[i]);
        }
        return path;
    }

    public List<String> getPathLinks(String source, String dest) {
        List<String> path = new ArrayList<>();
        List<String> pathNodes = getPathNodes(source, dest);
        for (int i = 0; i < pathNodes.size() - 1; i++) {
            path.add(linkNodeMap.get(pathNodes.get(i)).get(pathNodes.get(i + 1)));
        }
        return path;
    }

    public void print() {
        printCosts();
        printSuccessors();
    }

    private void printCosts() {
        System.out.println("Costs:");
        printHeader();
        for (int rowNo = 0; rowNo < num; rowNo++) {
            System.out.printf("%5s", nodes[rowNo]);
            for (int colNo = 0; colNo < num; colNo++) {
                int cost = costs[rowNo][colNo];
                if (cost == INFINITY) System.out.print("    ∞");
                else System.out.printf("%5d", cost);
            }
            System.out.println();
        }
    }

    private void printSuccessors() {
        System.out.println("Successors:");
        printHeader();
        for (int rowNo = 0; rowNo < num; rowNo++) {
            System.out.printf("%5s", nodes[rowNo]);
            for (int colNo = 0; colNo < num; colNo++) {
                int successor = successors[rowNo][colNo];
                String nextNode = successor != -1 ? nodes[successor] : "-";
                System.out.printf("%5s", nextNode);
            }
            System.out.println();
        }
    }

    private void printHeader() {
        System.out.print("     ");
        for (int colNo = 0; colNo < num; colNo++) {
            System.out.printf("%5s", nodes[colNo]);
        }
        System.out.println();
    }

}