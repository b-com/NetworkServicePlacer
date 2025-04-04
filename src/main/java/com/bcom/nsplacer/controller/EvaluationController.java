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
package com.bcom.nsplacer.controller;

import com.bcom.nsplacer.heroku.WSocketController;
import com.bcom.nsplacer.misc.MathUtils;
import com.bcom.nsplacer.misc.ParameterInitializer;
import com.bcom.nsplacer.misc.StreamUtils;
import com.bcom.nsplacer.model.FileEntry;
import com.bcom.nsplacer.model.dto.EvaluationParams;
import com.bcom.nsplacer.model.dto.EvaluationResults;
import com.bcom.nsplacer.placement.*;
import com.bcom.nsplacer.placement.enums.*;
import com.bcom.nsplacer.placement.routing.DijkstraRoutingAlgorithm;
import com.bcom.nsplacer.placement.routing.UCSRoutingAlgorithm;
import com.bcom.nsplacer.service.FileEntryService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/eval")
public class EvaluationController {

    public static ParameterInitializer availableCpu = new ParameterInitializer(false, 0, 0, 1000000);
    public static ParameterInitializer availableStorage = new ParameterInitializer(false, 0, 0, 1000000);
    public static ParameterInitializer availableBandwidth = new ParameterInitializer(false, 0, 0, 10);
    public static ParameterInitializer availableLatency = new ParameterInitializer(false, 0, 0, 1);

    @Autowired
    private FileEntryService fileEntryService;

    @Autowired
    private WSocketController wSocketController;

    private Map<String, SessionParams> sessions = new HashMap<>();

    private SessionParams getSessionParams(HttpServletRequest request) {
        if (!sessions.containsKey(request.getSession().getId())) {
            sessions.put(request.getSession().getId(), new SessionParams());
        }
        return sessions.get(request.getSession().getId());
    }

    @PostMapping("/start")
    public String start(HttpServletRequest request, HttpServletResponse response, @RequestBody EvaluationParams params) throws IOException {
        final SessionParams sessionParams = getSessionParams(request);
        sessionParams.getResults().setCounter(0);
        sessionParams.params = params;
        String networkTopology = params.getNetworkTopology();
        FileEntry fileEntry = fileEntryService.findByName(networkTopology);
        NetworkGraph networkGraph = ZooTopologyIO.fromXML(StreamUtils.readString(fileEntryService.getInputStream(fileEntry.getId())), EvaluationParams.toInitializer(params.getSnCPU()), EvaluationParams.toInitializer(params.getSnStorage()), EvaluationParams.toInitializer(params.getSnBandwidth()), EvaluationParams.toInitializer(params.getSnLatency()));
        if (sessionParams.placer != null) {
            sessionParams.placer.destroy();
        }
        sessionParams.getResults().setLinkRBPercent(SessionParams.createLinkRBPercent(networkGraph));
        sessionParams.getResults().setNodeRCPercent(SessionParams.createNodeRCPercent(networkGraph));
        sessionParams.placer = new Placer(networkGraph, null, PlacementApproach.valueOf(params.getApproach()), params.getVneConst(), params.getShuffle(),
                TerminationType.valueOf(params.getTerminationType()), RoutingType.UCS.toString().equals(params.getRouting()) ? new UCSRoutingAlgorithm(networkGraph) : new DijkstraRoutingAlgorithm(networkGraph),
                SearchStrategy.valueOf(params.getStrategy()), params.getTimeout(), null);
        sessionParams.placer.setEvaluationParams(params);
        new Thread(new Evaluator(sessionParams)).start();
        return "ok";
    }

    @GetMapping("/status")
    public EvaluationResults status(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return getSessionParams(request).getResults();
    }

    @GetMapping("/getPlacementDetails")
    public String getPlacementDetails(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final SessionParams sessionParams = getSessionParams(request);
        return sessionParams.placementDetails;
    }

    @GetMapping("/getResourceDetails")
    public NetworkGraph getResourceDetails(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final SessionParams sessionParams = getSessionParams(request);
        return sessionParams.placer.getNetworkGraph();
    }

    @GetMapping("/stop")
    public String stop(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final SessionParams sessionParams = getSessionParams(request);
        try {
            sessionParams.getResults().setStop(true);
            sessionParams.placer.stop();
        } catch (Exception ex) {
        }
        return "ok";
    }

    @Getter
    @Setter
    public static class SessionParams {
        private Placer placer;
        private String placementDetails = null;
        private EvaluationResults results = new EvaluationResults();
        private EvaluationParams params;

        public static String createLinkRBPercent(NetworkGraph graph) {
            StringBuilder sb = new StringBuilder();
            for (NetworkLink link : graph.getLinks()) {
                if (link.isLoop()) {
                    continue;
                }
                sb.append(link.getLabel() + "," + (int) link.getBandwidth().getRemainingPercent() + "\n");
            }
            return sb.toString();
        }

        public static String createNodeRCPercent(NetworkGraph graph) {
            StringBuilder sb = new StringBuilder();
            for (NetworkNode node : graph.getNodes()) {
                sb.append(node.getLabel() + "," + (int) ((node.getCpu().getRemainingPercent() + node.getStorage().getRemainingPercent()) / 2.0) + "\n");
            }
            return sb.toString();
        }
    }

    public static class Evaluator implements Runnable {

        private SessionParams sessionParams;

        public Evaluator(SessionParams sessionParams) {
            this.sessionParams = sessionParams;
        }

        @Override
        public void run() {
            try {
                TopologyType topologyType = TopologyType.valueOf(sessionParams.params.getServiceTopology());
                ParameterInitializer requiredCPU = EvaluationParams.toInitializer(sessionParams.params.getSgCPU());
                ParameterInitializer requiredStorage = EvaluationParams.toInitializer(sessionParams.params.getSgStorage());
                ParameterInitializer requiredBandwidth = EvaluationParams.toInitializer(sessionParams.params.getSgBandwidth());
                ParameterInitializer requiredLatency = EvaluationParams.toInitializer(sessionParams.params.getSgLatency());
                sessionParams.getResults().setRunning(true);
                sessionParams.getResults().setStop(false);
                List<Long> times = new ArrayList<>();
                ServiceGraph serviceGraph = new ServiceGraph();
                while (!sessionParams.getResults().isStop()) {
                    serviceGraph.create(topologyType, sessionParams.params.getServiceSize(), sessionParams.params.getSgUsers(), requiredCPU, requiredStorage, requiredBandwidth, requiredLatency);
                    sessionParams.placer.setServiceGraph(serviceGraph);
                    sessionParams.placer.run();
                    if (sessionParams.placer.hasFoundPlacement()) {
                        sessionParams.placer.applyBestFoundPlacement();
                        sessionParams.getResults().setCounter(sessionParams.placer.getPerformedPlacementsCounter());
                        sessionParams.getResults().setLinkRBPercent(SessionParams.createLinkRBPercent(sessionParams.placer.getNetworkGraph()));
                        sessionParams.getResults().setNodeRCPercent(SessionParams.createNodeRCPercent(sessionParams.placer.getNetworkGraph()));
                        times.add(sessionParams.placer.getExecutionTime());
                    } else {
                        break;
                    }
                }
                sessionParams.placementDetails = sessionParams.placer.getPlacementListDescription();
                String precision = "%.0f";
                double bwRemaining = ((double) sessionParams.placer.getNetworkGraph().getTotalRemainingBandwidth() /
                        sessionParams.placer.getNetworkGraph().getTotalMaximumBandwidth() * 100.0);
                double cpuRemaining = ((double) sessionParams.placer.getNetworkGraph().getTotalRemainingCpu() /
                        sessionParams.placer.getNetworkGraph().getTotalMaximumCpu() * 100.0);
                double stRemaining = ((double) sessionParams.placer.getNetworkGraph().getTotalRemainingStorage() /
                        sessionParams.placer.getNetworkGraph().getTotalMaximumStorage() * 100.0);
                List<Long> quartiles = MathUtils.quartile(times);
                if (quartiles.isEmpty()) {
                    for (int i = 0; i < 5; i++) {
                        quartiles.add(0l);
                    }
                }
                sessionParams.getResults().setQ0Time(quartiles.get(0).intValue());
                sessionParams.getResults().setQ1Time(quartiles.get(1).intValue());
                sessionParams.getResults().setQ2Time(quartiles.get(2).intValue());
                sessionParams.getResults().setQ3Time(quartiles.get(3).intValue());
                sessionParams.getResults().setQ4Time(quartiles.get(4).intValue());
                sessionParams.getResults().setAvgTime((int) MathUtils.average(MathUtils.toDoubleArray(times)));
                sessionParams.getResults().setBwRemaining(String.format(precision, bwRemaining));
                sessionParams.getResults().setCpuRemaining(String.format(precision, cpuRemaining));
                sessionParams.getResults().setStRemaining(String.format(precision, stRemaining));
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                sessionParams.getResults().setRunning(false);
            }
        }
    }
}
