/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.plugin.rabbitmq.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.util.config.ConfigResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Erlang will automatically create a random cookie file when the RabbitMQ server starts up.
 * This will be typically located in /var/lib/rabbitmq/.erlang.cookie  on Unix systems
 * and C:\Documents and Settings\Current User\Application Data\RabbitMQ\.erlang.cookie on Windows.
 * If it is a manual install it may be in user home.
 * @author Helena Edelson
 */
public class ErlangCookieHandler {

    private static final Log logger = LogFactory.getLog(ErlangCookieHandler.class);

    /**
     * @param conf
     * @return the content in the cookie
     */
    public static String configureCookie(ConfigResponse conf) {
        if (conf == null) {
            throw new IllegalArgumentException("ConfigResponse must not be null.");
        }

        String nodeCookie = null;

        File file = null;

        if (conf.getValue(DetectorConstants.NODE_COOKIE_VALUE) == null) {
            file = inferCookie(conf);
        } else {
            if (conf.getValue(DetectorConstants.NODE_COOKIE_LOCATION) != null) {
                String userSetLocation = conf.getValue(DetectorConstants.NODE_COOKIE_LOCATION).trim();
                if (userSetLocation != null && !userSetLocation.isEmpty()) {
                    file = new File(userSetLocation);
                }
            }
        }

        if (file != null && file.exists()) {
            nodeCookie = getErlangCookieValue(file);
            if (nodeCookie != null) logger.debug(nodeCookie + " " + file.getAbsolutePath());
        }

        return nodeCookie;
    }

    /**
     * Read in the erlang cookie string from the path
     * @param file
     * @return
     */
    protected static String getErlangCookieValue(File file) {
        logger.debug("Attempting to read file " + file);
        String erlangCookieValue = null;

        try {
            if (file != null && file.exists()) {
                BufferedReader in = new BufferedReader(new FileReader(file));
                erlangCookieValue = in.readLine();

                if (in != null) {
                    in.close();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Agent does not have permission to read the cookie: " + e.getMessage(), e);
        }

        return erlangCookieValue;
    }

    /**
     * Infer the cookie file by most likely location.
     * @param conf The ConfigResponse is from RabbitServerDetector.discoverServices
     * @return java.io.File '/path/to/.erlang.cookie'
     */
    public static File inferCookie(ConfigResponse conf) {

        final String fileName = ".erlang.cookie";

        final String platform = conf.getValue(DetectorConstants.PLATFORM_TYPE);

        final String home = System.getProperty("user.home");

        File file = new File("/var/lib/rabbitmq/" + fileName);

        if (file.exists()) {
            return file;
        } else {
            if (home != null) {
                /** for manual installs */
                file = new File(new StringBuilder(home).append(File.separator).append(fileName).toString());
                if (file.exists()) {
                    return file;
                }
                /** best attempt windows */
                else {
                    if (platform.equals(OperatingSystem.NAME_WIN32)) {
                        file = doWindows(fileName, home);
                    }
                }
            }
        }

        return file;
    }

    /**
     * Best attempt Windows
     * @param fileName
     * @return
     */
    private static File doWindows(String fileName, String home) {
        StringBuilder base = new StringBuilder("C:").append(File.separator);

        StringBuilder sb = new StringBuilder(base.toString()).append("Documents and Settings").append(File.separator);

        File a = new File(sb.append(home).append(File.separator).append("Application Data").append(File.separator)
                .append("RabbitMQ").append(File.separator).append(fileName).toString());

        if (a.exists()) {
            return a;
        } else {
            File b = new File(sb.append(home).append(File.separator).append(fileName).toString());

            if (b.exists()) {
                return b;
            } else {
                File c = new File(base.append("WINDOWS").append(File.separator).append(fileName).toString());
                return c.exists() ? c : null;
            }
        }
    }
}
