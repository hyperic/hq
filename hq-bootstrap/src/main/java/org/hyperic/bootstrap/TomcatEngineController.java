/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
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

package org.hyperic.bootstrap;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.SigarException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link EngineController} which controls the ASF Tomcat
 * instance that ships with open source HQ
 * @author jhickey
 * 
 */
@Service
public class TomcatEngineController implements EngineController {

    private ProcessManager processManager;
    private String catalinaHome;
    private String catalinaBase;
    private String serverHome;
    private final Log log = LogFactory.getLog(TomcatEngineController.class);
    private String ptql;

    @Autowired
    public TomcatEngineController(ProcessManager processManager,
                                  @Value("#{ systemProperties['engine.home'] }") String engineHome,
                                  @Value("#{ systemProperties['server.home'] }") String serverHome) {
        this.processManager = processManager;
        this.serverHome = serverHome;
        this.catalinaHome = engineHome + "/hq-server";
        this.catalinaBase = engineHome + "/hq-server";
        this.ptql = "State.Name.sw=java,Args.*.eq=-Dcatalina.base=" + catalinaBase;
    }

    public int start(List<String> javaOpts) {
        List<String> catalinaOpts = new ArrayList<String>(javaOpts);
        String configFileUrl;
        try {
            configFileUrl = new File(catalinaBase +
                             "/conf/hq-catalina.properties").toURI().toURL().toString();
        } catch (MalformedURLException e) {
            log.error("Unable to determine URL for config file " + catalinaBase +
                             "/conf/hq-catalina.properties.  Cause: " + e.getMessage());
            return 1;
        }
        catalinaOpts.add("-Dcatalina.config=" + configFileUrl);
        catalinaOpts.add("-Dcom.sun.management.jmxremote");
        catalinaOpts.add("-Djava.endorsed.dirs" + catalinaHome + "/endorsed");
        catalinaOpts.add("-Dcatalina.base=" + catalinaBase);
        catalinaOpts.add("-Dcatalina.home=" + catalinaHome);
        catalinaOpts.add("-Djava.io.tmpdir=" + catalinaBase + "/temp");
        catalinaOpts.add("-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager");
       

        String[] commandLine = new String[catalinaOpts.size() + 5];
        String classpath = catalinaHome + "/bin/bootstrap.jar";
        String javaHome = System.getProperty("java.home");
        commandLine[0] = javaHome + "/bin/java";
        commandLine[1] = "-cp";
        commandLine[2] = classpath;
        int index = 3;
        for (String catalinaOpt : catalinaOpts) {
            commandLine[index] = catalinaOpt;
            index++;
        }
        commandLine[index] = "org.apache.catalina.startup.Bootstrap";
        commandLine[index + 1] = "start";
        // This blocks on the tomcat process to keep the server process alive
        // until stop is called. Set suppressOutput=false to allow standard
        // out and error (e.g. thread dump) to be outputted.
        return processManager.executeProcess(commandLine, serverHome, false, -1);
    }

    public boolean stop() throws Exception {
        long serverPid = processManager.getPidFromProcQuery(ptql);
        if (serverPid == -1) {
            log.info("HQ server not running");
            return true;
        }
        processManager.kill(serverPid);
        if (!(processManager.waitForProcessDeath(60, serverPid))) {
            processManager.forceKill(serverPid);
            if (!processManager.waitForProcessDeath(1, serverPid)) {
                // the server really doesn't want to stop
                return false;
            }
        }
        return true;
    }

    public void halt() throws SigarException {
        long serverPid = processManager.getPidFromProcQuery(ptql);
        if (serverPid != -1) {
            processManager.forceKill(serverPid);
        }
    }

    public boolean isEngineRunning() throws SigarException {
        long serverPid = processManager.getPidFromProcQuery(ptql);
        if (serverPid == -1) {
            return false;
        }
        return true;
    }
}
