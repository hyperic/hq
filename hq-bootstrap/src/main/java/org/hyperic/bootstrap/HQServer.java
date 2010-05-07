package org.hyperic.bootstrap;

import java.io.File;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.jdbc.DBUtil;
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
    private String engineHome;
    private ProcessManager processManager;
    private EmbeddedDatabaseController embeddedDatabaseController;
    private ServerConfigurator serverConfigurator;
    private EngineController engineController;
    private OperatingSystem osInfo;
    private DataSource dataSource;
    static final int DB_UPGRADE_PROCESS_TIMEOUT = 60 * 1000;

    @Autowired
    public HQServer(@Value("#{ systemProperties['server.home'] }") String serverHome,
                    @Value("#{ systemProperties['engine.home'] }") String engineHome,
                    ProcessManager processManager,
                    EmbeddedDatabaseController embeddedDatabaseController,
                    ServerConfigurator serverConfigurator, EngineController engineController,
                    OperatingSystem osInfo, DataSource dataSource) {
        this.serverHome = serverHome;
        this.engineHome = engineHome;
        this.processManager = processManager;
        this.embeddedDatabaseController = embeddedDatabaseController;
        this.serverConfigurator = serverConfigurator;
        this.engineController = engineController;
        this.osInfo = osInfo;
        this.dataSource = dataSource;
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
        log.info("Verifying HQ database schema...");
        upgradeDB();
        if(!(verifySchema())) {
            //Schema is not valid.  Something went wrong with the DB upgrade.
            return;
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
    
  
    
    boolean verifySchema() {
            Statement stmt  = null;
            ResultSet rs    = null;
            Connection conn = null;
            try {
                conn = dataSource.getConnection();
                stmt = conn.createStatement();
                final String sql = "select propvalue from EAM_CONFIG_PROPS " +
                    "WHERE propkey = '" + HQConstants.SchemaVersion + "'";
                rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    final String currSchema = rs.getString("propvalue");
                    if (currSchema.contains(HQConstants.SCHEMA_MOD_IN_PROGRESS)) {
                        log.fatal("HQ DB schema is in a bad state: '" + currSchema +
                            "'.  This is most likely due to a failed upgrade.  " +
                            "Please either restore from backups and start your " +
                            "previous version of HQ or contact HQ support.  " +
                            "HQ cannot start while the current DB Schema version " +
                            "is in this state");
                       return false;
                    }
                }
            } catch (SQLException e) {
                log.error("Error verifying if HQ schema is valid.  Cause: " + e.getMessage());
            }  finally {
                DBUtil.closeJDBCObjects(HQServer.class.getName(), conn, stmt, rs);
            }
            return true;
    }

    int upgradeDB() {
        String logConfigFileUrl;
        try {
            logConfigFileUrl = new File(serverHome +
                             "/conf/log4j.xml").toURI().toURL().toString();
        } catch (MalformedURLException e) {
            log.error("Unable to determine URL for logging config file " + serverHome +
                             "/conf/log4j.xml.  Cause: " + e.getMessage());
            return 1;
        }
        
        String javaHome = System.getProperty("java.home");
        return processManager.executeProcess(
            new String[] { javaHome + "/bin/java",
                          "-cp",
                          serverHome + "/lib/ant-launcher.jar",
                          "-Dserver.home=" + serverHome,
                          "-Dant.home=" + serverHome,
                          "-Dtomcat.home="  + engineHome + "/hq-server",
                          "-Dlog4j.configuration=" + logConfigFileUrl,
                          "org.apache.tools.ant.launch.Launcher",
                          "-q",
                          "-lib",
                          serverHome + "/lib",
                          "-listener",
                          "org.apache.tools.ant.listener.Log4jListener",
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
