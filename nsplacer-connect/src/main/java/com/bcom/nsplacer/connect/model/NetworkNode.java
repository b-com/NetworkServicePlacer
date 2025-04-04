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
package com.bcom.nsplacer.connect.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;
import java.util.Random;

@Getter
@Setter
@ToString
public class NetworkNode implements Comparable<NetworkNode> {

    private String label;

    private Resource cpu, storage;

    private int x, y;

    public NetworkNode() {
        cpu = new Resource(1000, 1000);
        storage = new Resource(1000, 1000);
        label = "unknown";
    }

    public NetworkNode clone(boolean reset) {
        NetworkNode n = new NetworkNode();
        n.setLabel(getLabel());
        n.setCpu(cpu.clone(reset));
        n.setStorage(storage.clone(reset));
        return n;
    }

    public boolean checkFeasibility() {
        return (cpu.getAmount() >= 0 && storage.getAmount() >= 0);
    }

    public void setRandomValues(Random random) {
        cpu.setRandomValues(random);
        storage.setRandomValues(random);
    }

    public boolean canAccommodate(ServiceNode f) {
        return (cpu.getAmount() >= f.getCpu() && storage.getAmount() >= f.getStorage());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkNode that = (NetworkNode) o;
        return label.equals(that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }

    @Override
    public int compareTo(NetworkNode o) {
        return getLabel().compareTo(o.getLabel());
    }
}

