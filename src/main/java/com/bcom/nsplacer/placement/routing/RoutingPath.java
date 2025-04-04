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
package com.bcom.nsplacer.placement.routing;

import com.bcom.nsplacer.placement.NetworkGraph;
import com.bcom.nsplacer.placement.ServiceLink;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RoutingPath {

    private List<String> links;

    public RoutingPath() {
        links = new ArrayList<>();
    }

    public RoutingPath(List<String> links) {
        this.links = links;
    }

    public boolean canAccommodate(NetworkGraph g, ServiceLink serviceLink) {
        for (String linkLabel : links) {
            if (!g.getLink(linkLabel).canAccommodate(serviceLink)) {
                return false;
            }
        }
        return true;
    }

    public String getSrcNode(NetworkGraph g) {
        return links.isEmpty() ? null : g.getLink(links.get(0)).getSrcNode();
    }

    public String getDstNode(NetworkGraph g) {
        return links.isEmpty() ? null : g.getLink(links.get(links.size() - 1)).getDstNode();
    }

    public void takeBandwidth(NetworkGraph g, int v) {
        for (String link : links) {
            g.getLink(link).getBandwidth().addAmount(-v);
        }
    }

    public int getTotalLatency(NetworkGraph g) {
        int sum = 0;
        for (String l : links) {
            sum += g.getLink(l).getLatency().getAmount();
        }
        return sum;
    }

    @Override
    public String toString() {
        return "RoutingPath{" +
                "links=" + links +
                '}';
    }
}
