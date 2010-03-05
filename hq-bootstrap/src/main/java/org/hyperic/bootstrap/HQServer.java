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
    private ProcessManager processManager;
    private EmbeddedDatabaseController embeddedDatabaseController;
    private ServerConfigurator serverConfigurator;
    private EngineController engineController;
    private OperatingSystem osInfo;
    static final int DB_UPGRADE_PROCESS_TIMEOUT = 60 * 1000;

    @Autowired
    public HQServer(@Value("#{ systemProperties['server.home'] }") String serverHome,
                    ProcessManager processManager,
                    EmbeddedDatabaseController embeddedDatabaseController,
                    ServerConfigurator serverConfigurator, EngineController engineController,
                    OperatingSystem osInfo) {
        this.serverHome = serverHome;
        this.processManager = processManager;
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
        upgradeDB();
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

    int upgradeDB() {
        log.info("Verifying HQ database schema...");
        return processManager.executeProcess(
            new String[] { "java",
                          "-cp",
                          serverHome + "/lib/ant-launcher.jar",
                          "-Dserver.home=" + serverHome,
                          "-Dant.home=" + serverHome,
                          "org.apache.tools.ant.launch.Launcher",
                          "-q",
                          "-lib",
                          serverHome + "/lib",
                          "-logger",
                          "org.hyperic.tools.ant.installer.InstallerLogger",
                          "-buildfile",
                          serverHome + "/data/db-upgrade.xml",
                          "upgrade" }, serverHome, true, HQServer.DB_UPGRADE_PROCESS_TIMEOUT);
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
