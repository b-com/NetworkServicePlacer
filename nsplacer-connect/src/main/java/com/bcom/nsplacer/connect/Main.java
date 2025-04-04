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
package com.bcom.nsplacer.connect;

import com.bcom.nsplacer.connect.model.*;
import com.bcom.nsplacer.connect.placement.RoutingAlgorithm;
import com.bcom.nsplacer.connect.placement.RoutingPath;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.google.common.primitives.Primitives;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class Main {

    public static WebSocketClient websocket;
    public static WebSocketSession websocketSession;
    private static ObjectMapper mapper;

    public static synchronized void send(WebSocketSession wss, WebSocketMessage<?> wsMsg) {
        try {
            wss.sendMessage(wsMsg);
        } catch (Exception e) {
        }
    }

    public static TextMessage createMessage(Object obj) {
        try {
            return (Primitives.isWrapperType(obj.getClass()) || (obj instanceof String)) ?
                    new TextMessage("" + obj) :
                    new TextMessage(mapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        Properties config = readConfig(args);

        mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        websocket = new StandardWebSocketClient();
        System.out.println("Trying to connect to " + config.get("wsAddress") + "...");
        websocket.doHandshake(new AbstractWebSocketHandler() {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                websocketSession = session;
                System.out.println("Connection established!");
                send(session, createMessage("Placer"));
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                String payloadString = null;
                byte payloadBytes[] = null;
                if (message instanceof TextMessage) {
                    payloadString = ((TextMessage) message).getPayload();
                } else {
                    payloadBytes = ((BinaryMessage) message).getPayload().array();
                }

                if ("Pong".equals(payloadString)) {
                    return;
                }

                if (payloadString != null) {
                    CustomPlacerRequest request = mapper.readValue(payloadString, CustomPlacerRequest.class);

                    System.out.println("Received placement request! (" + mapper.writeValueAsString(request.getParams()) + ")");
                    System.out.println("  Trying to place...");

                    CustomPlacerResponse response = place(request);
                    if (response.isSucceeded()) {
                        System.out.println("  Placement finished! Placer could place the service successfully!");
                    } else {
                        System.out.println("  Placement finished! Placer failed to place the service!");
                    }
                    send(session, createMessage(response));
                }
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                System.out.println("Connection closed!");
            }
        }, "" + config.get("wsAddress"));

        while (true) {
            Thread.sleep(10000);
            send(websocketSession, createMessage("Ping"));
        }
    }

    /**
     * This is a sample placement algorithm, that places randomly the VNFs and uses a shortest path routing algorithms for placing VLs.
     * You need to implement your algorithm here!
     *
     * @param request
     */
    public static CustomPlacerResponse place(CustomPlacerRequest request) {
        CustomPlacerResponse response = new CustomPlacerResponse();

        response.setPlacedServiceNodes(request.getService().getTraversedNodes());

        List<NetworkNode> allNodes = new ArrayList<>(request.getNetwork().getNodes());
        Collections.shuffle(allNodes);
        response.setPlacedNetworkNodes(new ArrayList<>());
        for (ServiceNode sn : response.getPlacedServiceNodes()) {
            boolean placed = false;
            for (int i = 0; i < allNodes.size(); i++) {
                NetworkNode n = allNodes.get(i);
                if (n.canAccommodate(sn)) {
                    response.getPlacedNetworkNodes().add(n.getLabel());
                    placed = true;
                    allNodes.remove(n); // We place VNF of a service over different network nodes
                    break;
                }
            }
            if (!placed) {
                response.setSucceeded(false);
                return response;
            }
        }
        response.setPlacedServiceLinks(request.getService().getLinks());
        response.setPlacedPaths(new ArrayList<>());
        RoutingAlgorithm routingAlgorithm = new RoutingAlgorithm(request.getNetwork());
        for (ServiceLink sl : response.getPlacedServiceLinks()) {
            String srcNode = null, dstNode = null;
            for (int i = 0; i < response.getPlacedServiceNodes().size(); i++) {
                if (sl.getSrcNode().equals(response.getPlacedServiceNodes().get(i).getLabel())) {
                    srcNode = response.getPlacedNetworkNodes().get(i);
                }
                if (sl.getDstNode().equals(response.getPlacedServiceNodes().get(i).getLabel())) {
                    dstNode = response.getPlacedNetworkNodes().get(i);
                }
            }
            RoutingPath route = routingAlgorithm.route(srcNode, dstNode, sl);
            if (route == null) {
                response.setSucceeded(false);
                return response;
            }
            response.getPlacedPaths().add(route);
        }
        response.setSucceeded(true);
        return response;
    }

    private static Properties readConfig(String[] args) {
        Properties config = new Properties();
        if (args.length > 0) {
            config.put("wsAddress", args[0]);
        } else {
            config.put("wsAddress", "ws://localhost:8080/ws");
        }
        return config;
    }
}
