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

package org.hyperic.bootstrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sets up Tomcat config by:
 * <ol>
 * <li>Combining hq-server.conf and catalina.properties into a single
 * CATALINA_BASE/conf/hq-catalina.properties file.</li>
 * 
 * <li>Copying hq-server.conf to the HQ webapp for use by the application</li>
 * 
 * <li>Copying the license file to its proper place.
 * </ol>
 */
@Component
public class ServerConfiguratorImpl implements ServerConfigurator {

   
    private String serverHome;
    private String engineHome;
    private Properties serverProps = new Properties();

    @Autowired
    public ServerConfiguratorImpl(@Value("#{ systemProperties['server.home'] }") String serverHome,
                                  @Value("#{ systemProperties['engine.home'] }") String engineHome) {
        this.serverHome = serverHome;
        this.engineHome = engineHome;
    }

    public void configure() throws Exception {
        loadServerProps();
        loadCatalinaProps();
        exportEngineProps();
        copyServerConf();
        copyLoggingConf();
        copyLicenseFile();
    }

    public Properties getServerProps() {
        return this.serverProps;
    }


    private void loadServerProps() throws IOException {
        FileInputStream fi = null;
        File confFile = new File(serverHome + File.separator + "conf" + File.separator +
                                 "hq-server.conf");
        if (!confFile.exists()) {
            error("No hq-server.conf file found. Expected to find it at: " + confFile.getPath());
        }

        try {
            fi = new FileInputStream(confFile);
            serverProps.load(fi);
        } finally {
            if (fi != null)
                fi.close();
        }
    }

    private void loadCatalinaProps() throws IOException {
        FileInputStream fi = null;
        File catalinaProps = new File(engineHome + File.separator + "hq-server" + File.separator +
                                      "conf" + File.separator + "catalina.properties");
        if (!catalinaProps.exists()) {
            return;
        }
        try {
            fi = new FileInputStream(catalinaProps);
            serverProps.load(fi);
        } finally {
            if (fi != null)
                fi.close();
        }
    }

    private void exportEngineProps() throws IOException {
        Properties propsToExport = new Properties();
      
        for(Object prop: serverProps.keySet()) {
            if(prop != null && !(((String)prop).startsWith("server.database"))) {
                propsToExport.put(prop, serverProps.get(prop));
            }
        }
        FileOutputStream fo = null;
        try {
            fo = new FileOutputStream(engineHome + File.separator + "hq-server" + File.separator +
                                      "conf" + File.separator + "hq-catalina.properties");
            propsToExport.store(fo, null);
        } finally {
            if (fo != null) {
                fo.close();
            }
        }
    }

    private void copyLicenseFile() throws IOException {
        File licenseFile = new File(serverHome + File.separator + "conf" + File.separator +
                                    "license.xml");
        if (!licenseFile.exists()) {
            return;
        }
        FileInputStream fi = null;
        FileOutputStream fo = null;
        try {
            String targetLicenseDir = engineHome + File.separator + "hq-server" + File.separator +
                                      "webapps" + File.separator + "ROOT" + File.separator + "WEB-INF" + File.separator +
                                      "license";
            new File(targetLicenseDir).mkdir();
            fi = new FileInputStream(licenseFile);
            fo = new FileOutputStream(targetLicenseDir + File.separator + "license.xml");
            copyStream(fi, fo);
        } finally {
            if (fi != null)
                fi.close();
            if (fo != null)
                fo.close();
        }
    }

    private void copyServerConf() throws IOException {
        File confFile = new File(serverHome + File.separator + "conf" + File.separator +
                                 "hq-server.conf");
        FileInputStream fi = null;
        FileOutputStream fo = null;
        try {
            fi = new FileInputStream(confFile);
            fo = new FileOutputStream(engineHome + File.separator + "hq-server" + File.separator +
                                      "webapps" + File.separator + "ROOT" + File.separator +
                                      "WEB-INF" + File.separator + "classes" + File.separator +
                                      "hq-server.conf");
            copyStream(fi, fo);
        } finally {
            if (fi != null)
                fi.close();
            if (fo != null)
                fo.close();
        }
    }

    private void copyLoggingConf() throws IOException {
        // ../conf/server-log4j.xml should only be used by the HQ server
        File serverConfFile = new File(serverHome + File.separator + "conf" + File.separator +
                                       "server-log4j.xml");
        FileInputStream fi = null;
        FileOutputStream webAppLoggingConfig = null;
        
        try {
            fi = new FileInputStream(serverConfFile);
            webAppLoggingConfig = new FileOutputStream(engineHome + File.separator + "hq-server" + File.separator +
                                      "webapps" + File.separator + "ROOT" + File.separator +
                                      "WEB-INF" + File.separator + "classes" + File.separator +
                                      "log4j.xml");
            copyStream(fi, webAppLoggingConfig);
        } finally {
            if (fi != null) {
                fi.close();
            }
            if (webAppLoggingConfig != null) {
                webAppLoggingConfig.close();
            }
        }

       // ../conf/log4j.xml should only be used by Tomcat and the HQ bootstrap process
       File bootstrapConfFile = new File(serverHome + File.separator + "conf" + File.separator +
                                         "log4j.xml");
       FileInputStream conf = null;
       FileOutputStream engineLoggingConfig = null;
       try {
           conf= new FileInputStream(bootstrapConfFile);
           engineLoggingConfig = new FileOutputStream(engineHome +  File.separator + "hq-server" + File.separator + "lib" + File.separator +
                                     "log4j.xml");
           copyStream(conf, engineLoggingConfig);
       } finally {
           if (conf != null) {
               conf.close();
           }
           if (engineLoggingConfig != null) {
               engineLoggingConfig.close();
           }
       }
       
    }
    
    private void copyStream(InputStream is, OutputStream os) throws IOException {

        byte[] buf = new byte[2048];
        int bytesRead = 0;
        while (true) {
            bytesRead = is.read(buf);
            if (bytesRead == -1)
                break;
            os.write(buf, 0, bytesRead);
        }
    }

    private void error(String s) {
        throw new IllegalStateException(s);
    }

}
