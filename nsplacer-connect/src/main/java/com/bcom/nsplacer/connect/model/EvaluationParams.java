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
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EvaluationParams {

    private String networkTopology;
    private String snCPU;
    private String snStorage;
    private String snBandwidth;
    private String snLatency;

    private String serviceTopology;
    private Integer serviceSize;
    private String sgCPU;
    private String sgStorage;
    private String sgBandwidth;
    private String sgLatency;

    private Integer timeout;
    private String routing;
    private String strategy;
    private String approach;
    private String terminationType;
    private Boolean shuffle;

}
