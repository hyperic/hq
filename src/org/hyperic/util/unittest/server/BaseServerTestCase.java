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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
 * <p>The test case that all server-side unit tests should extend. Before starting 
 * the server, the user must set the <code>hq.unittest.jboss.home</code> system  
 * property to the path where the jboss server that will be used for unit testing 
 * resides. That jboss instance must contain a "unittest" configuration created 
 * with the <code>unittest-prepare-jboss</code> Ant target. The datasource file 
 * (<code>hq-ds.xml</code>) in the "unittest" configuration must point to the 
 * *preexisting* unit testing server database.</p>
 *
 * <p>In addition, the <code>hq.unittest.hq.home</code> system property must be set 
 * to the local path where the HQ src resides (.ORG or EE versions depending 
 * on the type of unit test). The <code>hq.unittest.working.dir</code> system 
 * property must be set to the directory where the database dump file containing 
 * the initial server state resides (by default in $HQ_HOME/unittest/data).</p>
 *
 * <p>Finally (and most importantly), the system classloader must be 
 * set to the {@link IsolatingDefaultSystemClassLoader}.</p>
 */
public abstract class BaseServerTestCase extends TestCase {
    
    private static final Log log = LogFactory.getLog(BaseServerTestCase.class);
    
    private static final String logCtx = BaseServerTestCase.class.getName();
    
    /**
     * The dump file containing the initial unit test database state.
     */
    private static final String DUMP_FILE = "hqdb.dump.xml.gz";
    
    /**
     * Path designated for file in the current run
     * contents may be deleted while not running
     */
    private static final String WORKING_DIR = "hq.unittest.working.dir";
    
    /**
     * The system property specifying the path to the jboss deployment 
     * that will be used for unit testing.
     */
    public static final String JBOSS_HOME_DIR = "hq.unittest.jboss.home";
    
    /**
     * The system property specifying the path to the HQ home directory. 
     * The HQ server will be deployed from the HQ home "build" subdirectory.
     */
    public static final String HQ_HOME_DIR = "hq.unittest.hq.home";
    
    /**
     * The "unittest" configuration that the jboss deployment must have installed 
     * and prepared using the Ant "prepare-jboss" target.
     */
    public static final String JBOSS_UNIT_TEST_CONFIGURATION = "unittest";
    
    private static ServerLifecycle server;
            
    private static URL deployment;
    
    private final boolean restoreDatabaseOnFirstRun;
    
    private static boolean restoredOnFirstRun;
        
    /**
     * Creates an instance.
     *
     * @param name The test case name.
     * @param restoreDatabase <code>true</code> to restore the database before 
     *                        the first unit test in this test case; 
     *                        <code>false</code> to not restore the database.
     * @see #restoreDatabase()                       
     */
    public BaseServerTestCase(String name, boolean restoreDatabase) {
        super(name);
        restoreDatabaseOnFirstRun = restoreDatabase;
    }
    
    /**
     * Creates the initial unit test database dump file if it doesn't exist 
     * already. Also restores the database once before the first unit test in 
     * this test case if the constructor parameter is set to <code>restoreDatabase</code>.
     * 
     * Subclasses should never override this method, only extend it if necessary.
     */
    public void setUp() throws Exception {
        super.setUp();
        // we must make sure that the initial database dump always occurs
        dumpDatabase();
        
        if (restoreDatabaseOnFirstRun && !restoredOnFirstRun) {
            restoreDatabase();
            restoredOnFirstRun = true;
        }
    }
    
    /**
     * Delegates to the super class.
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     * Retrieve a connection to the unit test database. This connection 
     * may be used to verify that a state change has occurred during the 
     * unit test.
     * 
     * @return The database connection.
     * @throws UnitTestDBException
     */
    protected final Connection getConnectionToHQDatabase() throws UnitTestDBException {
        return getConnection(false);
    }
    
    /**
     * Restore the unit test database to the original state specified 
     * by the <code>test-dbsetup</code> Ant target.
     * 
     * @throws UnitTestDBException
     */
    protected final void restoreDatabase()
            throws UnitTestDBException {
        
        log.info("Restoring unit test database from dump file.");
        
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
                new FileInputStream(getHQWorkingDir()+"/"+DUMP_FILE)));
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

    /**
     * Used to insert new data into the unit test database. Will either update 
     * existing data or insert fresh.
     * 
     * @param schema filename to extract XML data from.
     * @throws UnitTestDBException 
     */
    protected final void insertSchemaData(String filename)
        throws UnitTestDBException {
        overlayDBData(filename, DatabaseOperation.CLEAN_INSERT);
    }

    /**
     * Used to insert new data into the unit test database. Will either update 
     * existing data or insert fresh.
     * 
     * @param List<String> of schema filenames to extract XML data from.
     * @throws UnitTestDBException 
     */
    protected final void insertSchemaData(List filenames)
        throws UnitTestDBException {
        overlayDBData(filenames, DatabaseOperation.CLEAN_INSERT);
    }
    
    /**
     * Used to delete data specified by the schema from the unit test database.
     * 
     * @param List<String> of schema filenames to extract XML data from.
     * @throws UnitTestDBException 
     */
    protected final void deleteSchemaData(List filenames)
        throws UnitTestDBException {
        overlayDBData(filenames, DatabaseOperation.DELETE);
    }
    
    /**
     * Used to delete data specified by the schema from the unit test database.
     * 
     * @param schema filename to extract XML data from.
     * @throws UnitTestDBException 
     */
    protected final void deleteSchemaData(String filename)
        throws UnitTestDBException {
        overlayDBData(filename, DatabaseOperation.DELETE);
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
     * @return The local interface registry for looking up local interfaces for 
     *          the deployed EJBs.
     * @throws Exception
     */
    protected final LocalInterfaceRegistry deployHQ() throws Exception {
        if (server == null || !server.isStarted()) {
            startServer();
        }
        
        deployment = getHQDeployment();
                
        return server.deploy(deployment);
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
     * Stop the jboss server. This facility is provided for completeness, but 
     * if back to back server restarts are performed then unit tests may fail 
     * because of server socket binding issues (the jboss server socket 
     * factories do not have SO_REUSEADDR set to true).
     * 
     * In general, it should be left up to the framework to stop the server 
     * once it has been started. If a unit test does not explicitly stop the 
     * server, then a shutdown hook stops the server automatically before the 
     * unit tests finish executing within their forked vm. 
     * 
     * If the user must stop the server, then any future server restarts within 
     * the same unit test vm should be delayed until the jboss server sockets 
     * can move from a TIME_WAIT to CLOSED state.
     */
    protected final void stopServer() {
        if (server != null) {
            server.stopServer();
            assertFalse(server.isStarted());
            server = null;
            deployment = null;
        }
    }
    
    
    /**
     * Create the database dump file used to restore the database if that 
     * dump file does not already exist.
     */
    private void dumpDatabase() throws Exception {
        File dbdump = new File(getHQWorkingDir(), DUMP_FILE);
        if (!dbdump.exists()) {
            log.info("Creating database dump file at "+dbdump.getCanonicalPath()+
                     " for the unit test framework.");
            
            Connection conn = null;
            
            try {
                conn = getConnection(false);
                // we need the ear to initially deploy so that we can
                // capture the plugins which take a lot more time
                // to initially deploy
                deployHQ();
                undeployHQ();
                dumpDatabase(conn);
            } finally {
                DBUtil.closeConnection(logCtx, conn);
            }
            
            log.info("Finished creating database dump file.");
        }        
    }

    private Connection getConnection(boolean forRestore)
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
                if (url.toLowerCase().contains("?")) {
                    buf = "&";
                }
                url = url + buf + "sessionVariables=FOREIGN_KEY_CHECKS=0";
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

    private final void dumpDatabase(Connection conn)
            throws UnitTestDBException {
        try {
            IDatabaseConnection idbConn = new DatabaseConnection(conn);
            IDataSet fullDataSet;
            fullDataSet = idbConn.createDataSet();
            GZIPOutputStream gstream =
                new GZIPOutputStream(
                    new FileOutputStream(getHQWorkingDir()+"/"+DUMP_FILE));
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

    private void overlayDBData(String filename, DatabaseOperation operation)
        throws UnitTestDBException {
        List filenames = new ArrayList();
        filenames.add(filename);
        overlayDBData(filenames, operation);
    }
    
    private void overlayDBData(List filenames, DatabaseOperation operation)
        throws UnitTestDBException {
        Connection conn = null;
        Statement stmt = null;
        InputStream stream = null;
        try {
            conn = getConnection(true);
            stmt = conn.createStatement();
            conn.setAutoCommit(false);
            IDatabaseConnection iconn = new DatabaseConnection(conn);
            // this is done for MySQL via another method
            if (DBUtil.isPostgreSQL(conn)) {
                stmt.execute("set constraints all deferred");
            } else if (DBUtil.isOracle(conn)) {
                stmt.execute("alter session set constraints = deferred");
            }
            for (Iterator i=filenames.iterator(); i.hasNext(); ) {
                String filename = (String)i.next();
                File overlayFile = new File(getHQWorkingDir(), filename);
                if (filename.endsWith(".gz")) {
                    stream = new GZIPInputStream(new FileInputStream(
                        overlayFile));
                } else {
                    stream = new BufferedInputStream(new FileInputStream(
                        overlayFile));
                }
                IDataSet dataset = new FlatXmlDataSet(stream);
                operation.execute(iconn, dataset);
                stream.close();
            }
            conn.commit();
        } catch (UnitTestDBException e) {
            throw new UnitTestDBException(e);
        } catch (SQLException e) {
            throw new UnitTestDBException(e);
        } catch (DatabaseUnitException e) {
            throw new UnitTestDBException(e);
        } catch (FileNotFoundException e) {
            throw new UnitTestDBException(e);
        } catch (IOException e) {
            throw new UnitTestDBException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, null);
            try {
                if (stream != null) stream.close();
            } catch (IOException e) {
                throw new UnitTestDBException(e);
            }
        }
    }    
        
    private String getJBossHomeDir() {
        String jbossHomeDir = System.getProperty(JBOSS_HOME_DIR);
        
        if (jbossHomeDir == null) {
            throw new IllegalStateException("The "+JBOSS_HOME_DIR+
                            " system property was not set");
        }
        
        return jbossHomeDir;
    }
        
    private URL getHQDeployment() throws MalformedURLException {
        return new URL("file:"+getHQHomeDir()+"/build/hq.ear/");
    }
    
    private String getHQHomeDir() {
        String hqHomeDir = System.getProperty(HQ_HOME_DIR);
        
        if (hqHomeDir == null) {
            throw new IllegalStateException("The "+HQ_HOME_DIR+
                                    " system property was not set");
        }
        
        return hqHomeDir;
    }
    
    private String getHQWorkingDir() {
        String hqWorkingDir = System.getProperty(WORKING_DIR);

        if (hqWorkingDir == null) {
            throw new IllegalStateException("The "+WORKING_DIR+
                                    " system property was not set");
        }

        return hqWorkingDir;
    }
    
}
