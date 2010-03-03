package org.hyperic.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.sigar.OperatingSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TomcatEngineController implements EngineController {

    private ProcessManager processManager;
    private String engineHome;
    private String serverHome;
    private String serverPidFile;
    
    
    @Autowired
    public TomcatEngineController(ProcessManager processManager, @Value("#{ systemProperties['engine.home'] }") String engineHome,
                                  @Value("#{ systemProperties['server.home'] }") String serverHome, @Value("#{ systemProperties['server.pidfile'] }") String serverPidFile) {
        this.processManager = processManager;
        this.engineHome = engineHome;
        this.serverHome = serverHome;
        this.serverPidFile = serverPidFile;
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
        if (!(OperatingSystem.IS_WIN32)) {
            return processManager.executeProcess(
                new String[] { engineHome + "/hq-server/bin/startup.sh" }, serverHome,
                new String[] { "JAVA_OPTS=",
                              "CATALINA_OPTS=" + catalinaOptString.toString(),
                              "CATALINA_PID=" + serverPidFile });
        }
        //TODO windows
        return 0;
    }

    public void stop() {
        // TODO Auto-generated method stub

    }

}
