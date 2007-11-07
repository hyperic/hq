/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Bootstraps the JBoss environment by:
 * <ol>
 *  <li> Copying template config files from a special directory to their 
 *       ultimate desinations in JBoss, as specified by the conf.map file.</li>
 *
 *  <li> Performing property replacement on the destination files, as
 *       specified by properties in the hq-server.conf file.</li>
 *
 *  <li> Copying the license file to its proper place.
 * </ol>
 */
public class HQMain {

    // An array of server config properties for backwards compatibility.  On
    // an upgrade scenario these properties will not be present in the
    // hq-server.conf, and need to be defined for proper template substitution.
    private static final String[][] COMPAT_PROPS = {
        { "server.jms.highmemory", "350" },
        { "server.jms.maxmemory", "400" },
        { "server.database-minpoolsize", "5" },
        { "server.database-maxpoolsize", "500" },
        { "server.database-blockingtimeout", "5000" },
        { "server.database-idletimeout", "15"}
    };

    private static final String META
        = "META-INF" + File.separator + "jboss-service.xml";

    private String serverHome;
    private String engineHome;
    private Properties symbols;
    private String deployDir = null;
    private String confDir = null;

    private HQMain (String serverHome, String engineHome) {
        this.serverHome = serverHome;
        this.engineHome = engineHome;
    }

    public static void initialize (String serverHome,
                                   String engineHome) throws Exception {
        HQMain m = new HQMain(serverHome, engineHome);
        m.intializeSymbols();
        m.loadSymbolFile();
        m.copyConfigFiles();
        m.copyLicenseFile();
    }

    private void intializeSymbols()
    {
        symbols = new Properties();
        for (int i = 0; i < COMPAT_PROPS.length; i++) {
            symbols.put(COMPAT_PROPS[i][0], COMPAT_PROPS[i][1]);
        }
    }

    private void loadSymbolFile () throws IOException {

        FileInputStream fi = null;
        File symbolFile = new File
            (serverHome 
             + File.separator + "conf"
             + File.separator + "hq-server.conf");
        if (!symbolFile.exists()) {
            error("No hq-server.conf file found. Expected to find it at: "
                  + symbolFile.getPath());
        }

        try {
            fi = new FileInputStream(symbolFile);
            symbols.load(fi);

            //XXX: Hack for upgraded servers that use the embedded database
            String jdbcUrl = symbols.getProperty("server.database-url");
            if (jdbcUrl.startsWith("jdbc:postgresql:") &&
                !jdbcUrl.endsWith("?protocolVersion=2")) {
                symbols.setProperty("server.database-url",
                                    jdbcUrl + "?protocolVersion=2");
            }
        } finally {
            if (fi != null) fi.close();
        }
    }

    private void copyConfigFiles () throws IOException {
        
        // System.err.println("Copying config files to: " + engineHome);

        Properties confMap = new Properties();
        FileInputStream fi = null;
        FileOutputStream fo = null;

        // Find conf.map in ${server.home}/conf/templates/conf.map
        File confMapFile = new File
            (serverHome 
             + File.separator + "conf"
             + File.separator + "templates"
             + File.separator + "conf.map");
        if (!confMapFile.exists()) {
            error("No conf.map file found.  Expected to find it at: " 
                  + confMapFile.getPath());
        }

        // Read in properties
        try {
            fi = new FileInputStream(confMapFile);
            confMap.load(fi);
        } finally {
            if (fi != null) fi.close();
        }

        // "deploy" and "conf" must be defined
        deployDir = confMap.getProperty("deploy");
        if (deployDir == null) {
            error("No 'deploy' attribute found in conf.map file: "
                  + confMapFile.getPath());
        }
        confDir = confMap.getProperty("conf");
        if (confDir == null) {
            error("No 'conf' attribute found in conf.map file: "
                  + confMapFile.getPath());
        }

        // Substitute for ENGINE in conf and deploy
        deployDir = HQInitUtil.replace(deployDir, "${ENGINE}", engineHome);
        confDir   = HQInitUtil.replace(confDir,   "${ENGINE}", engineHome);

        // Iterate over properties, performing replacement for keywords
        // deploy, conf, META
        Enumeration srcFiles = confMap.propertyNames();
        String src;
        String dest;
        while (srcFiles.hasMoreElements()) {
            src = (String) srcFiles.nextElement();
            if (src.equals("conf") || src.equals("deploy")) continue;
            dest = confMap.getProperty(src);
            dest = HQInitUtil.replace(dest, "${conf}", confDir);
            dest = HQInitUtil.replace(dest, "${deploy}", deployDir);
            dest = HQInitUtil.replace(dest, "${META}", META);

            if (dest.endsWith("/")) dest += src;

            if (!File.separator.equals("/")) {
                dest = HQInitUtil.replace(dest, "/", File.separator);
            }

            // Copy to dest
            fi = null;
            fo = null;
            File srcFile = new File
                (serverHome 
                 + File.separator + "conf"
                 + File.separator + "templates"
                 + File.separator + src);
            if (!srcFile.exists()) error("File not found: " + src);
            try {
                fi = new FileInputStream(srcFile);
                fo = new FileOutputStream(dest);
                HQInitUtil.copyStream(fi, fo, symbols);

            } finally {
                if (fi != null) fi.close();
                if (fo != null) fo.close();
            }
        }
    }

    private void copyLicenseFile () throws IOException {
        File licenseFile = new File
            (serverHome 
             + File.separator + "conf" 
             + File.separator + "license.xml");
        if (!licenseFile.exists()) {
            return;
        }
        FileInputStream fi = null;
        FileOutputStream fo = null;
        try {
            fi = new FileInputStream(licenseFile);
            fo = new FileOutputStream
                (deployDir
                 + File.separator + "hq.ear"
                 + File.separator + "license"
                 + File.separator + "license.xml");
            HQInitUtil.copyStream(fi, fo);
        } finally {
            if (fi != null) fi.close();
            if (fo != null) fo.close();
        }
    }


    private void error(String s) {
        throw new IllegalStateException(s);
    }

    public static void initializeHQServer (String serverHome,
                                           String engineHome,
                                           String mainClass,
                                           String[] args) 
        throws Exception {

        HQMain.initialize(serverHome, engineHome);
        if (mainClass != null) {
            Class mclass = Class.forName(mainClass);
            Method mmethod
                = mclass.getMethod("main", new Class[] {args.getClass()});
            mmethod.invoke(null, new Object[] {args});
        }
    }

    /**
     * server.home and engine.home must be defined in the 
     * system properties.
     * If defined, mainClass defines the class to pass control on to
     */
    public static void main (String[] args) {
        try {
            initializeHQServer(System.getProperty("server.home"),
                               System.getProperty("engine.home"),
                               System.getProperty("mainClass"),
                               args);
        } catch (Exception e) {
            throw new IllegalStateException("Error initializing HQ server: "
                                            + e.getClass().getName() + ": "
                                            + e.getMessage());
        }
    }
}
