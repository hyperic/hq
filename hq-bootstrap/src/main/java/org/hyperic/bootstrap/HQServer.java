package org.hyperic.bootstrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SysInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HQServer {

    private final Log log = LogFactory.getLog(HQServer.class);
    private String serverHome;
    private String serverPidFile;
    private ProcessManager processManager;

    private ServerConfigurator serverConfigurator;
    private EngineController engineController;
    private SysInfo osInfo;
    private static final long DEFAUT_DB_PORT = 5432;

    @Autowired
    public HQServer(@Value("#{ systemProperties['server.home'] }") String serverHome,
                    @Value("#{ systemProperties['server.pidfile'] }") String serverPidFile,
                    ProcessManager processManager, ServerConfigurator serverConfigurator,
                    EngineController engineController, SysInfo osInfo) {
        this.serverHome = serverHome;
        this.serverPidFile = serverPidFile;
        this.processManager = processManager;
        this.serverConfigurator = serverConfigurator;
        this.engineController = engineController;
        this.osInfo = osInfo;
    }

    public int start() {
        try {
            if (isServerAlreadyRunning()) {
                return 0;
            }
        } catch (SigarException e) {
            log.error("Unable to determine if HQ server is already running", e);
            return 1;
        }
        try {
            serverConfigurator.configure();
        } catch (Exception e) {
            log.error("Error configuring server", e);
        }
        if (new File(serverHome + "/hqdb").exists()) {
            log.debug("Calling startBuiltinDB");
            try {
                if (!startBuiltInDB()) {
                    // We couldn't start DB and not already running. Time to
                    // bail.
                    return 1;
                }
            } catch (Exception e) {
                log.error("Error starting built-in database", e);
                return 1;
            }
            log.debug("startBuiltinDB completed");
        }
        upgradeDB();
        List<String> javaOpts = getJavaOpts();
        engineController.start(javaOpts);
        log.debug("Waiting for webapp port to come up...");
        if (!processManager.isPortInUse(Long.valueOf(serverConfigurator.getServerProps()
            .getProperty("server.webapp.port")), 90)) {
            log.error("HQ failed to start");
            return 1;
        }
        return 0;
    }

    boolean startBuiltInDB() throws SigarException, IOException {
        String dbPidFile = serverHome + "/hqdb/data/postmaster.pid";
        log
            .debug("Checking if HQ built-in database is already running using pidfile: " +
                   dbPidFile);
        long dbPid = processManager.getPidFromPidFile(dbPidFile);
        if (dbPid != -1) {
            log.info("HQ built-in database already running (pid file found: " + dbPidFile +
                     "), not starting it again.");
            return true;
        }
        boolean fileDeleted = new File(dbPidFile).delete();
        if (fileDeleted) {
            log.info("Removed stale pid file " + dbPidFile);
        }
        log.info("Starting HQ built-in database...");
        if (!(OperatingSystem.IS_WIN32)) {
            processManager.executeProcess(new String[] { serverHome + "/bin/db-start.sh" },
                serverHome);
        } else {
            // TODO
        }
        if (!processManager.isPortInUse(getDBPort(), 10)) {
            log.error("HQ built-in database failed to start");
            log.error("The log file " + serverHome +
                      "/hqdb/data/hqdb.log may contain further details on why it failed to start");
            return false;
        }
        log.info("HQ built-in database started.");
        return true;
    }

    long getDBPort() throws IOException {
        log.debug("loading dbport...");
        long port = HQServer.DEFAUT_DB_PORT;
        FileInputStream fi = null;
        File confFile = new File(serverHome + "/hqdb/data/postgresql.conf");
        if (!confFile.exists()) {
            log.warn("No postgresql.conf file found.  Assuming default port " + port);
        }

        try {
            fi = new FileInputStream(confFile);
            Properties props = new Properties();
            props.load(fi);
            if (props.getProperty("port") != null) {
                // postgres puts a comment on the same line as the prop value.
                String fullPropString = props.getProperty("port");
                String portStr = fullPropString.split("#")[0];
                try {
                    port = Long.valueOf(portStr.trim());
                } catch (NumberFormatException e) {
                    log.warn("Error getting built-in DB port from config file: " + e.getMessage() +
                             ". Will try default port: " + HQServer.DEFAUT_DB_PORT);
                }
            }
        } finally {
            if (fi != null)
                fi.close();
        }
        log.debug("loaded dbport=" + port);
        return port;
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
        log.info("Verify HQ database schema...");
        return processManager.executeProcess(
            new String[] { serverHome + "/bin/ant",
                          "--noconfig",
                          "-q",
                          "-Dserver.home=" + serverHome,
                          "-logger",
                          "org.hyperic.tools.ant.installer.InstallerLogger",
                          "-f",
                          serverHome + "/data/db-upgrade.xml",
                          "upgrade" }, serverHome);

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
        System.err.println("Usage: HQServer {start|stop}");
        System.exit(1);
    }
}
