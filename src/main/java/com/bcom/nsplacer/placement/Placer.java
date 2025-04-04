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

import com.bcom.nsplacer.model.dto.EvaluationParams;
import com.bcom.nsplacer.placement.enums.PlacementApproach;
import com.bcom.nsplacer.placement.enums.SearchStrategy;
import com.bcom.nsplacer.placement.enums.TerminationType;
import com.bcom.nsplacer.placement.routing.RoutingAlgorithm;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Setter
@Getter
public class Placer implements Runnable {

    private NetworkGraph networkGraph;
    private ServiceGraph serviceGraph;
    private volatile boolean running = false;
    private SearchState bestFoundState;
    private boolean optimalState = false;
    private long beginTime, finishTime;
    private boolean backtracking = true, shuffle, vneConst = false;
    private TerminationType terminationType;
    private RoutingAlgorithm routingAlgorithm;
    private SearchStrategy strategy;
    private long timeout;
    private PlacerTerminationAction placerTermination;
    private boolean checkOverallLatency;
    private StringBuilder placementListDescription = new StringBuilder();
    private int performedPlacementsCounter = 0;
    private PlacementApproach placementApproach;
    private ExecutorService executor;
    private EvaluationParams evaluationParams;

    public Placer(NetworkGraph networkGraph, ServiceGraph serviceGraph, PlacementApproach placementApproach,
                  boolean vneConst,
                  boolean shuffle,
                  TerminationType terminationType,
                  RoutingAlgorithm routingAlgorithm,
                  SearchStrategy strategy,
                  long timeout, PlacerTerminationAction action) {
        this.placementApproach = placementApproach;
        this.strategy = strategy;
        this.networkGraph = networkGraph.clone(false);
        this.shuffle = shuffle;
        this.vneConst = vneConst;
        this.terminationType = terminationType;
        this.timeout = timeout;
        this.placerTermination = action;
        this.routingAlgorithm = routingAlgorithm;
        setServiceGraph(serviceGraph);
        executor = Executors.newCachedThreadPool();
    }

    public void destroy() {
        try {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
    }

    public void setServiceGraph(ServiceGraph g) {
        if (g != null) {
            this.serviceGraph = g.clone();
        }
    }

    @Override
    public void run() {
        if (!running) {
            running = true;
            beginTime = System.currentTimeMillis();
            try {
                bestFoundState = null;
                optimalState = false;
                List<CountDownLatch> latches;
                if (getStrategy().equals(SearchStrategy.Parallel)) {
                    List<PlacerWorker> workers = new ArrayList<>();

                    workers.add(new PlacerWorker(this, PlacementApproach.NodeBased, TerminationType.FirstFound, SearchStrategy.FairABO, timeout, new CountDownLatch(1)));
                    workers.add(new PlacerWorker(this, PlacementApproach.NodeBased, TerminationType.FirstFound, SearchStrategy.ABO, timeout, new CountDownLatch(1)));
                    //workers.add(new PlacerWorker(this, PlacementApproach.LinkBased, TerminationType.FirstFound, SearchStrategy.ABO, timeout, new CountDownLatch(1)));
                    workers.add(new PlacerWorker(this, PlacementApproach.NodeBased, TerminationType.FirstFound, SearchStrategy.DBO, timeout, new CountDownLatch(1)));
                    //workers.add(new PlacerWorker(this, PlacementApproach.LinkBased, TerminationType.FirstFound, SearchStrategy.DBO, timeout, new CountDownLatch(1)));

                    for (int i = 0; i < workers.size(); i++) {
                        executor.submit(workers.get(i));
                    }
                    for (int i = 0; i < workers.size(); i++) {
                        workers.get(i).getLatch().await();
                    }
                    SearchState bfState = null;
                    for (int i = 0; i < workers.size(); i++) {
                        bfState = workers.get(i).getBestFound();
                        if (bfState != null) {
                            optimalState = workers.get(i).getStrategy().isCompleteSearch();
                            break;
                        }
                    }
                    bestFoundState = bfState;
                } else {
                    if (SearchStrategy.BF.equals(getStrategy())) {
                        strategy = SearchStrategy.EnhancedBF;
                        backtracking = false;
                    }
                    PlacerWorker worker = new PlacerWorker(this, getPlacementApproach(), getTerminationType(), getStrategy(), timeout, new CountDownLatch(1));
                    executor.submit(worker);
                    worker.getLatch().await();
                    optimalState = worker.getStrategy().isCompleteSearch();
                    bestFoundState = worker.getBestFound();
                }
            } catch (InterruptedException iex) {
                iex.printStackTrace();
            } finally {
                finishTime = System.currentTimeMillis();
                running = false;
                if (placerTermination != null) {
                    placerTermination.perform(this);
                }
            }
        }
    }

    public synchronized void stop() {
        running = false;
    }

    public long getExecutionTime() {
        return finishTime - beginTime;
    }

    public boolean hasFoundPlacement() {
        return (bestFoundState != null);
    }

    public void applyBestFoundPlacement() {
        networkGraph = bestFoundState.getNetworkGraph().clone(false);

        placementListDescription.append("Placement #" + performedPlacementsCounter).append("\n");
        placementListDescription.append("Nodes: " + bestFoundState.getPlacementNodeMap()).append("\n");
        placementListDescription.append("Links: " + bestFoundState.getPlacementLinkMap()).append("\n");
        placementListDescription.append("Latency: " + bestFoundState.calcPlacedLatency()).append("\n");
        placementListDescription.append("\n");

        performedPlacementsCounter++;
    }

    public int getPerformedPlacementsCounter() {
        return performedPlacementsCounter;
    }

    public String getPlacementListDescription() {
        return placementListDescription.toString().trim();
    }

    /**
     * If the placements are valid, this function returns true else returns false
     */
    public boolean validatePerformedPlacements() {
        return PlacementValidator.validate(networkGraph, serviceGraph, getPlacementListDescription());
    }

}
