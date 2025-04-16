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
package com.bcom.nsplacer.config;

import com.bcom.nsplacer.placement.PlacerWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class WSocketController implements WebSocketHandler {

    public static Map<String, WSConnection> connectionMap = Collections.synchronizedMap(new HashMap<>());
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

            if (placerResponseListener != null) {
                placerResponseListener.received(payloadString);
            }
        } catch (Exception ex) {
        }
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
            return (obj instanceof String) ?
                    new TextMessage("" + obj) :
                    new TextMessage(jsonMapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
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
        if (placerResponseListener != null) {
            PlacerWorker.CustomPlacerResponse resp = new PlacerWorker.CustomPlacerResponse();
            resp.setSucceeded(false);
            ObjectMapper mapper = new ObjectMapper();
            placerResponseListener.received(mapper.writeValueAsString(resp));
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
                con = connectionMap.get(sessionId);
            }
        }
        return con;
    }
}
