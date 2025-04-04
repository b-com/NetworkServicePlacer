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
package com.bcom.nsplacer.placement.evaluation;

import com.bcom.nsplacer.NsPlacerApplication;
import com.bcom.nsplacer.misc.MathUtils;
import com.bcom.nsplacer.misc.ParameterInitializer;
import com.bcom.nsplacer.misc.StreamUtils;
import com.bcom.nsplacer.placement.*;
import com.bcom.nsplacer.placement.enums.PlacementApproach;
import com.bcom.nsplacer.placement.enums.SearchStrategy;
import com.bcom.nsplacer.placement.enums.TerminationType;
import com.bcom.nsplacer.placement.enums.TopologyType;
import com.bcom.nsplacer.placement.routing.RoutingAlgorithm;
import com.bcom.nsplacer.placement.routing.UCSRoutingAlgorithm;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class Evaluation {

    public static ParameterInitializer requiredCpu = new ParameterInitializer(false, 0, 0, 1);
    public static ParameterInitializer requiredStorage = new ParameterInitializer(false, 0, 0, 1);
    public static ParameterInitializer requiredBandwidth = new ParameterInitializer(false, 0, 0, 1);
    public static ParameterInitializer requiredLatency = new ParameterInitializer(false, 0, 0, 10);
    public static ParameterInitializer availableCpu = new ParameterInitializer(false, 0, 0, 10);
    public static ParameterInitializer availableStorage = new ParameterInitializer(false, 0, 0, 10);
    public static ParameterInitializer availableBandwidth = new ParameterInitializer(false, 0, 0, 1000);
    public static ParameterInitializer availableLatency = new ParameterInitializer(false, 0, 0, 1);

    public static void singleTest() throws Exception {
        //String SNTopology = "BtNorthAmerica.graphml.xml";
        String SNTopology = "BtEurope.graphml.xml";
        //String SNTopology = "BtAsiaPac.graphml.xml";
        //String SNTopology = "Grid-7x6.xml";
        NetworkGraph networkGraph = ZooTopologyIO.fromXML(StreamUtils.readString(new File(NsPlacerApplication.zooTopologiesDir + "/" + SNTopology)), availableCpu, availableStorage, availableBandwidth, availableLatency);
        boolean shuffle = true;
        TopologyType topologyType = TopologyType.DaisyChain;
        SearchStrategy strategy = SearchStrategy.ABO;
        int timeout = 1000;
        boolean vneConst = true;

        int cnt = 0;
        ServiceGraph serviceGraph = new ServiceGraph();
        int c0 = 1000;
        int r0 = 8;
        double resourceUnitCost = 0.5;
        double resourceUnitRevenue = 1.0;
        double r2c = 0;

        boolean newMethod = false;
        long randomSeed = 0;
        String userLocation = "12";

        Random random = new Random(randomSeed);
        RoutingAlgorithm routingAlgorithm = new UCSRoutingAlgorithm(networkGraph);
        Placer placer = new Placer(networkGraph, null, PlacementApproach.NodeBased, vneConst, shuffle,
                TerminationType.FirstFound, routingAlgorithm,
                strategy,
                timeout, null);

        List<Integer> placedSGSizeList = new ArrayList<>();
        UCSRoutingAlgorithm routing = new UCSRoutingAlgorithm(networkGraph);

        for (int i = 0; i < 100; i++) {
//            System.out.println("\nPlacing service #" + i + "...");
            //int sgSize = (Math.abs(random.nextInt()) % 6) + 3;
            int sgSize = 3;
            //int e2eLatency = (Math.abs(random.nextInt()) % 20);
            int e2eLatency = 10;
            //System.out.println("SGSize: " + sgSize + ", E2E-Latency: " + e2eLatency);
            serviceGraph.create(topologyType, sgSize, userLocation, requiredCpu, requiredStorage, requiredBandwidth, requiredLatency);

            if (newMethod) {
                List<Record> records = new ArrayList<>();
                for (int j = 0; j < networkGraph.getNodes().size(); j++) {
                    Record rec = new Record();
                    rec.nodeLabel = networkGraph.getNodes().get(j).getLabel();
                    SearchState state = new SearchState(networkGraph, serviceGraph, 0, null, null, null);
                    rec.distance = rec.nodeLabel.equals(userLocation) ? 0 : routing.route(state, rec.nodeLabel, userLocation, new ServiceLink()).get(0).getLinks().size();
                    records.add(rec);
                }
                Collections.sort(records, Comparator.comparingInt(o -> o.distance));
                for (int j = 0; j < records.size(); j++) {
                    if (records.get(j).distance * 2 > e2eLatency) {
                        while (records.size() > j) {
                            records.remove(j);
                        }
                        break;
                    }
                }
                Collections.reverse(records);

                for (int j = 0; j < records.size(); j++) {
                    serviceGraph.getNodes().get(1).setNetworkNode(records.get(j).nodeLabel);
                    placer.setServiceGraph(serviceGraph);
                    placer.run();
                    if (placer.hasFoundPlacement()) {
                        break;
                    }
                }
            } else {
                placer.setServiceGraph(serviceGraph);
                placer.run();
            }

            if (placer.hasFoundPlacement()) {
                placedSGSizeList.add(sgSize);
                int latency = placer.getBestFoundState().calcPlacedLatency();
                //System.out.println("Latency: " + latency);
                //System.out.println("time: " + placer.getExecutionTime());
                placer.applyBestFoundPlacement();

                int totalRemainingBandwidth = placer.getNetworkGraph().getTotalRemainingBandwidth();
                double trbPercent = totalRemainingBandwidth / placer.getNetworkGraph().getTotalMaximumBandwidth() * 100.0;
                //System.out.println("totalRemainingBandwidth: " + totalRemainingBandwidth + String.format(" (%.2f%%)", trbPercent));

                cnt++;
                //System.out.println("Placement succeeded! cnt: " + cnt + ", R2C: " + calcR2C(placedSGSizeList, r0, c0, resourceUnitCost, resourceUnitRevenue));
            } else {
                //System.out.println("Placement failed!");
                placer.setStrategy(SearchStrategy.EnhancedBF);
                System.out.println("changed strategy");
            }
            System.out.println("" + (i + 1) + "\t" + cnt);
        }

        System.out.println("Successful placement count: " + cnt);
        System.out.println("R2C: " + calcR2C(placedSGSizeList, r0, c0, resourceUnitCost, resourceUnitRevenue));

//            System.out.println("Validation result: " + placer.validatePerformedPlacements());
//            System.out.println("Total time: " + totalTime);
//            System.out.println("Average time: " + (int) ((double) totalTime / cnt));
//            System.out.println("Average latency: " + (int) ((double) totalLatency / cnt));
//            System.out.println("totalRemainingBandwidth: " + totalRemainingBandwidth + String.format(" (%.2f%%)", trbPercent));
//            System.out.println("# of placed services: " + cnt);

        //System.out.println(subgraphAvg);

        placer.destroy();
    }

    private static String calcR2C(List<Integer> list, double r0, double c0, double resourceUnitCost, double resourceUnitRevenue) {
        double r = 0, c = c0;
        for (int i = 0; i < list.size(); i++) {
            r += r0 + (list.get(i) - 1) * 2 * resourceUnitRevenue;
            c += (list.get(i) - 1) * 2 * resourceUnitCost;
        }
        return String.format("%.3f", r / c);
    }

    public static void evaluation() throws Exception {
        List<String> SNTopologies = Arrays.asList(
                NsPlacerApplication.zooTopologiesDir + "/" + "BtEurope.graphml.xml"
                //NsPlacerApplication.zooTopologiesDir + "/" + "BtAsiaPac.graphml.xml"
                //NsPlacerApplication.zooTopologiesDir + "/" + "Sago.graphml.xml"
                //NsPlacerApplication.zooTopologiesDir + "/" + "BtNorthAmerica.graphml.xml"
        );
        List<TopologyType> topologyTypes = Arrays.asList(
                TopologyType.DaisyChain,
                TopologyType.Ring,
                TopologyType.Star
        );
        List<SearchStrategy> searchStrategies = Arrays.asList(
                //SearchStrategy.Parallel
                //SearchStrategy.FairABO,
                SearchStrategy.ABO
                //SearchStrategy.DBO,
                //SearchStrategy.EnhancedBF,
                //SearchStrategy.BF
        );
        int timeout = 1000;
        boolean vneConst = false;
        for (String SNTopology : SNTopologies) {
            System.out.println(SNTopology);
            TerminationType placerType = TerminationType.FirstFound;

            NetworkGraph networkGraph = ZooTopologyIO.fromXML(StreamUtils.readString(new File(SNTopology)), availableCpu, availableStorage, availableBandwidth, availableLatency);
            RoutingAlgorithm routingAlgorithm = new UCSRoutingAlgorithm(networkGraph);

            System.out.println("Demand\tServiceTopology\tMethod\tServiceSize\t#PlacedServices\tQ0\tQ1\tQ2\tQ3\tQ4\tAverage\tFinalRemainedBandwidthPercent\tAverageUsedBandwidthPercent\tOptimalPercent");
            for (int bandwidthDemand = 1; bandwidthDemand <= 10; bandwidthDemand += 2) {
                requiredBandwidth = new ParameterInitializer(false, 0, 0, bandwidthDemand);
                for (TopologyType topologyType : topologyTypes) {
                    for (SearchStrategy strategy : searchStrategies) {
                        for (int serviceSize = 3; serviceSize <= 10; serviceSize++) {
                            boolean shuffle = false;
                            Placer placer = new Placer(networkGraph, null, PlacementApproach.NodeBased, vneConst, shuffle, placerType, routingAlgorithm, strategy, timeout, null);

                            int cnt = 0, optimalCnt = 0;
                            List<Long> times = new ArrayList<>();
                            Random random = null;
                            ServiceGraph serviceGraph = new ServiceGraph();
                            while (true) {
                                serviceGraph.create(topologyType, serviceSize, null, requiredCpu, requiredStorage, requiredBandwidth, requiredLatency);
                                placer.setServiceGraph(serviceGraph);
                                placer.run();
                                if (placer.hasFoundPlacement()) {
                                    if (placer.isOptimalState()) {
                                        optimalCnt++;
                                    }
                                    placer.applyBestFoundPlacement();
                                    cnt++;
                                    times.add(placer.getExecutionTime());
                                } else {
                                    String precision = "%.2f";
                                    double percentRemaining = ((double) placer.getNetworkGraph().getTotalRemainingBandwidth() /
                                            placer.getNetworkGraph().getTotalMaximumBandwidth() * 100.0);
                                    double usedResourcePerServicePercent = (100.0 - percentRemaining) / cnt;
                                    List<Long> quartiles = MathUtils.quartile(times);
                                    if (quartiles.isEmpty()) {
                                        for (int i = 0; i < 5; i++) {
                                            quartiles.add(0l);
                                        }
                                    }
                                    placer.destroy();
                                    System.out.println(bandwidthDemand
                                            + "\t" + topologyType
                                            + "\t" + strategy
                                            + "\t" + serviceSize
                                            + "\t" + cnt
                                            + "\t" + quartiles.get(0)
                                            + "\t" + quartiles.get(1)
                                            + "\t" + quartiles.get(2)
                                            + "\t" + quartiles.get(3)
                                            + "\t" + quartiles.get(4)
                                            + "\t" + (int) MathUtils.average(MathUtils.toDoubleArray(times))
                                            + "\t" + String.format(precision, percentRemaining)
                                            + "\t" + String.format(precision, usedResourcePerServicePercent)
                                            + "\t" + String.format(precision, (double) optimalCnt / cnt)
                                    );

                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        //for (int i = 0; i < 100; i++) {
        singleTest();
        //}
        //evaluation();
        //compareTheResults();
    }

    public static void compareTheResults() throws Exception {
        String files[] = new String[]{
                "./Bt-Europe.xlsx",
                "./Bt-Asia.xlsx",
                //"./Bt-North-America.xlsx",
        };

        File vbsFile = new File("./misc/XlsToCsv.vbs");
        List<EvaluationRecord> records = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            System.out.println("File: " + files[i]);
            File file = new File(files[i]);
            File tempFile = new File(file.getName() + ".csv");
            String output = runCommand("cscript " + vbsFile.getAbsolutePath() + " " + file.getAbsolutePath() + " " + tempFile.getAbsolutePath(), null);
            addRecords(tempFile.getName(), tempFile, records);
            tempFile.delete();
        }

        for (EvaluationRecord record : records) {
            EvaluationRecord ref = null;
            for (EvaluationRecord r : records) {
                if (r.getBandwidth() == record.getBandwidth() && r.getTopologyType().equals(record.getTopologyType()) &&
                        r.getStrategy().equals(SearchStrategy.Parallel) && r.getServiceSize() == record.getServiceSize()) {
                    ref = r;
                    break;
                }
            }
            int rsp = record.getPlacedServices();
            rsp = rsp == 0 ? 1 : rsp;
            double measure = (double) ref.getPlacedServices() / rsp;
            measure = (measure - 1.0) * 100.0;
            record.setMeasure(measure);
        }

        //for (int serviceSize = 3; serviceSize <= 10; serviceSize++) {
        //  System.out.println("serviceSize: " + serviceSize);
        for (SearchStrategy strategy : SearchStrategy.values()) {
            if (SearchStrategy.Parallel.equals(strategy)) {
                continue;
            }
            System.out.println("Strategy: " + strategy);
            //int finalServiceSize = serviceSize;
            List<EvaluationRecord> list = records.stream().filter(x -> (x.getStrategy().equals(strategy))
                    && (x.getServiceSize() < 9)
            ).collect(Collectors.toList());
            List<Double> measures = list.stream().map(x -> x.getMeasure()).collect(Collectors.toList());
            List<Double> quartile = MathUtils.quartile(measures);
            if (!quartile.isEmpty()) {
                for (int i = 0; i < quartile.size(); i++) {
                    System.out.print(toString(quartile.get(i), 2) + "\t");
                }
            }
            System.out.print(toString(MathUtils.average(measures), 2));
            System.out.println();
        }
        // }
    }

    public static void addRecords(String substrateNetwork, File file, List<EvaluationRecord> records) throws Exception {
        Scanner sc = new Scanner(StreamUtils.readString(file));
        sc.nextLine();
        while (sc.hasNext()) {
            String line = sc.nextLine().trim();
            if ("".equals(line)) {
                continue;
            }
            EvaluationRecord record = new EvaluationRecord();
            String[] split = line.split(",");
            record.setBandwidth(Integer.parseInt(split[0]));
            record.setTopologyType(TopologyType.valueOf(split[1]));
            record.setStrategy(SearchStrategy.valueOf(split[2]));
            record.setServiceSize(Integer.parseInt(split[3]));
            record.setPlacedServices(Integer.parseInt(split[4]));
            record.setQ0(Integer.parseInt(split[5]));
            record.setQ1(Integer.parseInt(split[6]));
            record.setQ2(Integer.parseInt(split[7]));
            record.setQ3(Integer.parseInt(split[8]));
            record.setQ4(Integer.parseInt(split[9]));
            record.setAvg(Integer.parseInt(split[10]));
            record.setRemainingBandwidth(Double.parseDouble(split[11]));
            record.setAverageUsedBandwidth(Double.parseDouble(split[12]));
            records.add(record);
        }
        sc.close();
    }

    public static String runCommand(String cmd, String systemInput) {
        Process p;
        int status = -1;
        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();
        try {
            p = Runtime.getRuntime().exec(cmd);
            if (systemInput != null) {
                p.getOutputStream().write(systemInput.getBytes());
            }
            p.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            while ((line = reader.readLine()) != null) {
                error.append(line).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    public static String toString(double d, int precision) {
        return String.format("%." + precision + "f", d);
    }

    public static class Record {
        public String nodeLabel;
        public int distance;
        public SearchState placement;
    }

    @Getter
    @Setter
    @ToString
    public static class EvaluationRecord implements Comparable<EvaluationRecord> {
        private int bandwidth;
        private TopologyType topologyType;
        private SearchStrategy strategy;
        private int serviceSize;
        private int placedServices;
        private int Q0, Q1, Q2, Q3, Q4, avg;
        private double remainingBandwidth, averageUsedBandwidth;
        private Double measure = 0.0;

        @Override
        public int compareTo(EvaluationRecord o) {
            return measure.compareTo(o.measure);
        }
    }
}
