package org.hyperic.bootstrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.SigarException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Impl of {@link EmbeddedDatabaseController} that controls our built-in
 * Postgres DB
 * @author jhickey
 * 
 */
@Service
public class PostgresEmbeddedDatabaseController implements EmbeddedDatabaseController {

    static final long DEFAUT_DB_PORT = 5432;
    static final int DB_PROCESS_TIMEOUT = 60 * 1000;
    private String serverHome;
    private ProcessManager processManager;
    private OperatingSystem osInfo;
    int dbPortStopCheckTries = 30;

    private final Log log = LogFactory.getLog(PostgresEmbeddedDatabaseController.class);

    @Autowired
    public PostgresEmbeddedDatabaseController(
                                              @Value("#{ systemProperties['server.home'] }") String serverHome,
                                              ProcessManager processManager, OperatingSystem osInfo) {
        this.serverHome = serverHome;
        this.processManager = processManager;
        this.osInfo = osInfo;
    }

    public boolean startBuiltInDB() throws SigarException, IOException {
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
        if (OperatingSystem.isWin32(osInfo.getName())) {
            // TODO Windows
            throw new UnsupportedOperationException();
        }
        processManager.executeProcess(new String[] { serverHome + "/bin/db-start.sh" }, serverHome,
            false, PostgresEmbeddedDatabaseController.DB_PROCESS_TIMEOUT);
        try {
            if (!processManager.isPortInUse(getDBPort(), 10)) {
                log.error("HQ built-in database failed to start");
                log.error("The log file " + serverHome +
                          "/logs/hqdb.log may contain further details on why it failed to start");
                return false;
            }
        } catch (Exception e) {
            log.error("Unable to determine if HQ built-in database started: " + e.getMessage());
            return false;
        }
        log.info("HQ built-in database started.");
        return true;
    }

    public boolean stopBuiltInDB() throws SigarException, IOException {
        String dbPidFile = serverHome + "/hqdb/data/postmaster.pid";
        log.debug("Checking if HQ built-in database is running using pidfile: " + dbPidFile);
        long dbPid = processManager.getPidFromPidFile(dbPidFile);
        if (dbPid == -1) {
            log.info("HQ built-in database not running");
            return true;
        }
        log.info("Stopping HQ built-in database...");
        if (OperatingSystem.isWin32(osInfo.getName())) {
            // TODO
            throw new UnsupportedOperationException();
        }
        processManager.executeProcess(new String[] { serverHome + "/bin/db-stop.sh" }, serverHome,
            false, PostgresEmbeddedDatabaseController.DB_PROCESS_TIMEOUT);
        try {
            if (!isDBStopped(dbPortStopCheckTries)) {
                log.error("HQ built-in database failed to stop");
                log.error("The log file " + serverHome +
                          "/logs/hqdb.log may contain further details on why it failed to stop");
                return false;
            }
        } catch (Exception e) {
            log.error("Unable to determine if database was stopped: " + e.getMessage());
            return false;
        }
        log.info("HQ built-in database stopped.");
        return true;
    }

    public long getDBPort() throws IOException {
        log.debug("loading dbport...");
        long port = PostgresEmbeddedDatabaseController.DEFAUT_DB_PORT;
        FileInputStream fi = null;
        File confFile = new File(serverHome + "/hqdb/data/postgresql.conf");
        if (!confFile.exists()) {
            log.warn("No postgresql.conf file found.  Assuming default port " + port);
            return port;
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
                             ". Will try default port: " +
                             PostgresEmbeddedDatabaseController.DEFAUT_DB_PORT);
                }
            }
        } finally {
            if (fi != null)
                fi.close();
        }
        log.debug("loaded dbport=" + port);
        return port;
    }

    public boolean shouldUse() {
        log.debug("checking hqdb dir exists: " + serverHome + "/hqdb");
        return new File(serverHome + "/hqdb").exists();
    }

    boolean isDBStopped(int maxTries) throws Exception {
        long dbPort = getDBPort();
        boolean dbPortInUse = true;
        for (int i = 0; i < maxTries; i++) {
            dbPortInUse = processManager.isPortInUse(dbPort, 1);
            if (dbPortInUse == false) {
                return true;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return false;
    }

}
