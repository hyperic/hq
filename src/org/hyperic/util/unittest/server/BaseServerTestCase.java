/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.util.unittest.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import junit.framework.TestCase;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.hyperic.util.jdbc.DBUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * The test case that all server-side unit tests should extend. Before starting 
 * the server, the user must set the <code>UNITTEST_JBOSS_HOME</code> environment 
 * variable to the path where the jboss server that will be used for unit testing 
 * resides. That jboss instance must contain a "unittest" configuration that has 
 * already had the Ant <code>prepare-jboss</code> target run against it. The 
 * <code>hq-ds.xml</code> datasource file must be configured to point to the 
 * *preexisting* unit testing server database.
 * 
 * In addition, the <code>HQ_HOME</code> and <code>HQEE_HOME</code> environment 
 * variables must be set, respectively, to the local path where the HQEE and HQ.ORG 
 * src trees reside.
 */
public abstract class BaseServerTestCase extends TestCase {
    
    private static final String logCtx = BaseServerTestCase.class.getName();
    private static final String DUMP_FILE = "dbdump.xml.gz";
    
    /**
     * Path designated for file in the current run
     * contents may be deleted while not running
     */
    private static final String WORKING_DIR = "/tmp";
    
    /**
     * The environment variable specifying the path to the jboss deployment 
     * that will be used for unit testing.
     */
    public static final String JBOSS_HOME_DIR_ENV_VAR = "UNITTEST_JBOSS_HOME";
    
    /**
     * The environment variable specifying the path to the HQ home directory. 
     * The HQ.ORG server will be deployed from the HQ home "build" subdirectory.
     */
    public static final String HQ_HOME_DIR_ENV_VAR = "HQ_HOME";
    
    /**
     * The environment variable specifying the path to the HQEE home directory. 
     * The HQEE server will be deployed from the HQEE home "build" subdirectory.
     */
    public static final String HQEE_HOME_DIR_ENV_VAR = "HQEE_HOME";
    
    /**
     * The "unittest" configuration that the jboss deployment must have installed 
     * and prepared using the Ant "prepare-jboss" target.
     */
    public static final String JBOSS_UNIT_TEST_CONFIGURATION = "unittest";
    
    private static ServerLifecycle server;
            
    private static URL deployment;
    
    
    /**
     * Creates an instance.
     *
     * @param name The test case name.
     */
    public BaseServerTestCase(String name) {
        super(name);
    }
    
    /**
     * Delegates to the super class.
     * 
     * @see junit.framework.TestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
    }
    
    /**
     * Delegates to the super class.
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    protected final Connection getConnection(boolean forRestore)
            throws UnitTestDBException {
        try {
            String deployDir = 
                getJBossHomeDir()+"/server/"+JBOSS_UNIT_TEST_CONFIGURATION+"/deploy/";
            
            File file = new File(deployDir, "hq-ds.xml");
            Document doc =  new SAXBuilder().build(file);
            Element element =
                doc.getRootElement().getChild("local-tx-datasource");
            String url = element.getChild("connection-url").getText();
            String user = element.getChild("user-name").getText();
            String passwd = element.getChild("password").getText();
            String driverClass = element.getChild("driver-class").getText();
            if (forRestore && driverClass.toLowerCase().contains("mysql")) {
                String buf = "?";
                if (driverClass.toLowerCase().contains("?")) {
                    buf = "&";
                }
                driverClass = driverClass +
                    buf + "sessionVariables=FOREIGN_KEY_CHECKS=0";
            }
            Driver driver = (Driver)Class.forName(driverClass).newInstance();
            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", passwd);
            return driver.connect(url, props);
        } catch (JDOMException e) {
            throw new UnitTestDBException(e);
        } catch (IOException e) {
            throw new UnitTestDBException(e);
        } catch (InstantiationException e) {
            throw new UnitTestDBException(e);
        } catch (SQLException e) {
            throw new UnitTestDBException(e);
        } catch (IllegalAccessException e) {
            throw new UnitTestDBException(e);
        } catch (ClassNotFoundException e) {
            throw new UnitTestDBException(e);
        }
    }

    protected final void restoreDatabase()
            throws UnitTestDBException {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection(true);
            IDatabaseConnection idbConn = new DatabaseConnection(conn);
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            // this is done for MySQL via another method
            if (DBUtil.isPostgreSQL(conn)) {
                stmt.execute("set constraints all deferred");
            } else if (DBUtil.isOracle(conn)) {
                stmt.execute("alter session set constraints = deferred");
            }
            IDataSet dataset = new FlatXmlDataSet(new GZIPInputStream(
                new FileInputStream(WORKING_DIR+"/"+DUMP_FILE)));
            DatabaseOperation.CLEAN_INSERT.execute(idbConn, dataset);
            conn.commit();
        } catch (SQLException e) {
            throw new UnitTestDBException(e);
        } catch (DataSetException e) {
            throw new UnitTestDBException(e);
        } catch (FileNotFoundException e) {
            throw new UnitTestDBException(e);
        } catch (IOException e) {
            throw new UnitTestDBException(e);
        } catch (DatabaseUnitException e) {
            throw new UnitTestDBException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, null);
        }
    }

    protected final void dumpDatabase(Connection conn)
            throws UnitTestDBException {
        try {
            IDatabaseConnection idbConn = new DatabaseConnection(conn);
            IDataSet fullDataSet;
            fullDataSet = idbConn.createDataSet();
            GZIPOutputStream gstream =
                new GZIPOutputStream(
                    new FileOutputStream(WORKING_DIR+"/"+DUMP_FILE));
            FlatXmlDataSet.write(fullDataSet, gstream);
            gstream.finish();
        } catch (SQLException e) {
            throw new UnitTestDBException(e);
        } catch (FileNotFoundException e) {
            throw new UnitTestDBException(e);
        } catch (IOException e) {
            throw new UnitTestDBException(e);
        } catch (DataSetException e) {
            throw new UnitTestDBException(e);
        }
    }
    
    /**
     * Start the jboss server with the "unittest" configuration.
     * 
     * @throws Exception
     */
    protected final void startServer() throws Exception {        
        if (server == null || !server.isStarted()) {
            String jbossHomeDir = getJBossHomeDir();
            
            server = new ServerLifecycle(new File(jbossHomeDir), 
                                         JBOSS_UNIT_TEST_CONFIGURATION);
            server.startServer();
        }
        
        assertTrue(server.isStarted());
    }
    
    /**
     * Deploy the HQ application into the jboss server, starting the jboss server 
     * first if necessary.
     * 
     * @param isEE <code>true</code> to deploy the HQEE EAR file; 
     *             <code>false</code> to deploy the HQ.ORG EAR file.
     * @throws Exception
     */
    protected final void deployHQ(boolean isEE) throws Exception {
        if (server == null || !server.isStarted()) {
            startServer();
        }
        
        deployment = getHQDeployment(isEE);
        
        server.deploy(deployment);
    }
    
    /**
     * Undeploy the HQ or HQEE application from the jboss server.
     * 
     * @throws Exception
     */
    protected final void undeployHQ() throws Exception {
        if (server != null && server.isStarted() && deployment != null) {
            server.undeploy(deployment);
            deployment = null;
        }
    }
    
    /**
     * Stop the jboss server.
     */
    protected final void stopServer() {
        if (server != null) {
            server.stopServer();
            assertFalse(server.isStarted());
            server = null;
            deployment = null;
        }
    }
    
    private String getJBossHomeDir() {
        String jbossHomeDir = System.getenv(JBOSS_HOME_DIR_ENV_VAR);
        
        if (jbossHomeDir == null) {
            throw new IllegalStateException("The "+JBOSS_HOME_DIR_ENV_VAR+
                            " environment variable was not set");
        }
        
        return jbossHomeDir;
    }
        
    private URL getHQDeployment(boolean isEE) throws MalformedURLException {
        if (isEE) {
            return new URL("file:"+getHQEEHomeDir()+"/build/hq.ear/");
        } else {
            return new URL("file:"+getHQHomeDir()+"/build/hq.ear/");
        }
    }
    
    private String getHQHomeDir() {
        String hqHomeDir = System.getenv(HQ_HOME_DIR_ENV_VAR);
        
        if (hqHomeDir == null) {
            throw new IllegalStateException("The "+HQ_HOME_DIR_ENV_VAR+
                            " environment variable was not set");
        }
        
        return hqHomeDir;
    }
    
    private String getHQEEHomeDir() {
        String hqHomeDir = System.getenv(HQEE_HOME_DIR_ENV_VAR);
        
        if (hqHomeDir == null) {
            throw new IllegalStateException("The "+HQEE_HOME_DIR_ENV_VAR+
                            " environment variable was not set");
        }
        
        return hqHomeDir;
    }
    
}
