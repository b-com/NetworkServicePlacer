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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Getter
@Setter
@ToString
public class ServiceLink {

    private String label;

    private String srcNode, dstNode;

    private int bandwidth;

    public ServiceLink() {
        bandwidth = 10;
        dstNode = srcNode = label = "unknown";
    }

    public ServiceLink clone() {
        ServiceLink l = new ServiceLink();
        l.setLabel(getLabel());
        l.setSrcNode(getSrcNode());
        l.setDstNode(getDstNode());
        l.setBandwidth(bandwidth);
        return l;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceLink that = (ServiceLink) o;
        return label.equals(that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }

}
