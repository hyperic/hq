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

import java.io.File;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
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
import org.hyperic.util.exec.ShutdownType;
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
    private final String serverHome;
    private final String engineHome;
    private final ProcessManager processManager;
    private final EmbeddedDatabaseController embeddedDatabaseController;
    private final ServerConfigurator serverConfigurator;
    private final EngineController engineController;
    private final OperatingSystem osInfo;
    private final DataSource dataSource;
    
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

    /**
     * @return {@link System#exit(int)} exit code 
     */
    public int start() {
        log.info("Starting HQ server...");
        try {
            if (engineController.isEngineRunning()) {
                return ShutdownType.AbnormalStop.exitCode() ;
            }
        } catch (SigarException e) {
            log.error("Unable to determine if HQ server is already running.  Cause: " + e, e);
            return ShutdownType.AbnormalStop.exitCode() ; 
        }
        try {
            serverConfigurator.configure();
        } catch (Exception e) {
            log.error("Error configuring server: " + e, e);
        }
        if (embeddedDatabaseController.shouldUse()) {
            log.debug("Calling startBuiltinDB");
            try {
                if (!embeddedDatabaseController.startBuiltInDB()) {
                    // We couldn't start DB and not already running. Time to
                    // bail.
                    return ShutdownType.AbnormalStop.exitCode() ;  
                }
                
                //Add a JVM shutdown hook to ensure that the db is always stopped 
                log.debug("Latching the cleaning process on the JVM's shutdown hook") ; 
                Runtime.getRuntime().addShutdownHook(new Thread() { 
                    @Override
                    public final void run() {
                        try {
                            embeddedDatabaseController.stopBuiltInDB();
                        } catch (Exception e) {
                            log.error("Error stopping built-in database: " + e, e);
                            return;
                        }//EO catch block
                    }//EOM  
                }) ;
            } catch (Exception e) {
                log.error("Error starting built-in database: " + e, e);
                return ShutdownType.AbnormalStop.exitCode() ; 
            }
            log.debug("startBuiltinDB completed");
        }
        log.info("Verifying HQ database schema...");

        try {
            upgradeDB();
        } catch (Throwable e) {
            log.error("Error running database upgrade routine: " + e, e);
            return ShutdownType.AbnormalStop.exitCode() ;
        }
        
        if (!(verifySchema())) {
            // Schema is not valid. Something went wrong with the DB upgrade.
            return ShutdownType.AbnormalStop.exitCode() ;
        }                                                                                           
        List<String> javaOpts = getJavaOpts();
        log.info("Booting the HQ server...");
        return engineController.start(javaOpts);
    }

    public void stop() {
        log.info("Stopping HQ server...");
        try {
            engineController.stop();
        } catch (Throwable e) {
            log.error("Error stopping HQ server: " + e, e);
            return;
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
        final String sInconsistentStateErrorMsgTemplate = "HQ DB schema is in a bad state: '%s'." + 
                " This is most likely due to a failed upgrade.  " +
                "Please either restore from backups and start your " +
                "previous version of HQ or contact HQ support.  " +
                "HQ cannot start while the current DB Schema version " +
                "is in this state" ; 
        
        boolean isSuccessful = false ;   
        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            final String sql = "select propvalue from EAM_CONFIG_PROPS " + "WHERE propkey = '" +
                               HQConstants.SchemaVersion + "'";
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                final String currSchema = rs.getString("propvalue");
                log.info("HQ DB schema: " + currSchema);
                if (currSchema.contains(HQConstants.SCHEMA_MOD_IN_PROGRESS)) {
                    log.fatal(String.format(sInconsistentStateErrorMsgTemplate, currSchema));
                }else { 
                    isSuccessful = true ; 
                }//EO else if DB version is in a consistent state 
            }else { 
                log.fatal(String.format(sInconsistentStateErrorMsgTemplate, "No CAM_SCHEMA_VERSION property found"));
            }//EO else if there was no CAM_SCHEMA_VERSION proprety 
            
        } catch (SQLException e) {
            try {
                DatabaseMetaData metaData = (conn == null) ? null : conn.getMetaData();
                String url = (metaData == null) ? null : metaData.getURL();
                log.error("Error verifying if HQ schema is valid.  url= " + url + ", Cause: " + e, e);
            } catch (SQLException e1) {
                log.error(e,e);
            }
        } finally {
            DBUtil.closeJDBCObjects(HQServer.class.getName(), conn, stmt, rs);
        }
        return isSuccessful ; 
    }

    int upgradeDB() throws Throwable{
        String logConfigFileUrl;
        try {
            logConfigFileUrl = new File(serverHome + "/conf/log4j.xml").toURI().toURL().toString();
        } catch (MalformedURLException e) {
            log.error("Unable to determine URL for logging config file " + serverHome +
                      "/conf/log4j.xml.  Cause: " + e, e);
            throw e ; 
        }

        String javaHome = System.getProperty("java.home");
         
        final int returnCode =  processManager.executeProcess(
            new String[] { javaHome + "/bin/java",
                          "-cp",
                          serverHome + "/lib/ant-launcher-1.7.1.jar",
                          "-Dserver.home=" + serverHome,
                          "-Dant.home=" + serverHome,
                          "-Dtomcat.home=" + engineHome + "/hq-server",
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
        
        if(returnCode == 1) { 
            throw new IllegalStateException("An Error had occured during an attempt to upgrade the database aborting (process exeuction returned a 1 return code)") ; 
        }else return returnCode ; 
    }

    public static void main(String[] args) {
    	
    	int iReturnCode = -1 ; 
    	
    	ClassPathXmlApplicationContext appContext = null;
        try {
            appContext = new ClassPathXmlApplicationContext(
                new String[] { "classpath*:/META-INF/spring/bootstrap-context.xml" });
        } catch (Exception e) { 
            System.err.println("Error initializing bootstrap class: " + e.toString() + " " + e.getMessage());						
            e.printStackTrace() ; 
            return;
        }
        HQServer server = appContext.getBean(HQServer.class);
        if ("start".equals(args[0])) {
        	iReturnCode = server.start();
        } else if ("stop".equals(args[0])) {
            server.stop();
            iReturnCode = ShutdownType.NormalStop.exitCode() ; 
            //return ;  
        } else {
            System.err.println("Usage: HQServer {start|stop}");
        }			
        
        //delegate the shutdown behavior to the ShutdownType strategies.
        final ShutdownType enumShutdownType = ShutdownType.reverseValueOf(iReturnCode) ;
        System.out.println("[HQServer.main("+args[0]+")]: Shutdown of type '" + enumShutdownType + 
        																		"' was request") ;
        enumShutdownType.shutdown() ; 
        
    }
}
