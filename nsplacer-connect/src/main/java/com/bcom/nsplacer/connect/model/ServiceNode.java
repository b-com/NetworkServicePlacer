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

@Getter
@Setter
@ToString
public class ServiceNode {

    private String label;

    private int cpu, storage;

    public ServiceNode() {
        cpu = 100;
        storage = 100;
        label = "unknown";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceNode that = (ServiceNode) o;
        return label.equals(that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }

    public ServiceNode clone() {
        ServiceNode n = new ServiceNode();
        n.setLabel(getLabel());
        n.setCpu(cpu);
        n.setStorage(storage);
        return n;
    }
}
