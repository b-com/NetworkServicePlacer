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
package com.bcom.nsplacer.service;

import com.bcom.nsplacer.model.BaseModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author masoud
 */
public abstract class BaseService<E extends BaseModel> {

    private final JpaRepository<E, String> repo;

    public BaseService(JpaRepository<E, String> repo) {
        this.repo = repo;
    }

    public List<E> list() {
        List<E> all = repo.findAll();
        Collections.sort(all);
        return all;
    }

    public E create(E e) {
        return repo.save(e);
    }

    public E read(String id) {
        Optional<E> findById = repo.findById(id);
        return findById.orElse(null);
    }

    public E update(E e) {
        return repo.save(e);
    }

    public void delete(String id) {
        repo.deleteById(id);
    }

    public void deleteAll() {
        repo.deleteAll();
    }
}
