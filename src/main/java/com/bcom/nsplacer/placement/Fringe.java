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

import java.util.*;

@Getter
@Setter
public class Fringe {

    private List<SearchState> list = new ArrayList<>();
    private boolean isQueue;
    private boolean shuffle;
    private int maxReachedSize = 0;

    public Fringe(boolean isQueue, boolean shuffle) {
        this.isQueue = isQueue;
        this.shuffle = shuffle;
    }

    public void put(List<SearchState> states, int maxFC) {
        for (int i = 0; i < states.size(); i++) {
            if (maxFC == -1) {
                list.add(states.get(i));
            } else {
                if ((int) Math.round(states.get(i).getObjectiveValue()) <= maxFC) {
                    list.add(states.get(i));
                }
            }
        }
        maxReachedSize = Math.max(maxReachedSize, list.size());
        if (isQueue) {
            if (shuffle) {
                Collections.shuffle(list);
            }
            Collections.sort(list);
        }
        //printLogs();
    }

    public void printLogs() {
        Map<Integer, Integer> depthCounter = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            int d = list.get(i).getDepth();
            if (!depthCounter.containsKey(d)) {
                depthCounter.put(d, 0);
            }
            depthCounter.put(d, depthCounter.get(d) + 1);
        }
        System.out.println("Fringe size: " + size());
        System.out.println("Fringe DepthCounter: " + depthCounter);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public int size() {
        return list.size();
    }

    public SearchState take() {
        if (isEmpty()) {
            return null;
        }
        return list.remove(list.size() - 1);
    }

}
