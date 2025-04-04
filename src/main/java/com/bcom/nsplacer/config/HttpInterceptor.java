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
package com.bcom.nsplacer.config;

import com.bcom.nsplacer.misc.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@Component
public class HttpInterceptor implements HandlerInterceptor {

    public static final String resourcePath = "./res";

    @Autowired
    private ServletContext servletContext;

    public MediaType getMediaType(String fileName) {
        try {
            String mimeType = servletContext.getMimeType(fileName);
            return MediaType.parseMediaType(mimeType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if ("".equals(uri) || "/".equals(uri)) {
            uri = "/index.html";
        }

        if (!uri.startsWith("/api/")) {
            returnFile(request, response, uri);
            return false;
        }
        return true;
    }

    private void returnFile(HttpServletRequest request, HttpServletResponse response, String uri) throws IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,DELETE,PUT,OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Allow-Credentials", "" + true);
        response.setHeader("Access-Control-Max-Age", "" + 600);
        response.setHeader("Cache-Control", "max-age=3600");

        File file = new File(HttpInterceptor.resourcePath + uri);
        if (!uri.contains("/../") && file.exists()) {
            response.setContentType("" + getMediaType(uri.substring(1)));
            byte[] bytes = StreamUtils.readBytes(file);
            response.getOutputStream().write(bytes);
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        StreamUtils.responseOnHtml(response, "Page not found! Page=" + uri, true);
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) throws Exception {
    }
}
