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
public class NetworkGraph {

    private List<NetworkNode> nodes = new ArrayList<>();

    private List<NetworkLink> links = new ArrayList<>();

    @JsonIgnore
    public NetworkNode getNode(String label) {
        for (NetworkNode n : nodes) {
            if (label.equals(n.getLabel())) {
                return n;
            }
        }
        return null;
    }

    @JsonIgnore
    public List<String> getNodeLabels() {
        List<String> list = new ArrayList<>();
        for (NetworkNode n : nodes) {
            list.add(n.getLabel());
        }
        return list;
    }

    @JsonIgnore
    public List<String> getLinkLabels() {
        List<String> list = new ArrayList<>();
        for (NetworkLink l : links) {
            list.add(l.getLabel());
        }
        return list;
    }

    @JsonIgnore
    public NetworkLink getLink(String label) {
        for (NetworkLink l : links) {
            if (label.equals(l.getLabel())) {
                return l;
            }
        }
        return null;
    }

    @JsonIgnore
    public NetworkLink getLink(String src, String dst) {
        for (NetworkLink l : links) {
            if (src.equals(l.getSrcNode()) && dst.equals(l.getDstNode())) {
                return l;
            }
        }
        return null;
    }

    public NetworkGraph clone(boolean initialize) {
        NetworkGraph graph = new NetworkGraph();
        for (int i = 0; i < nodes.size(); i++) {
            graph.getNodes().add(nodes.get(i).clone(initialize));
        }
        for (int i = 0; i < links.size(); i++) {
            graph.getLinks().add(links.get(i).clone(initialize));
        }
        return graph;
    }

    @JsonIgnore
    public int getTotalRemainingCpu() {
        int sum = 0;
        for (NetworkNode node : getNodes()) {
            sum += node.getCpu().getAmount();
        }
        return sum;
    }

    @JsonIgnore
    public int getTotalMaximumCpu() {
        int sum = 0;
        for (NetworkNode node : getNodes()) {
            sum += node.getCpu().getInitialAmount();
        }
        return sum;
    }

    @JsonIgnore
    public int getTotalUsedCpu() {
        return getTotalMaximumCpu() - getTotalRemainingCpu();
    }

    @JsonIgnore
    public int getTotalUsedStorage() {
        return getTotalMaximumStorage() - getTotalRemainingStorage();
    }

    @JsonIgnore
    public int getTotalUsedBandwidth() {
        return getTotalMaximumBandwidth() - getTotalRemainingBandwidth();
    }

    @JsonIgnore
    public int getTotalRemainingStorage() {
        int sum = 0;
        for (NetworkNode node : getNodes()) {
            sum += node.getStorage().getAmount();
        }
        return sum;
    }

    @JsonIgnore
    public int getTotalMaximumStorage() {
        int sum = 0;
        for (NetworkNode node : getNodes()) {
            sum += node.getStorage().getInitialAmount();
        }
        return sum;
    }

    @JsonIgnore
    public int getTotalRemainingBandwidth() {
        int sum = 0;
        for (NetworkLink link : getLinks()) {
            if (!link.isLoop()) {
                sum += link.getBandwidth().getAmount();
            }
        }
        return sum;
    }

    @JsonIgnore
    public int getTotalMaximumBandwidth() {
        int sum = 0;
        for (NetworkLink link : getLinks()) {
            if (!link.isLoop()) {
                sum += link.getBandwidth().getInitialAmount();
            }
        }
        return sum;
    }

    public List<Integer> listRemainingBandwidth() {
        Collections.sort(getLinks(), Comparator.comparingInt(o -> o.getBandwidth().getAmount()));
        List<Integer> list = new ArrayList<>();
        for (NetworkLink l : getLinks()) {
            if (!l.isLoop()) {
                list.add(l.getBandwidth().getAmount());
            }
        }
        return list;
    }

    public Map<String, Integer> mapOfAggregatedRemainingBandwidth() {
        Map<String, Integer> map = new HashMap<>();
        for (NetworkLink l : getLinks()) {
            if (!l.isLoop()) {
                int rb = l.getBandwidth().getAmount();
                Integer nb = map.get(l.getSrcNode());
                map.put(l.getSrcNode(), (nb == null) ? rb : nb + rb);
            }
        }
        return map;
    }

    public List<Integer> listOfAggregatedRemainingBandwidth() {
        Map<String, Integer> map = mapOfAggregatedRemainingBandwidth();
        List<Integer> list = new ArrayList<>();
        for (Object k : map.keySet()) {
            list.add(map.get(k));
        }
        Collections.sort(list);
        return list;
    }

}
