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
package com.bcom.nsplacer.heroku;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

@Getter
@Setter
@NoArgsConstructor
public class WSConnection {

    // General
    private WSocketType type;
    private WebSocketSession wss;

    // For publisher-subscriber
    private String topic;

    // For WebRTC
    private String webrtcName;
    private String webrtcOtherName;

    // For Meeting
    private boolean openToJoin = false;
    private WSConnection meetingOtherSide;

    WSConnection(WebSocketSession wss) {
        this.wss = wss;
    }
}
