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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ZooTopologyIO {

    public static NetworkGraph fromXML(String xml, ParameterInitializer cpu, ParameterInitializer storage, ParameterInitializer bandwidth, ParameterInitializer latency) throws IOException {
        xml = xml.trim();
        if (!xml.contains("<graphml ")) {
            throw new IOException("Not a valid Zoo Topology XML file format!");
        }

        if (cpu == null) {
            cpu = new ParameterInitializer(false, 0, 0, 1000000);
        }
        if (storage == null) {
            storage = new ParameterInitializer(false, 0, 0, 1000000);
        }
        if (bandwidth == null) {
            bandwidth = new ParameterInitializer(false, 0, 0, 10);
        }
        if (latency == null) {
            latency = new ParameterInitializer(false, 0, 0, 1);
        }

        NetworkGraph ng = new NetworkGraph();
        int pointer = 0;
        int linkIndex = 1;
        while (true) {
            pointer = xml.indexOf("<node ", pointer + 1);
            if (pointer < 0) {
                break;
            }
            int p = xml.indexOf("id=\"", pointer);
            String label = xml.substring(xml.indexOf("\"", p) + 1,
                    xml.indexOf("\"", xml.indexOf("\"", p) + 1));
            NetworkNode node = new NetworkNode();
            node.getCpu().setInitialAmount(cpu.get());
            node.getCpu().setAmount(node.getCpu().getInitialAmount());
            node.getStorage().setInitialAmount(storage.get());
            node.getStorage().setAmount(node.getStorage().getInitialAmount());
            node.setLabel(label);
            ng.getNodes().add(node);

            NetworkLink link = new NetworkLink();
            link.setLabel("" + linkIndex);
            link.setSrcNode(node.getLabel());
            link.setDstNode(node.getLabel());
            link.getBandwidth().setInitialAmount(Integer.MAX_VALUE);
            link.getBandwidth().setAmount(Integer.MAX_VALUE);
            link.getLatency().setInitialAmount(0);
            link.getLatency().setAmount(0);
            ng.getLinks().add(link);
            linkIndex++;
        }

        int i = 0;
        pointer = 0;
        while (true) {
            pointer = xml.indexOf("<edge ", pointer + 1);
            if (pointer < 0) {
                break;
            }
            int p = xml.indexOf("source=\"", pointer);
            String source = xml.substring(xml.indexOf("\"", p) + 1,
                    xml.indexOf("\"", xml.indexOf("\"", p) + 1));
            p = xml.indexOf("target=\"", pointer);
            String target = xml.substring(xml.indexOf("\"", p) + 1,
                    xml.indexOf("\"", xml.indexOf("\"", p) + 1));
            NetworkLink link = new NetworkLink();
            link.setLabel("" + linkIndex);
            link.setSrcNode(source);
            link.setDstNode(target);
            link.getBandwidth().setInitialAmount(bandwidth.get());
            link.getBandwidth().setAmount(link.getBandwidth().getInitialAmount());
            link.getLatency().setInitialAmount(latency.get());
            link.getLatency().setAmount(link.getLatency().getInitialAmount());
            ng.getLinks().add(link);
            linkIndex++;

            NetworkLink clone = link.clone(false);
            clone.setLabel("" + linkIndex);
            clone.setSrcNode(link.getDstNode());
            clone.setDstNode(link.getSrcNode());
            clone.getBandwidth().setInitialAmount(bandwidth.get());
            clone.getBandwidth().setAmount(link.getBandwidth().getInitialAmount());
            clone.getLatency().setInitialAmount(latency.get());
            clone.getLatency().setAmount(link.getLatency().getInitialAmount());
            ng.getLinks().add(clone);
            linkIndex++;

            i++;
        }
        return ng;
    }

    public static String toXML(NetworkGraph graph) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?><graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">").append("\n");
        sb.append("<graph edgedefault=\"undirected\">").append("\n");
        for (NetworkNode node : graph.getNodes()) {
            sb.append("<node id=\"" + node.getLabel() + "\">").append("\n");
            sb.append("</node>").append("\n");
        }
        Set<String> bag = new HashSet<>();
        for (NetworkLink link : graph.getLinks()) {
            if (link.isLoop() || bag.contains(link.getDstNode() + "-" + link.getSrcNode())) {
                continue;
            }
            bag.add(link.getSrcNode() + "-" + link.getDstNode());
            sb.append("<edge source=\"" + link.getSrcNode() + "\" target=\"" + link.getDstNode() + "\">").append("\n");
            sb.append("</edge>").append("\n");
        }
        sb.append("</graph>").append("\n");
        sb.append("</graphml>").append("\n");
        return sb.toString();
    }
}
