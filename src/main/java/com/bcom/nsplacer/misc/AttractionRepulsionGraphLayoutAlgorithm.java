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
package com.bcom.nsplacer.misc;

import com.bcom.nsplacer.placement.NetworkGraph;
import com.bcom.nsplacer.placement.NetworkLink;
import com.bcom.nsplacer.placement.NetworkNode;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

public class AttractionRepulsionGraphLayoutAlgorithm implements Runnable {

    private static final double coef = 0.25;
    public boolean stop = false;
    private int iterationCount, width, height;
    private double scaleFactor;
    private Map<String, Point2D.Double> posMap = new HashMap<>();
    private Map<String, Point2D.Double> dispMap = new HashMap<>();
    private NetworkGraph graph;

    public AttractionRepulsionGraphLayoutAlgorithm(NetworkGraph graph, int iterationCount, int width, int height) {
        this.graph = graph;
        this.iterationCount = iterationCount;
        this.width = width;
        this.height = height;
    }

    private void improve(int iteration) {
        for (NetworkNode v : graph.getNodes()) {
            dispMap.get(v.getLabel()).setLocation(0, 0);
            for (NetworkNode u : graph.getNodes()) {
                if (!v.equals(u)) {
                    Point2D.Double deltaPos = new Point2D.Double();
                    deltaPos.setLocation(posMap.get(v.getLabel()).x - posMap.get(u.getLabel()).x, posMap.get(v.getLabel()).y - posMap.get(u.getLabel()).y);
                    double length = length(deltaPos);
                    normalize(deltaPos);
                    scale(deltaPos, forceRepulsive(length, scaleFactor));
                    add(dispMap.get(v.getLabel()), deltaPos);
                }
            }
        }

        for (NetworkLink link : graph.getLinks()) {
            if (link.isLoop()) {
                continue;
            }
            Point2D.Double deltaPos = new Point2D.Double();
            deltaPos.setLocation(posMap.get(link.getSrcNode()).x - posMap.get(link.getDstNode()).x, posMap.get(link.getSrcNode()).y - posMap.get(link.getDstNode()).y);
            double length = length(deltaPos);
            normalize(deltaPos);
            scale(deltaPos, forceAttractive(length, scaleFactor));

            sub(dispMap.get(link.getSrcNode()), deltaPos);
            add(dispMap.get(link.getDstNode()), deltaPos);
        }

        for (NetworkNode v : graph.getNodes()) {
            Point2D.Double disp = new Point2D.Double(dispMap.get(v.getLabel()).getX(), dispMap.get(v.getLabel()).getY());
            double length = length(disp);
            normalize(disp);
            scale(disp, 1.0);
            add(posMap.get(v.getLabel()), disp);

            posMap.get(v.getLabel()).x = Math.min(width, Math.max(0.0, posMap.get(v.getLabel()).x));
            posMap.get(v.getLabel()).y = Math.min(height, Math.max(0.0, posMap.get(v.getLabel()).y));
        }
    }

    private void add(Point2D.Double p1, Point2D.Double p2) {
        p1.x += p2.x;
        p1.y += p2.y;
    }

    private void sub(Point2D.Double p1, Point2D.Double p2) {
        p1.x -= p2.x;
        p1.y -= p2.y;
    }

    private void scale(Point2D.Double p, double k) {
        p.x *= k;
        p.y *= k;
    }

    private void normalize(Point2D.Double p) {
        double norm = 1.0 / length(p);
        p.x *= norm;
        p.y *= norm;
    }

    private double length(Point2D.Double p) {
        return Math.sqrt(p.x * p.x + p.y * p.y);
    }

    private double forceAttractive(double d, double k) {
        return d * d / k;
    }

    private double forceRepulsive(double d, double k) {
        return k * k / d;
    }

    @Override
    public void run() {
        double area = Math.min(width * width, height * height);
        scaleFactor = coef * Math.sqrt(area / graph.getNodes().size());

        for (NetworkNode node : graph.getNodes()) {
            dispMap.put(node.getLabel(), new Point2D.Double());
            posMap.put(node.getLabel(), new Point2D.Double(node.getX(), node.getY()));
        }

        for (int i = 0; (i < iterationCount) && !stop; i++) {
            improve(i);
        }
        for (NetworkNode node : graph.getNodes()) {
            Point2D.Double p = posMap.get(node.getLabel());
            node.setX((int) p.x);
            node.setY((int) p.y);
        }
    }
}