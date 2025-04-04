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
package com.bcom.nsplacer;

import com.bcom.nsplacer.config.HttpInterceptor;
import com.bcom.nsplacer.controller.FileController;
import com.bcom.nsplacer.dao.FileEntryDao;
import com.bcom.nsplacer.misc.GraphUtils;
import com.bcom.nsplacer.misc.StreamUtils;
import com.bcom.nsplacer.placement.NetworkGraph;
import com.bcom.nsplacer.placement.ZooTopologyIO;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
@EnableScheduling
public class NsPlacerApplication implements ApplicationContextAware {

    public static final String zooTopologiesDir = "./zoo-topologies";

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(NsPlacerApplication.class, args);
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static <T> T getBean(Class<T> requiredType) {
        return context.getBean(requiredType);
    }

    public static void startDatabase() {
        FileEntryDao fileEntryDao = context.getBean(FileEntryDao.class);

        if (fileEntryDao.count() == 0) {
            File[] zooTopologyFilesArray = new File(zooTopologiesDir).listFiles();
            FileController fileController = getBean(FileController.class);
            if (zooTopologyFilesArray != null && zooTopologyFilesArray.length != 0) {
                try {
                    List<File> zooTopologyFiles = new ArrayList<>(Arrays.asList(zooTopologyFilesArray));
                    zooTopologyFiles.sort(Comparator.comparing(File::getName));
                    for (File file : zooTopologyFiles) {
                        fileController.saveFile(file.getName(), Files.newInputStream(file.toPath()));
                        NetworkGraph graph = ZooTopologyIO.fromXML(StreamUtils.readString(Files.newInputStream(file.toPath())), null, null, null, null);
                        GraphUtils.setRandomLayout(graph);
                        GraphUtils.improveLayout(graph);
                        fileController.saveFile(file.getName() + ".layout", new ByteArrayInputStream(GraphUtils.getLayout(graph).getBytes()));
                    }
                } catch (IOException e) {
                }
            }
        }
    }

    @PostConstruct
    public void starter() {
        startDatabase();
    }

    @PreDestroy
    public void shutdownHook() {
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        context = (ConfigurableApplicationContext) ac;
    }

    @Bean(name = "multipartResolver")
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
        multipartResolver.setMaxUploadSize(10000000000l);
        return multipartResolver;
    }

    @Configuration
    public class InterceptorConfig extends WebMvcConfigurerAdapter {

        @Autowired
        HttpInterceptor serviceInterceptor;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(serviceInterceptor);
        }
    }
}
