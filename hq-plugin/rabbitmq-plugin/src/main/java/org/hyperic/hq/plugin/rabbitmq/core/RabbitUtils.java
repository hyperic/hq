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
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * RabbitUtils
 * @author Helena Edelson
 */
public class RabbitUtils {

    private static final Log logger = LogFactory.getLog(RabbitUtils.class);

    private static final String NODE_KEY = "node.cookie";

    /**
     * 
     * @param conf
     * @return
     */
    public static String handleCookie(ConfigResponse conf) {
        String nodeCookie = null;

        /** If this is user input set it regardless if it is not null,
         * things may have changed, a new node added, etc. */
        String location = conf.getValue(NODE_KEY);

        /** To Do add more user entry validation of path */
        if (location != null && location.length() > 0) {
           nodeCookie = doHandleCookie(location);
        }

        /** We could have had location and still the cookie might be null.
         * if no value, automatically set it by making several best attempts */
        if (nodeCookie == null) {
            nodeCookie = doHandleCookie(conf);
        }

        logger.debug("RabbitProductPlugin: node.cookie set to " + nodeCookie);
        return nodeCookie;
    }

    /**
     * If the user enters the location, return the value.
     * @param location
     * @return
     */
    public static String doHandleCookie(String location) {
        return getErlangCookieValue(new File(location));
    }

    /**
     * Makes bet attempt. Can return null.
     * @param conf
     * @return
     */
    public static String doHandleCookie(ConfigResponse conf) {
        return getErlangCookieValue(inferCookie(conf));
    }

    /**
     * Read in the erlang cookie string from the path
     * @param cookie
     * @return
     */
    protected static String getErlangCookieValue(File cookie) {
        String erlangCookieValue = null;

        BufferedReader in = null;

        try {
            if (cookie != null) {
                if (cookie.exists()) {
                    in = new BufferedReader(new FileReader(cookie));
                    erlangCookieValue = in.readLine();
                    Assert.notNull("erlangData must not be null.", erlangCookieValue);
                    Assert.hasText(erlangCookieValue);
                }

                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            logger.error("Error reading the Erlang Cookie: " + e.getMessage(), e);
        }

        if (logger.isDebugEnabled()) logger.debug("erlangCookieValue=" + erlangCookieValue);

        return erlangCookieValue;
    }

    /**
     * Make a few attempts to acquire the cookie by most likely
     * location per OS.
     * @param conf
     * @return
     */
    private static File inferCookie(ConfigResponse conf) {
        File erlangCookie = null;

        final String fileName = ".erlang.cookie";

        final String platform = conf.getValue("platform.type");

        final String home = System.getProperty("user.home");

        /** best attempt generic */
        erlangCookie = doCrossPlatform(fileName, home);

        if (erlangCookie == null) {
            if (platform.equals(OperatingSystem.NAME_WIN32)) {
                erlangCookie = doWindows(fileName, home);
            } else {
                erlangCookie = doGenericUnixLinux(fileName, home);
            }
        }

        return erlangCookie;
    }

    /**
     * @param fileName
     * @param home
     * @return
     */
    private static File doCrossPlatform(String fileName, String home) {
        Assert.hasText(fileName, "fileName must not be null.");
        File a = null;

        if (home != null) {
            a = new File(new StringBuilder(home).append(File.separator).append(fileName).toString());
        }

        File b = new File(new StringBuilder(System.getProperty("user.dir")).append(File.separator).append(fileName).toString());

        return a != null && a.exists() ? a : (b.exists() ? b : null);
    }

    /**
     * Best attempt mac/linux/unix
     * @param fileName
     * @param home
     * @return
     */
    private static File doGenericUnixLinux(String fileName, String home) {
        Assert.hasText(fileName, "fileName must not be null.");

        File a = null;
        File b = new File("/var/lib/rabbitmq/" + fileName);
        File c = new File("/opt/local/var/lib/rabbitmq/" + fileName);

        if (home != null) {
            a = new File(home + fileName);
        }
        return a != null && a.exists() ? a : (b.exists() ? b : (c.exists() ? c : null));
    }

    /**
     * Best attempt Windows
     * @param fileName
     * @return
     */
    private static File doWindows(String fileName, String home) {
        File a = new File(new StringBuilder("C:").append(File.separator).append("WINDOWS")
                .append(File.separator).append(fileName).toString());

        File b = new File(new StringBuilder("C:").append(File.separator).append("Documents and Settings").append(File.separator)
                .append(home).append(File.separator).append(fileName).toString());

        return a.exists() ? a : (b.exists() ? b : null);
    }
}
