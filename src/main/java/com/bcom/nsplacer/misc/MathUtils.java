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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MathUtils {

    public static <E extends Comparable<E>> List<E> quartile(List<E> values) {
        List<E> arr = new ArrayList<>(values);
        Collections.sort(arr);
        List<E> list = new ArrayList<>();
        if (!values.isEmpty()) {
            list.add(arr.get(0));
            list.add(arr.get((int) (arr.size() * 0.25)));
            list.add(arr.get((int) (arr.size() * 0.50)));
            list.add(arr.get((int) (arr.size() * 0.75)));
            list.add(arr.get(arr.size() - 1));
        }
        return list;
    }

    public static <E> List<Double> toDoubleArray(List<E> values) {
        List<Double> list = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            list.add(Double.parseDouble("" + values.get(i)));
        }
        return list;
    }

    public static double average(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        return sum(values) / values.size();
    }

    public static double sum(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (int i = 0; i < values.size(); i++) {
            sum += values.get(i);
        }
        return sum;
    }

    public static double min(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        double min = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            min = Math.min(min, values.get(i));
        }
        return min;
    }

    public static double max(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        double max = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            max = Math.max(max, values.get(i));
        }
        return max;
    }

    public static double variance(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        double avg = average(values), sum = 0.0;
        for (double d : values) {
            sum += (avg - d) * (avg - d);
        }
        return sum / values.size();
    }
}
