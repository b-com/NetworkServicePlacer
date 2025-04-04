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
package com.bcom.nsplacer.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@MappedSuperclass
public class BaseModel implements Comparable<BaseModel> {

    @Id
    @Column
    private String id;

    @Column
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    private Date mdate;

    @PrePersist
    public void onPrePersist() {
        id = "" + UUID.randomUUID();
        mdate = new Date();
    }

    @PreUpdate
    public void onPreUpdate() {
        mdate = new Date();
    }

    @Override
    public int compareTo(BaseModel o) {
        return mdate.compareTo(o.mdate);
    }
}
