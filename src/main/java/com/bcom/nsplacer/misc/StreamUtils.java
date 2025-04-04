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

import com.bcom.nsplacer.config.HttpInterceptor;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class StreamUtils {

    public static void responseOnHtml(HttpServletResponse response, String msg, boolean center) throws IOException {
        String page = readString(new File(HttpInterceptor.resourcePath + "/response.html"));
        if (center) {
            page = page.replace("Here-goes-the-response", "<pre style=\"border: none; text-align: center;\"><code>" + msg + "</code></pre>");
        } else {
            page = page.replace("Here-goes-the-response", "<pre style=\"border: none;\"><code>" + msg + "</code></pre>");
        }
        response.setContentType("text/html");
        response.getOutputStream().write(page.getBytes());
        response.setStatus(HttpServletResponse.SC_OK);
    }

    public static void writeString(String str, File file) throws IOException {
        writeBytes(str.getBytes(StandardCharsets.UTF_8), file);
    }

    public static void writeBytes(byte[] b, File file) throws IOException {
        OutputStream os = Files.newOutputStream(file.toPath());
        os.write(b);
        os.flush();
        os.close();
    }

    public static String readString(File file) throws IOException {
        return new String(readBytes(file), StandardCharsets.UTF_8);
    }

    public static String readString(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        copy(is, os, false, false);
        return new String(os.toByteArray(), StandardCharsets.UTF_8);
    }

    public static byte[] readBytes(File file) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        copy(Files.newInputStream(file.toPath()), os, true, true);
        return os.toByteArray();
    }

    public static void copy(InputStream is, OutputStream os, boolean closeInput, boolean closeOutput) throws IOException {
        byte[] b = new byte[10000];
        while (true) {
            int r = is.read(b);
            if (r < 0) {
                break;
            }
            os.write(b, 0, r);
        }
        if (closeInput) {
            is.close();
        }
        if (closeOutput) {
            os.flush();
            os.close();
        }
    }

    public static int getFreeMemPercent() {
        return (int) ((double) Runtime.getRuntime().freeMemory() / Runtime.getRuntime().totalMemory() * 100.0);
    }
}
