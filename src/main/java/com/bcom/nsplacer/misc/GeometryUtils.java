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

import java.awt.*;

public class GeometryUtils {

    public static Point rotate(int x, int y, int cx, int cy, double theta) {
        Point point = new Point();
        x -= cx;
        y -= cy;
        point.x = (int) (x * Math.cos(theta) - y * Math.sin(theta)) + cx;
        point.y = (int) (x * Math.sin(theta) + y * Math.cos(theta)) + cy;
        return point;
    }

    public static double thata(int x1, int y1, int x2, int y2) {
        return (x1 == x2) ? (Math.PI / 2.0) : Math.atan(((double) y2 - y1) / (x2 - x1));
    }
}
