/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.SigarException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Fully responsible for starting and stopping the HQ server
 * @author jhickey
 * 
 */
@Service
public class HQServer {

    private final Log log = LogFactory.getLog(HQServer.class);
    private String serverHome;  
    private EmbeddedDatabaseController embeddedDatabaseController;
    private ServerConfigurator serverConfigurator;
    private EngineController engineController;
    private OperatingSystem osInfo;


    @Autowired
    public HQServer(@Value("#{ systemProperties['server.home'] }") String serverHome,
                    EmbeddedDatabaseController embeddedDatabaseController,
                    ServerConfigurator serverConfigurator, EngineController engineController,
                    OperatingSystem osInfo) {
        this.serverHome = serverHome;
        this.embeddedDatabaseController = embeddedDatabaseController;
        this.serverConfigurator = serverConfigurator;
        this.engineController = engineController;
        this.osInfo = osInfo;
    }

    public void start() {
        log.info("Starting HQ server...");
        try {
            if (engineController.isEngineRunning()) {
                return;
            }
        } catch (SigarException e) {
            log.error("Unable to determine if HQ server is already running.  Cause: " +
                      e.getMessage());
            return;
        }
        try {
            serverConfigurator.configure();
        } catch (Exception e) {
            log.error("Error configuring server: " + e.getMessage());
        }
        if (embeddedDatabaseController.shouldUse()) {
            log.debug("Calling startBuiltinDB");
            try {
                if (!embeddedDatabaseController.startBuiltInDB()) {
                    // We couldn't start DB and not already running. Time to
                    // bail.
                    return;
                }
            } catch (Exception e) {
                log.error("Error starting built-in database: " + e.getMessage());
                return;
            }
            log.debug("startBuiltinDB completed");
        }
        List<String> javaOpts = getJavaOpts();
        log.info("Booting the HQ server...");
        engineController.start(javaOpts);
    }

    public void stop() {
        log.info("Stopping HQ server...");
        try {
            engineController.stop();
        } catch (Exception e) {
            log.error("Error stopping HQ server: " + e.getMessage());
            return;
        }
        if (embeddedDatabaseController.shouldUse()) {
            try {
                embeddedDatabaseController.stopBuiltInDB();
            } catch (Exception e) {
                log.error("Error stopping built-in database: " + e.getMessage());
                return;
            }
        }
        return;
    }

    List<String> getJavaOpts() {
        String javaOpts = serverConfigurator.getServerProps().getProperty("server.java.opts");
        String[] opts = javaOpts.split("\\s+");
        List<String> optList = new ArrayList<String>(Arrays.asList(opts));
        optList.add("-Dserver.home=" + serverHome);
        if ("SunOS".equals(osInfo.getName()) && osInfo.getArch().contains("64-bit")) {
            log.info("Setting -d64 JAVA OPTION to enable SunOS 64-bit JRE");
            optList.add("-d64");
        }
        return optList;
    }

   
    public static void main(String[] args) {
        ClassPathXmlApplicationContext appContext = null;
        try {
            appContext = new ClassPathXmlApplicationContext(
                new String[] { "classpath*:/META-INF/spring/bootstrap-context.xml" });
        } catch (Exception e) {
            System.err.println("Error initializing bootstrap class: " + e.getMessage());
            return;
        }
        HQServer server = appContext.getBean(HQServer.class);
        if ("start".equals(args[0])) {
            server.start();
        } else if ("stop".equals(args[0])) {
            server.stop();
        } else {
            System.err.println("Usage: HQServer {start|stop}");
        }
    }
}
