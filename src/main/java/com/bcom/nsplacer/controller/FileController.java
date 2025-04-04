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
package com.bcom.nsplacer.controller;

import com.bcom.nsplacer.misc.GraphUtils;
import com.bcom.nsplacer.misc.ParameterInitializer;
import com.bcom.nsplacer.misc.StreamUtils;
import com.bcom.nsplacer.model.FileEntry;
import com.bcom.nsplacer.placement.NetworkGraph;
import com.bcom.nsplacer.placement.ZooTopologyIO;
import com.bcom.nsplacer.service.FileEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileEntryService fileEntryService;

    @Autowired
    private ServletContext servletContext;

    public FileController() {
    }

    public MediaType getMediaType(String fileName) {
        try {
            String mimeType = servletContext.getMimeType(fileName);
            return MediaType.parseMediaType(mimeType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @GetMapping(value = "/download/{id}")
    public ResponseEntity<InputStreamResource> download(HttpServletRequest request, HttpServletResponse response, @PathVariable String id) {
        try {
            FileEntry info = fileEntryService.read(id);
            if (info == null) {
                throw new RuntimeException("File not found! Id = " + id);
            }
            MediaType mediaType = getMediaType(info.getName());
            InputStreamResource resource = new InputStreamResource(fileEntryService.getInputStream(info.getId()));
            return ResponseEntity.ok()
                                 .contentType(mediaType)
                                 .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; "
                            + "filename=\"" + info.getName() + "\"; "
                            + "filename*=UTF-8''" + URLEncoder.encode(info.getName(), "UTF-8").replace("+", "%20"))
                                 .contentLength(info.getLength())
                                 .body(resource);
        } catch (Exception ioex) {
            throw new RuntimeException("Exception while reading file: " + id);
        }
    }

    @PostMapping(value = "/upload")
    @ResponseBody
    public void uploadFile(HttpServletRequest request, @RequestParam("file") MultipartFile[] files) {
        for (MultipartFile file : files) {
            String id = null;
            try {
                id = saveFile(file.getOriginalFilename(), file.getInputStream());
            } catch (Exception e) {
            }
            if (id == null) {
                continue;
            }
            NetworkGraph graph = null;
            try {
                graph = ZooTopologyIO.fromXML(StreamUtils.readString(fileEntryService.getInputStream(id)), null, null, null, null);
            } catch (Exception e) {
                fileEntryService.delete(id);
            }
            if (graph != null) {
                GraphUtils.setRandomLayout(graph);
                GraphUtils.improveLayout(graph);
                saveFile(file.getOriginalFilename() + ".layout", new ByteArrayInputStream(GraphUtils.getLayout(graph).getBytes()));
            }
        }
    }

    public String saveFile(String fname, InputStream is) {
        String id = null;
        try {
            FileEntry info = new FileEntry();
            info.setName(fname);
            fileEntryService.create(info);
            id = info.getId();
            FileEntryService.FileDataOutputStream os = fileEntryService.getOutputStream(info.getId());
            StreamUtils.copy(is, os, true, true);
            info.setLength(os.length());
            fileEntryService.update(info);
            return id;
        } catch (IOException e) {
            if (id != null) {
                fileEntryService.delete(id);
            }
            return null;
        }
    }

    @GetMapping("/list")
    public List<FileEntry> list(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return fileEntryService.list().stream().filter(x -> x.getName().toLowerCase().endsWith(".xml")).sorted(Comparator.comparing(FileEntry::getName)).collect(Collectors.toList());
    }

    @GetMapping("/delete/{id}")
    public void delete(HttpServletRequest request, HttpServletResponse response, @PathVariable String id) {
        FileEntry fileEntry = fileEntryService.read(id);
        fileEntryService.delete(id);
        fileEntry = fileEntryService.readByName(fileEntry.getName() + ".layout");
        fileEntryService.delete(fileEntry.getId());
    }

    @GetMapping("/getLayout/{name}")
    public NetworkGraph getLayout(HttpServletRequest request, HttpServletResponse response, @PathVariable String name) throws IOException {
        ParameterInitializer availableCpu = new ParameterInitializer(false, 0, 0, 10);
        ParameterInitializer availableStorage = new ParameterInitializer(false, 0, 0, 10);
        ParameterInitializer availableBandwidth = new ParameterInitializer(false, 0, 0, 10);
        ParameterInitializer availableLatency = new ParameterInitializer(false, 0, 0, 1);
        NetworkGraph networkGraph = ZooTopologyIO.fromXML(StreamUtils.readString(fileEntryService.getInputStream(fileEntryService.readByName(name).getId())), availableCpu, availableStorage, availableBandwidth, availableLatency);

        FileEntry info = fileEntryService.readByName(name + ".layout");
        FileEntryService.FileDataInputStream inputStream = fileEntryService.getInputStream(info.getId());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        StreamUtils.copy(inputStream, os, true, true);
        String layout = new String(os.toByteArray());
        GraphUtils.setLayout(layout, networkGraph);

        return networkGraph;
    }

    @PostMapping("/setLayout/{name}")
    public void setLayout(HttpServletRequest request, HttpServletResponse response, @PathVariable String name, @RequestBody String layout) throws IOException {
        FileEntry info = fileEntryService.readByName(name + ".layout");
        fileEntryService.delete(info.getId());
        saveFile(name + ".layout", new ByteArrayInputStream(layout.getBytes()));
    }

    @GetMapping("/improveLayout/{name}")
    public NetworkGraph improveLayout(HttpServletRequest request, HttpServletResponse response, @PathVariable String name) throws IOException {
        NetworkGraph graph = getLayout(null, null, name);
        GraphUtils.improveLayout(graph);
        setLayout(null, null, name, GraphUtils.getLayout(graph));
        return graph;
    }

    @GetMapping("/randomLayout/{name}")
    public NetworkGraph randomLayout(HttpServletRequest request, HttpServletResponse response, @PathVariable String name) throws IOException {
        NetworkGraph graph = getLayout(null, null, name);
        GraphUtils.setRandomLayout(graph);
        GraphUtils.scaleLayout(graph);
        setLayout(null, null, name, GraphUtils.getLayout(graph));
        return graph;
    }

    @GetMapping("/rotateLayout/{name}")
    public NetworkGraph rotateLayout(HttpServletRequest request, HttpServletResponse response, @PathVariable String name) throws IOException {
        NetworkGraph graph = getLayout(null, null, name);
        GraphUtils.rotateLayout(graph);
        GraphUtils.scaleLayout(graph);
        setLayout(null, null, name, GraphUtils.getLayout(graph));
        return graph;
    }
}
