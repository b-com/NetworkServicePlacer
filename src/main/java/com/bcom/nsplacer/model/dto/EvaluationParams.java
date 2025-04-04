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
package com.bcom.nsplacer.model.dto;

import com.bcom.nsplacer.misc.ParameterInitializer;
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
    private String sgUsers;

    private Integer timeout;
    private String routing;
    private String strategy;
    private String approach;
    private String terminationType;
    private Boolean shuffle;
    private Boolean vneConst;

    public static ParameterInitializer toInitializer(String param) {
        param = param.trim().replace(" ", "");
        if (param.contains("-")) {
            int l = Integer.parseInt(param.substring(0, param.indexOf("-")));
            int h = Integer.parseInt(param.substring(param.indexOf("-") + 1));
            return new ParameterInitializer(true, l, h, 0);
        } else {
            return new ParameterInitializer(false, 0, 0, Integer.parseInt(param));
        }
    }

}
