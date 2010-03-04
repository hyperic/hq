package org.hyperic.bootstrap;

import java.io.File;
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
    private String serverPidFile;
    private ProcessManager processManager;
    private EmbeddedDatabaseController embeddedDatabaseController;
    private ServerConfigurator serverConfigurator;
    private EngineController engineController;
    private OperatingSystem osInfo;
    int serverStopCheckRetries = 60;

    @Autowired
    public HQServer(@Value("#{ systemProperties['server.home'] }") String serverHome,
                    @Value("#{ systemProperties['server.pidfile'] }") String serverPidFile,
                    ProcessManager processManager,
                    EmbeddedDatabaseController embeddedDatabaseController,
                    ServerConfigurator serverConfigurator, EngineController engineController,
                    OperatingSystem osInfo) {
        this.serverHome = serverHome;
        this.serverPidFile = serverPidFile;
        this.processManager = processManager;
        this.embeddedDatabaseController = embeddedDatabaseController;
        this.serverConfigurator = serverConfigurator;
        this.engineController = engineController;
        this.osInfo = osInfo;
    }

    public int start() {
        log.info("Starting HQ server...");
        try {
            if (isServerAlreadyRunning()) {
                return 0;
            }
        } catch (SigarException e) {
            log.error("Unable to determine if HQ server is already running.  Cause: " + e.getMessage());
            return 1;
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
                    return 1;
                }
            } catch (Exception e) {
                log.error("Error starting built-in database: " + e.getMessage());
                return 1;
            }
            log.debug("startBuiltinDB completed");
        }
        upgradeDB();
        List<String> javaOpts = getJavaOpts();
        log.info("Booting the HQ server...");
        engineController.start(javaOpts);
        log.debug("Waiting for webapp port to come up...");
        long webAppPort = Long.valueOf(serverConfigurator.getServerProps().getProperty(
            "server.webapp.port"));
        try {
            if (!processManager.isPortInUse(webAppPort, 90)) {
                log.error("HQ failed to start");
                return 1;
            }
        } catch (Exception e) {
            log.error("Unable to determine if HQ server started: " + e.getMessage());
            return 1;
        }
        log.info("HQ server booted.");
        log.info("Login to HQ at: http://127.0.0.1:" + webAppPort + "/");
        return 0;
    }

    public int stop() {
        log.info("Stopping HQ server...");
        try {
            engineController.stop();
        } catch (SigarException e) {
            log.error("Error stopping HQ server: " + e.getMessage());
            return 1;
        }
        try {
            if (!isServerStopped(serverStopCheckRetries)) {
                // we asked nicely, now use force
                engineController.halt();
                if (!isServerStopped(1)) {
                    // the server really doesn't want to stop
                    return 1;
                }
            }
        } catch (Exception e) {
            log.error("Unable to determine if server is stopped: " + e.getMessage());
            return 1;
        }

        if (embeddedDatabaseController.shouldUse()) {
            try {
                embeddedDatabaseController.stopBuiltInDB();
            } catch (Exception e) {
                log.error("Error stopping built-in database: " + e.getMessage());
                return 1;
            }
        }
        return 0;
    }

    boolean isServerAlreadyRunning() throws SigarException {
        log.debug("Checking if server is already running using pidfile: " + serverPidFile);
        long pid = processManager.getPidFromPidFile(serverPidFile);
        if (pid != -1) {
            log.info("HQ server is already running");
            return true;
        }
        // remove old pid file if exists
        boolean pidFileRemoved = new File(serverPidFile).delete();
        if (pidFileRemoved) {
            log.info("Removed stale pid file " + serverPidFile);
        }
        return false;
    }

    boolean isServerStopped(int maxTries) throws Exception {
        long pid = -1;
        for (int i = 0; i < maxTries; i++) {
            log.debug("waitForPid: waiting for " + serverPidFile);
            pid = processManager.getPidFromPidFile(serverPidFile);
            if (pid == -1) {
                log.info("HQ server exited");
                // remove old pid file if exists
                new File(serverPidFile).delete();
                return true;
            }
            log.debug("waitForPid: PID " + pid + " still alive");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        log.debug("Num tries for server PID check exhausted");
        log.info("HQ server PID " + pid + " did not exit.");
        return false;
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
            new String[] { serverHome + "/bin/ant",
                          "--noconfig",
                          "-q",
                          "-Dserver.home=" + serverHome,
                          "-logger",
                          "org.hyperic.tools.ant.installer.InstallerLogger",
                          "-f",
                          serverHome + "/data/db-upgrade.xml",
                          "upgrade" }, serverHome, true);

    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext appContext = null;
        try {
            appContext = new ClassPathXmlApplicationContext(
                new String[] { "classpath*:/META-INF/spring/bootstrap-context.xml" });
        } catch (Exception e) {
            System.err.println("Error initializing bootstrap class: " + e.getMessage());
            System.exit(1);
        }
        HQServer server = appContext.getBean(HQServer.class);
        if ("start".equals(args[0])) {
            int exitCode = server.start();
            System.exit(exitCode);
        }
        if ("stop".equals(args[0])) {
            int exitCode = server.stop();
            System.exit(exitCode);
        }
        System.err.println("Usage: HQServer {start|stop}");
        System.exit(1);
    }
}
