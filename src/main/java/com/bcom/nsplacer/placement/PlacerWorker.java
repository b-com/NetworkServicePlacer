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

import com.bcom.nsplacer.NsPlacerApplication;
import com.bcom.nsplacer.heroku.WSConnection;
import com.bcom.nsplacer.heroku.WSocketController;
import com.bcom.nsplacer.misc.StreamUtils;
import com.bcom.nsplacer.placement.enums.PlacementApproach;
import com.bcom.nsplacer.placement.enums.SearchStrategy;
import com.bcom.nsplacer.placement.enums.TerminationType;
import com.bcom.nsplacer.placement.routing.RoutingPath;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class PlacerWorker implements Runnable {

    private Placer placer;
    private TerminationType terminationType;
    private SearchStrategy strategy;
    private volatile long iterCount, maxIterCount;
    private SearchState bestFound = null;
    private CountDownLatch latch;
    private PlacementApproach approach;

    public PlacerWorker(Placer placer, PlacementApproach approach, TerminationType terminationType, SearchStrategy strategy, long timeout, CountDownLatch latch) {
        this.placer = placer;
        this.approach = approach;
        this.terminationType = terminationType;
        this.strategy = strategy;
        this.maxIterCount = timeout;
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            if (SearchStrategy.Custom.equals(strategy)) {
                WSocketController controller = NsPlacerApplication.getBean(WSocketController.class);
                WSConnection con = controller.getPlacer();
                if (con == null) {
                    return;
                }
                ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(10, true);
                controller.setPlacerResponseListener(resp -> {
                    try {
                        queue.put(resp);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                ObjectMapper mapper = new ObjectMapper();
                try {
                    StringBuilder sb = new StringBuilder();
                    Map<String, Object> map = new HashMap<>();
                    map.put("network", placer.getNetworkGraph());
                    map.put("service", placer.getServiceGraph());
                    map.put("params", placer.getEvaluationParams());
                    controller.send(con.getWss(), controller.createMessage(mapper.writeValueAsString(map)));
                    String responseJson = queue.poll(10, TimeUnit.MINUTES);
                    if (responseJson != null) { // Timeout has reached!
                        CustomPlacerResponse response = mapper.readValue(responseJson, CustomPlacerResponse.class);
                        if (response.succeeded) {
                            SearchState root = createRootState();
                            root.setPlacedServiceNodes(response.getPlacedServiceNodes());
                            root.setPlacedNetworkNodes(response.getPlacedNetworkNodes());
                            root.setPlacedServiceLinks(response.getPlacedServiceLinks());
                            root.setPlacedPaths(response.getPlacedPaths());
                            root.applyFoundPlacementResources();
                            bestFound = root;
                        }
                    }
                } catch (InterruptedException | JsonProcessingException e) {
                    e.printStackTrace();
                }
            } else {
                Fringe fringe = new Fringe(strategy.isCompleteSearch(), placer.isShuffle());
                SearchState root = createRootState();
                fringe.put(Arrays.asList(root), -1);
                int initFC = strategy.isCompleteSearch() ? (int) Math.round(root.getObjectiveValue()) : -1;
                iterCount = 0;
                boolean memLimitReached = false;
                if (strategy.isCompleteSearch()) {
                    while (!mustStop() && !memLimitReached) {
                        if (iterCount != 0) {
                            root = createRootState();
                            fringe.put(Arrays.asList(root), -1);
                            initFC++;
                        }
                        while (!fringe.isEmpty() && !mustStop()) {
                            long st = System.currentTimeMillis();
                            expand(fringe, initFC);
                            long et = System.currentTimeMillis();
                            iterCount++;
                            long dur = et - st;
                            if (dur > 1000 && StreamUtils.getFreeMemPercent() <= 12) {
                                memLimitReached = true;
                                break;
                            }
                        }
                    }
                } else {
                    while (!fringe.isEmpty() && !mustStop()) {
                        expand(fringe, -1);
                        iterCount++;
                    }
                }
            }
        } finally {
            latch.countDown();
        }
    }

    private SearchState createRootState() {
        SearchState root = new SearchState(placer.getNetworkGraph().clone(false), placer.getServiceGraph().clone(), 0, placer, strategy, approach);
        root.updateObjectiveValue();
        return root;
    }

    private boolean mustStop() {
        return !placer.isRunning() || (iterCount > this.maxIterCount) ||
                (TerminationType.FirstFound.equals(terminationType) && bestFound != null);
    }

    private void expand(Fringe fringe, int maxFC) {
        SearchState state = fringe.take();
        if (state.isFeasible()) {
            if (state.isTerminal()) {
                if ((bestFound == null) || (strategy.isMaximizer() ?
                        (state.getObjectiveValue() > bestFound.getObjectiveValue()) :
                        (state.getObjectiveValue() < bestFound.getObjectiveValue()))) {
                    bestFound = state;
                }
            } else {
                fringe.put(state.expand(), maxFC);
            }
        }
    }


    @Getter
    @Setter
    public static class CustomPlacerResponse {

        private boolean succeeded;
        private List<ServiceNode> placedServiceNodes;
        private List<String> placedNetworkNodes;
        private List<ServiceLink> placedServiceLinks;
        private List<RoutingPath> placedPaths;
    }

}
