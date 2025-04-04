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

import com.bcom.nsplacer.misc.CollectionUtils;
import com.bcom.nsplacer.misc.GraphUtils;
import com.bcom.nsplacer.misc.MathUtils;
import com.bcom.nsplacer.placement.enums.PlacementApproach;
import com.bcom.nsplacer.placement.enums.SearchStrategy;
import com.bcom.nsplacer.placement.routing.RoutingPath;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class SearchState implements Comparable<SearchState> {

    public static Random random = new Random(System.currentTimeMillis());
    private NetworkGraph networkGraph;
    private ServiceGraph serviceGraph;
    private Double objectiveValue = 0.0;
    private Integer depth;
    private List<ServiceNode> placedServiceNodes;
    private List<String> placedNetworkNodes;
    private List<ServiceLink> placedServiceLinks;
    private List<RoutingPath> placedPaths;
    private Placer placer;
    private SearchStrategy strategy;
    private int subgraphCount;
    private double fairnessCriteria;
    private PlacementApproach approach;

    public SearchState(NetworkGraph networkGraph, ServiceGraph serviceGraph, int depth, Placer placer, SearchStrategy strategy, PlacementApproach approach) {
        this.placer = placer;
        this.approach = approach;
        this.strategy = strategy;
        this.networkGraph = networkGraph;
        this.serviceGraph = serviceGraph;
        this.depth = depth;
        if (depth == 0) {
            serviceGraph.getTraversedNodes();
            serviceGraph.getTraversedLinks();
            //Collections.shuffle(serviceGraph.getTraversedNodes());
            placedServiceNodes = new ArrayList<>();
            placedNetworkNodes = new ArrayList<>();
            placedServiceLinks = new ArrayList<>();
            placedPaths = new ArrayList<>();
        }
    }

    private void generateIndices(List<List<Integer>> indicesList, List<List<RoutingPath>> paths, List<Integer> indices, int offset) {
        if (offset == paths.size()) {
            indicesList.add(indices);
        } else {
            for (int i = 0; i < paths.get(offset).size(); i++) {
                generateIndices(indicesList, paths, CollectionUtils.concat(new ArrayList<>(indices), i), offset + 1);
            }
        }
    }

    public void updateObjectiveValue() {
        if (SearchStrategy.DBO.equals(strategy)) {
            int sum = 0;
            for (RoutingPath path : placedPaths) {
                for (String link : path.getLinks()) {
                    sum += networkGraph.getLink(link).getLatency().getAmount();
                }
            }
            objectiveValue = (double) sum;
        } else if (strategy.isCompleteSearch()) {
            int unplacedBandwidth = 0;
            int usedBandwidth = 0;
            for (ServiceLink link : placer.getServiceGraph().getLinks()) {
                if (!placedServiceLinks.contains(link)) {
                    unplacedBandwidth += link.getBandwidth();
                }
            }
            for (int i = 0; i < placedServiceLinks.size(); i++) {
                List<String> links = placedPaths.get(i).getLinks();
                if ((links.size() == 1) && networkGraph.getLink(links.get(0)).isLoop()) {
                    // We suppose that loop paths does not consume bandwidth
                } else {
                    usedBandwidth += placedServiceLinks.get(i).getBandwidth() * links.size();
                }
            }
            if (SearchStrategy.UBO.equals(strategy)) {
                objectiveValue = (double) usedBandwidth;
            } else {
                objectiveValue = (double) usedBandwidth + unplacedBandwidth;
            }
        } else {
            objectiveValue = 0.0;
        }
    }

    public List<SearchState> expand() {
        if (placer.isShuffle()) {
            Collections.shuffle(approach.equals(PlacementApproach.NodeBased) ? networkGraph.getNodes() : networkGraph.getLinks());
        }
        if (SearchStrategy.EnhancedBF.equals(strategy)) {
            Collections.sort(networkGraph.getNodes(), (o1, o2) -> {
                double sum = 0;
                sum += ((double) o1.getCpu().getAmount() / o1.getCpu().getInitialAmount()) - ((double) o2.getCpu().getAmount() / o2.getCpu().getInitialAmount());
                return (int) Math.round(Math.signum(sum));
            });
        }

        List<SearchState> children = new ArrayList<>();
        if (approach.equals(PlacementApproach.NodeBased)) {
            ServiceNode placingServiceNode = serviceGraph.getTraversedNodes().get(depth);
            if (placingServiceNode.getNetworkNode() != null) {
                expandStateOverNode(networkGraph.getNode(placingServiceNode.getNetworkNode()), placingServiceNode, children);
            } else {
                for (NetworkNode placingNode : networkGraph.getNodes()) {
                    expandStateOverNode(placingNode, placingServiceNode, children);
                }
            }
        } else {
            ServiceLink placingServiceLink = serviceGraph.getTraversedLinks().get(depth);
            //Collections.sort(networkGraph.getLinks(), Comparator.comparingInt(o -> o.getBandwidth().getAmount()));
            List<RoutingPath> paths = generatePaths(placingServiceLink, networkGraph);
            for (RoutingPath placingPath : paths) {
                if (placingPath.canAccommodate(networkGraph, placingServiceLink)) {
                    expandStateOverLink(placingPath, placingServiceLink, children);
                }
            }
        }
        if (SearchStrategy.DBO.equals(strategy)) {
            Collections.sort(children);
        }
        if (!placer.isBacktracking()) {
            if (!children.isEmpty()) {
                SearchState lastChild = children.get(children.size() - 1);
                children.clear();
                children.add(lastChild);
            }
        }
        return children;
    }

    private List<RoutingPath> generatePaths(ServiceLink serviceLink, NetworkGraph networkGraph) {
        int srcNodeIndex = placedServiceNodes.indexOf(serviceGraph.getNodeMap().get(serviceLink.getSrcNode()));
        int dstNodeIndex = placedServiceNodes.indexOf(serviceGraph.getNodeMap().get(serviceLink.getDstNode()));
        boolean isSrcPlaced = srcNodeIndex >= 0;
        boolean isDstPlaced = dstNodeIndex >= 0;
        List<RoutingPath> paths = new ArrayList<>();
        if (isSrcPlaced && isDstPlaced) {
            String srcNode = placedNetworkNodes.get(srcNodeIndex);
            String dstNode = placedNetworkNodes.get(dstNodeIndex);
            paths.addAll(placer.getRoutingAlgorithm().route(this, srcNode, dstNode, serviceLink));
        } else if (isSrcPlaced) { // only source NF is placed
            String srcNode = placedNetworkNodes.get(srcNodeIndex);
            for (String dstNode : networkGraph.getNodeLabels()) {
                if (!dstNode.equals(srcNode)) {
                    List<RoutingPath> routes = placer.getRoutingAlgorithm().route(this, srcNode, dstNode, serviceLink);
                    if (!routes.isEmpty()) {
                        paths.addAll(routes);
                    }
                }
            }
        } else if (isDstPlaced) { // only destination NF is placed
            String dstNode = placedNetworkNodes.get(dstNodeIndex);
            for (String srcNode : networkGraph.getNodeLabels()) {
                if (!dstNode.equals(srcNode)) {
                    List<RoutingPath> routes = placer.getRoutingAlgorithm().route(this, srcNode, dstNode, serviceLink);
                    if (!routes.isEmpty()) {
                        paths.addAll(routes);
                    }
                }
            }
        } else { // Neither source nor destination is placed
            for (String srcNode : networkGraph.getNodeLabels()) {
                for (String dstNode : networkGraph.getNodeLabels()) {
                    if (!dstNode.equals(srcNode)) {
                        List<RoutingPath> routes = placer.getRoutingAlgorithm().route(this, srcNode, dstNode, serviceLink);
                        if (!routes.isEmpty()) {
                            paths.addAll(routes);
                        }
                    }
                }
            }
        }
        return paths;
    }

    private void expandStateOverNode(NetworkNode placingNode, ServiceNode placingServiceNode, List<SearchState> children) {
        NetworkGraph graph = networkGraph.clone(false);
        for (NetworkNode node : graph.getNodes()) {
            if (node.getLabel().equals(placingNode.getLabel())) {
                placingNode = node;
                break;
            }
        }

        if (placer.isVneConst() && placedNetworkNodes.contains(placingNode.getLabel())) {
            return;
        }

        if (!placingNode.canAccommodate(placingServiceNode)) {
            return;
        }

        SearchState child = new SearchState(graph, serviceGraph, depth + 1, placer, strategy, approach);
        child.setPlacedNetworkNodes(CollectionUtils.concat(new ArrayList<>(placedNetworkNodes), placingNode.getLabel()));
        child.setPlacedServiceNodes(CollectionUtils.concat(new ArrayList<>(placedServiceNodes), placingServiceNode));
        child.setPlacedServiceLinks(new ArrayList<>(placedServiceLinks));
        child.setPlacedPaths(new ArrayList<>(placedPaths));

        // Perform placement for the service node
        placingNode.getCpu().addAmount(-placingServiceNode.getCpu());
        placingNode.getStorage().addAmount(-placingServiceNode.getStorage());

        // Perform placement for the service links
        child.performLinkPlacements();

        // Update objective value
        if (child.isFeasible()) {
            child.updateObjectiveValue();
            child.setSubgraphCount(GraphUtils.getSubgraphCount(child.getNetworkGraph()));
            child.setFairnessCriteria();
            children.add(child);
        }
    }

    private void expandStateOverLink(RoutingPath placingPath, ServiceLink placingServiceLink, List<SearchState> children) {
        SearchState child = new SearchState(networkGraph.clone(false), serviceGraph, depth + 1, placer, strategy, approach);
        child.setPlacedServiceLinks(CollectionUtils.concat(new ArrayList<>(placedServiceLinks), placingServiceLink));
        child.setPlacedPaths(CollectionUtils.concat(new ArrayList<>(placedPaths), placingPath));
        child.setPlacedNetworkNodes(new ArrayList<>(placedNetworkNodes));
        child.setPlacedServiceNodes(new ArrayList<>(placedServiceNodes));

        placingPath.takeBandwidth(child.getNetworkGraph(), placingServiceLink.getBandwidth());

        ServiceNode srcServiceNode = serviceGraph.getNodeMap().get(placingServiceLink.getSrcNode());
        if (!placedServiceNodes.contains(srcServiceNode)) {
            NetworkNode srcNode = child.getNetworkGraph().getNode((placingPath.getSrcNode(networkGraph)));
            srcNode.getCpu().addAmount(-srcServiceNode.getCpu());
            srcNode.getStorage().addAmount(-srcServiceNode.getStorage());
            child.getPlacedNetworkNodes().add(srcNode.getLabel());
            child.getPlacedServiceNodes().add(srcServiceNode);
        }

        ServiceNode dstServiceNode = serviceGraph.getNodeMap().get(placingServiceLink.getDstNode());
        if (!placedServiceNodes.contains(dstServiceNode)) {
            NetworkNode dstNode = child.getNetworkGraph().getNode(placingPath.getDstNode(networkGraph));
            dstNode.getCpu().addAmount(-dstServiceNode.getCpu());
            dstNode.getStorage().addAmount(-dstServiceNode.getStorage());
            child.getPlacedNetworkNodes().add(dstNode.getLabel());
            child.getPlacedServiceNodes().add(dstServiceNode);
        }

        // Update objective value
        if (child.isFeasible()) {
            child.updateObjectiveValue();
            children.add(child);
        }
    }

    private void performLinkPlacements() {
        HashMap<String, Integer> placedSLMap = new HashMap<>();
        for (int i = 0; i < placedServiceLinks.size(); i++) {
            placedSLMap.put(placedServiceLinks.get(i).getLabel(), i);
        }
        HashMap<String, Integer> placedSNMap = new HashMap<>();
        for (int i = 0; i < placedServiceNodes.size(); i++) {
            placedSNMap.put(placedServiceNodes.get(i).getLabel(), i);
        }

        boolean sequentialPlacement = true;
        List<List<RoutingPath>> candidatePaths = new ArrayList<>();
        boolean routingFailed = false;
        for (ServiceLink vl : placer.getServiceGraph().getLinks()) {
            if (!placedSLMap.containsKey(vl.getLabel())) {
                if (placedSNMap.containsKey(vl.getSrcNode()) && placedSNMap.containsKey(vl.getDstNode())) {
                    String srcNode = placedNetworkNodes.get(placedSNMap.get(vl.getSrcNode()));
                    String dstNode = placedNetworkNodes.get(placedSNMap.get(vl.getDstNode()));

                    List<RoutingPath> routingPaths = placer.getRoutingAlgorithm().route(this, srcNode, dstNode, vl);

                    candidatePaths.add(routingPaths);

                    // For performance reasons, if we could not find any path for a VL
                    // This placement is not feasible and we can skip placing other VLs
                    if (routingPaths.isEmpty()) {
                        routingFailed = true;
                        placedServiceLinks.add(vl);
                        placedPaths.add(new RoutingPath());
                        break;
                    }

                    if (sequentialPlacement) {
                        RoutingPath path = routingPaths.get(0);
                        for (String linkLabel : path.getLinks()) {
                            NetworkLink link = networkGraph.getLink(linkLabel);
                            link.getBandwidth().addAmount(-vl.getBandwidth());
                        }
                        placedServiceLinks.add(vl);
                        placedPaths.add(path);
                    }
                }
            }
        }
    }

    public String getPlacementNodeMap() {
        List<ServiceNode> traversedNodes = serviceGraph.getTraversedNodes();
        List<List<String>> list = new ArrayList<>();
        for (int i = 0; i < traversedNodes.size(); i++) {
            List<String> l = new ArrayList<>();
            l.add(traversedNodes.get(i).getLabel());
            l.add(placedNetworkNodes.get(i));
            list.add(l);
        }
        Collections.sort(list, Comparator.comparing(o -> Integer.parseInt(o.get(0).substring(1))));
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < list.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append("" + list.get(i).get(0) + ":" + list.get(i).get(1));
        }
        sb.append("}");
        return sb.toString();
    }

    public String getPlacementLinkMap() {
        List<List<String>> list = new ArrayList<>();
        for (int i = 0; i < placedServiceLinks.size(); i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(placedPaths.get(i).getLinks().get(0));
            for (int j = 1; j < placedPaths.get(i).getLinks().size(); j++) {
                sb.append("-" + placedPaths.get(i).getLinks().get(j));
            }
            List<String> l = new ArrayList<>();
            l.add(placedServiceLinks.get(i).getLabel());
            l.add(sb.toString());
            list.add(l);
        }
        Collections.sort(list, Comparator.comparing(o -> Integer.parseInt(o.get(0).substring(1))));
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < list.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append("" + list.get(i).get(0) + ":" + list.get(i).get(1));
        }
        sb.append("}");
        return sb.toString();
    }

    public int calcPlacedLatency() {
        int graphSum = 0;
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < placedServiceLinks.size(); i++) {
            ServiceLink serviceLink = placedServiceLinks.get(i);
            if (serviceLink.getSrcNode().equals(serviceGraph.getSrcNode()) || serviceLink.getDstNode().equals(serviceGraph.getSrcNode())) {
                String otherServiceNodeLabel = serviceLink.getSrcNode().equals(serviceGraph.getSrcNode()) ? serviceLink.getDstNode() : serviceLink.getSrcNode();
                if (Integer.parseInt(otherServiceNodeLabel.substring(1)) <= serviceGraph.getUsers().length) {
                    Integer get = map.get(otherServiceNodeLabel);
                    if (get == null) {
                        get = 0;
                    }
                    get += placedPaths.get(i).getTotalLatency(networkGraph);
                    map.put(otherServiceNodeLabel, get);
                } else {
                    graphSum += placedPaths.get(i).getTotalLatency(networkGraph);
                }
            } else {
                graphSum += placedPaths.get(i).getTotalLatency(networkGraph);
            }
        }
        int max = 0;
        for (String key : map.keySet()) {
            max = Math.max(max, map.get(key));
        }
        return graphSum + max;
    }

    public void setFairnessCriteria() {
        if (strategy.isCompleteSearch()) {
            List<Double> list = new ArrayList<>();
            for (NetworkLink link : networkGraph.getLinks()) {
                if (!link.isLoop()) {
                    list.add((double) link.getBandwidth().getAmount());
                }
            }
            fairnessCriteria = MathUtils.variance(list);
        }
    }

    @Override
    public int compareTo(SearchState o) {
        int cmp = objectiveValue.compareTo(o.getObjectiveValue());
        if (strategy.equals(SearchStrategy.FairABO)) {
            // Compare f-costs
            cmp = -cmp;
            if (cmp != 0) {
                return cmp;
            }
            // Avoid defragmentation
            cmp = -(subgraphCount - o.subgraphCount);
            if (cmp != 0) {
                return cmp;
            }
            // Apply fairness
            cmp = -(int) Math.signum(fairnessCriteria - o.getFairnessCriteria());
            if (cmp != 0) {
                return cmp;
            }
            // Compare depths
            cmp = (depth - o.depth);
            if (cmp != 0) {
                return cmp;
            }
            return 0;
        } else if (strategy.equals(SearchStrategy.ABO) || strategy.equals(SearchStrategy.UBO)) {
            // Compare f-costs
            cmp = -cmp;
            if (cmp != 0) {
                return cmp;
            }
            // Avoid defragmentation
            cmp = -(subgraphCount - o.subgraphCount);
            if (cmp != 0) {
                return cmp;
            }
            // Compare depths
            cmp = (depth - o.depth);
            if (cmp != 0) {
                return cmp;
            }
            return 0;
        } else if (strategy.equals(SearchStrategy.DBO)) {
            // Compare costs
            cmp = -cmp;
            if (cmp != 0) {
                return cmp;
            }
            // Avoid defragmentation
            cmp = -(subgraphCount - o.subgraphCount);
            if (cmp != 0) {
                return cmp;
            }
            return 0;
        } else {
            return cmp;
        }
    }

    public boolean isFeasible() {
        // Check infeasibility for nodes
        for (int i = 0; i < networkGraph.getNodes().size(); i++) {
            if (!networkGraph.getNodes().get(i).checkFeasibility()) {
                return false;
            }
        }
        // Check infeasibility for links
        for (RoutingPath p : placedPaths) {
            if (p.getLinks() == null || p.getLinks().isEmpty()) {
                return false;
            }
        }
        // Check end-to-end latency
        int requiredLatency = placer.getServiceGraph().getLatency();
        int providedLatency = calcPlacedLatency();
        if (requiredLatency < providedLatency) {
            return false;
        }
        return true;
    }

    public boolean isTerminal() {
        return depth == (approach.equals(PlacementApproach.NodeBased) ?
                serviceGraph.getTraversedNodes().size() : serviceGraph.getTraversedLinks().size());
    }

    public void applyFoundPlacementResources() {
        for (NetworkNode node : networkGraph.getNodes()) {
            for (int i = 0; i < placedNetworkNodes.size(); i++) {
                if (node.getLabel().equals(placedNetworkNodes.get(i))) {
                    ServiceNode serviceNode = placedServiceNodes.get(i);
                    node.getCpu().addAmount(-serviceNode.getCpu());
                    node.getStorage().addAmount(-serviceNode.getStorage());
                }
            }
        }
        for (int i = 0; i < placedPaths.size(); i++) {
            RoutingPath path = placedPaths.get(i);
            ServiceLink serviceLink = placedServiceLinks.get(i);
            for (NetworkLink link : networkGraph.getLinks()) {
                if (path.getLinks().contains(link.getLabel())) {
                    link.getBandwidth().addAmount(-serviceLink.getBandwidth());
                }
            }
        }
    }
}
