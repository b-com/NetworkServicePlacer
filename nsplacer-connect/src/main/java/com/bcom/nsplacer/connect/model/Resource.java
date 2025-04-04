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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Random;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class Resource {

    private int amount;
    private int initialAmount;

    public Resource(Integer amount, Integer max) {
        this.amount = amount;
        this.initialAmount = max;
    }

    public void addAmount(int v) {
        amount += v;
    }

    public Resource clone(boolean reset) {
        return new Resource(reset ? initialAmount : amount, initialAmount);
    }

    public void setRandomValues(Random random) {
        setAmount(Math.abs(random.nextInt()) % (initialAmount + 1));
    }

    @JsonIgnore
    public boolean isFeasible() {
        return amount >= 0 && amount <= initialAmount;
    }

    @JsonIgnore
    public double getRemainingPercent() {
        return (double) amount / initialAmount * 100.0;
    }
}
