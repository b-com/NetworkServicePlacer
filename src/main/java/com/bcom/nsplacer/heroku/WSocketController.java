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
package com.bcom.nsplacer.heroku;

import com.bcom.nsplacer.placement.PlacerWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Primitives;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class WSocketController implements WebSocketHandler {

    public static Map<String, WSConnection> connectionMap = Collections.synchronizedMap(new HashMap<>());
    public static Map<String, ProxyGateway> proxyGatewayMap = Collections.synchronizedMap(new HashMap<>());
    private static ExecutorService executorService;
    private ObjectMapper jsonMapper;
    private CustomPlacerResponseListener placerResponseListener = null;

    public WSocketController() {
        jsonMapper = new ObjectMapper();
        executorService = Executors.newCachedThreadPool();
    }

    public void setPlacerResponseListener(CustomPlacerResponseListener placerResponseListener) {
        this.placerResponseListener = placerResponseListener;
    }

    public void send(WebSocketSession wss, WebSocketMessage<?> wsMsg) {
        executorService.execute(() -> {
            synchronized (wss) {
                try {
                    wss.sendMessage(wsMsg);
                } catch (Exception e) {
                }
            }
        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession wss) throws Exception {
        connectionMap.put(wss.getId(), new WSConnection(wss));
    }

    @Override
    public void handleMessage(WebSocketSession wsSession, WebSocketMessage<?> wsMsg) throws Exception {
        try {
            WSConnection connection = connectionMap.get(wsSession.getId());

            String payloadString = null;
            byte payloadBytes[] = null;
            if (wsMsg instanceof TextMessage) {
                payloadString = ((TextMessage) wsMsg).getPayload();
            } else {
                payloadBytes = ((BinaryMessage) wsMsg).getPayload().array();
            }

            if ("Ping".equals(payloadString)) {
                send(wsSession, createMessage("Pong"));
                return;
            }

            if (connection.getType() == null) {
                if (payloadString != null) {
                    String args[] = payloadString.split(";");
                    connection.setType(WSocketType.valueOf(args[0]));
                    if (WSocketType.Publisher.equals(connection.getType())) {
                        connection.setTopic(args[1]);
                    } else if (WSocketType.Subscriber.equals(connection.getType())) {
                        connection.setTopic(args[1]);
                    } else if (WSocketType.Proxy.equals(connection.getType())) {
                        // Nothing
                    } else if (WSocketType.WebRTC.equals(connection.getType())) {
                        // Nothing
                    } else if (WSocketType.Meeting.equals(connection.getType())) {
                        // Nothing
                    } else if (WSocketType.Placer.equals(connection.getType())) {
                        // Nothing
                    }
                }
            } else {
                if (WSocketType.Publisher.equals(connection.getType())) {
                    List<WSConnection> list = new ArrayList<>();
                    synchronized (connectionMap) {
                        for (String key : connectionMap.keySet()) {
                            WSConnection s = connectionMap.get(key);
                            if (WSocketType.Subscriber.equals(s.getType()) && connection.getTopic().equals(s.getTopic())) {
                                list.add(connectionMap.get(key));
                            }
                        }
                    }
                    for (WSConnection s : list) {
                        send(s.getWss(), wsMsg);
                    }
                } else if (WSocketType.Subscriber.equals(connection.getType())) {
                    // Nothing
                } else if (WSocketType.Proxy.equals(connection.getType())) {
                    handleProxyMessage(connection, wsMsg);
                } else if (WSocketType.WebRTC.equals(connection.getType())) {
                    handleWebRTCMessage(connection, wsMsg);
                } else if (WSocketType.Meeting.equals(connection.getType())) {
                    handleMeetingMessage(connection, wsMsg);
                } else if (WSocketType.Placer.equals(connection.getType())) {
                    if (placerResponseListener != null) {
                        placerResponseListener.received(payloadString);
                    }
                }
            }
        } catch (Exception ex) {
        }
    }

    public void handleProxyMessage(WSConnection session, WebSocketMessage<?> wsMsg) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(((BinaryMessage) wsMsg).getPayload().array()));
        String cmd = dis.readUTF();
        String id = dis.readUTF();
        if ("connect".equals(cmd)) {
            String ip = dis.readUTF();
            int port = dis.readInt();
            String version = dis.readUTF();
            ProxyGateway gateway = new ProxyGateway(this, session.getWss(), id, ip, port, version);
            proxyGatewayMap.put(id, gateway);
            new Thread(gateway).start();
        } else if ("send".equals(cmd)) {
            int len = dis.readInt();
            byte buf[] = new byte[len];
            dis.readFully(buf);
            ProxyGateway gateway = proxyGatewayMap.get(id);
            if (gateway != null) {
                gateway.send(buf);
            }
        } else if ("disconnect".equals(cmd)) {
            ProxyGateway gateway = proxyGatewayMap.get(id);
            if (gateway != null) {
                gateway.disconnect();
            }
        }
    }

    public void handleWebRTCMessage(WSConnection connection, WebSocketMessage<?> wsMsg) throws IOException {
        synchronized (connectionMap) {
            String payload = (String) wsMsg.getPayload();
            if (payload.contains("\"event\":\"setName\"")) {
                connection.setWebrtcName(getData(payload).trim());
                sendWebRTCList();
            } else if (payload.contains("\"event\":\"chatMessage\"")) {
                for (String sessionId : connectionMap.keySet()) {
                    WSConnection s = connectionMap.get(sessionId);
                    if (s.getWebrtcName() != null) {
                        send(s.getWss(), wsMsg);
                    }
                }
            } else if (payload.contains("\"event\":\"requestCall\"") || payload.contains("\"event\":\"cancelCall\"")) {
                String[] split = getData(payload).split(";");
                for (String sessionId : connectionMap.keySet()) {
                    WSConnection s = connectionMap.get(sessionId);
                    if ((s.getWebrtcName() != null) && split[1].equals(s.getWebrtcName())) {
                        send(s.getWss(), wsMsg);
                        break;
                    }
                }
            } else if (payload.contains("\"event\":\"replyCall\"")) {
                String[] split = getData(payload).split(";");
                boolean accepted = "true".equals(split[2]);
                for (String id : connectionMap.keySet()) {
                    WSConnection s = connectionMap.get(id);
                    if (split[0].equals(s.getWebrtcName())) {
                        if (accepted) {
                            s.setWebrtcOtherName(split[1]);
                        }
                        s.getWss().sendMessage(wsMsg);
                    } else if (split[1].equals(s.getWebrtcName())) {
                        if (accepted) {
                            s.setWebrtcOtherName(split[0]);
                        }
                    }
                }
            } else {
                if (connection.getWebrtcName() != null && connection.getWebrtcOtherName() != null) {
                    for (String id : connectionMap.keySet()) {
                        WSConnection s = connectionMap.get(id);
                        if (connection.getWebrtcOtherName().equals(s.getWebrtcName())) {
                            s.getWss().sendMessage(wsMsg);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void joinToMeeting(WSConnection c1, WSConnection c2) {
        c1.setOpenToJoin(false);
        c2.setOpenToJoin(false);
        c1.setMeetingOtherSide(c2);
        c2.setMeetingOtherSide(c1);
        send(c1.getWss(), createEventMessage("joined", null));
    }

    public TextMessage createEventMessage(String event, String data) {
        Map<String, String> response = new HashMap<>();
        response.put("event", event);
        if (data != null) {
            response.put("data", data);
        }
        TextMessage textMessage = null;
        try {
            textMessage = new TextMessage(jsonMapper.writeValueAsString(response));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return textMessage;
    }

    public TextMessage createMessage(Object obj) {
        try {
            return (Primitives.isWrapperType(obj.getClass()) || (obj instanceof String)) ?
                    new TextMessage("" + obj) :
                    new TextMessage(jsonMapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void handleMeetingMessage(WSConnection connection, WebSocketMessage<?> wsMsg) throws IOException {
        synchronized (connectionMap) {
            String payload = (String) wsMsg.getPayload();
            if (payload.contains("\"event\":\"join\"")) {
                boolean joined = false;
                for (String sessionId : connectionMap.keySet()) {
                    WSConnection c = connectionMap.get(sessionId);
                    if (WSocketType.Meeting.equals(c.getType()) && c.isOpenToJoin()) {
                        joinToMeeting(connection, c);
                        joined = true;
                        break;
                    }
                }
                if (!joined) {
                    connection.setOpenToJoin(true);
                }
            } else if (payload.contains("\"event\":\"leave\"")) {
                connection.setOpenToJoin(false);
                if (connection.getMeetingOtherSide() != null) {
                    send(connection.getMeetingOtherSide().getWss(), createEventMessage("left", null));
                    connection.setMeetingOtherSide(null);
                }
            } else {
                WSConnection otherSide = connection.getMeetingOtherSide();
                if (otherSide != null) {
                    send(otherSide.getWss(), wsMsg);
                }
            }
        }
    }

    private void sendWebRTCList() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String id : connectionMap.keySet()) {
            WSConnection s = connectionMap.get(id);
            if (s.getWebrtcName() != null) {
                sb.append(s.getWebrtcName() + "\n");
            }
        }
        TextMessage textMessage = createEventMessage("list", sb.toString().trim());
        for (String id : connectionMap.keySet()) {
            WSConnection s = connectionMap.get(id);
            if (s.getWebrtcName() != null) {
                send(s.getWss(), textMessage);
            }
        }
    }

    public String getData(String payload) {
        String data = payload.substring(payload.indexOf("\"data\""));
        data = data.substring(8);
        data = data.substring(0, data.indexOf("\""));
        return data;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession wss, CloseStatus closeStatus) throws Exception {
        WSConnection connection = connectionMap.get(wss.getId());
        connectionMap.remove(wss.getId());
        proxyGatewayMap.remove(wss.getId());
        if (WSocketType.WebRTC.equals(connection.getType())) {
            sendWebRTCList();
        } else if (WSocketType.Placer.equals(connection.getType())) {
            if (placerResponseListener != null) {
                PlacerWorker.CustomPlacerResponse resp = new PlacerWorker.CustomPlacerResponse();
                resp.setSucceeded(false);
                ObjectMapper mapper = new ObjectMapper();
                placerResponseListener.received(mapper.writeValueAsString(resp));
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession webSocketSession, Throwable throwable) throws Exception {
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public WSConnection getPlacer() {
        WSConnection con = null;
        synchronized (connectionMap) {
            for (String sessionId : connectionMap.keySet()) {
                if (WSocketType.Placer.equals(connectionMap.get(sessionId).getType())) {
                    con = connectionMap.get(sessionId);
                }
            }
        }
        return con;
    }
}
