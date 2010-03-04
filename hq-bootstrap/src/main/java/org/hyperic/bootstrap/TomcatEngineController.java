package org.hyperic.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.OperatingSystem;
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
    private String engineHome;
    private String serverHome;
    private String serverPidFile;
    private final Log log = LogFactory.getLog(TomcatEngineController.class);
    private OperatingSystem osInfo;

    @Autowired
    public TomcatEngineController(
                                  ProcessManager processManager,
                                  @Value("#{ systemProperties['engine.home'] }") String engineHome,
                                  @Value("#{ systemProperties['server.home'] }") String serverHome,
                                  @Value("#{ systemProperties['server.pidfile'] }") String serverPidFile,
                                  OperatingSystem osInfo) {
        this.processManager = processManager;
        this.engineHome = engineHome;
        this.serverHome = serverHome;
        this.serverPidFile = serverPidFile;
        this.osInfo = osInfo;
    }

    public int start(List<String> javaOpts) {
        List<String> catalinaOpts = new ArrayList<String>(javaOpts);
        catalinaOpts.add("-Dcatalina.config=file://" + engineHome +
                         "/hq-server/conf/hq-catalina.properties");
        catalinaOpts.add("-Dcom.sun.management.jmxremote");
        StringBuffer catalinaOptString = new StringBuffer();
        for (String catalinaOpt : catalinaOpts) {
            catalinaOptString.append(catalinaOpt).append(" ");
        }
        if (!OperatingSystem.isWin32(osInfo.getName())) {
            return processManager.executeProcess(new String[] { engineHome +
                                                                "/hq-server/bin/startup.sh" },
                serverHome, new String[] { "JAVA_OPTS=",
                                          "CATALINA_OPTS=" + catalinaOptString.toString().trim(),
                                          "CATALINA_PID=" + serverPidFile }, true);
        }
        // TODO windows
        throw new UnsupportedOperationException();
    }

    public int stop() throws SigarException {
        long serverPid = processManager.getPidFromPidFile(serverPidFile);
        if (serverPid == -1) {
            log.info("HQ server not running");
            return 0;
        }
        processManager.kill(serverPid);
        return 0;
    }

    public void halt() throws SigarException {
        if (!OperatingSystem.isWin32(osInfo.getName())) {
            processManager.executeProcess(new String[] { engineHome + "/hq-server/bin/shutdown.sh",
                                                        "-force" }, serverHome,
                new String[] { "CATALINA_PID=" + serverPidFile }, true);
            return;
        }
        // TODO windows
        throw new UnsupportedOperationException();
    }

}
