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
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

import junit.framework.TestCase;

/**
 * The test case that all server-side unit tests should extend. Before starting 
 * the server, the user must set the <code>UNITTEST_JBOSS_HOME</code> environment 
 * variable to the path where the jboss deployment that will be used for testing 
 * is deployed. That deployed jboss instance must contain a "unittest" configuration 
 * that has already had the Ant <code>prepare-jboss</code> target run against it.
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
     * The "unittest" configuration that the jboss deployment must have installed 
     * and prepared using the Ant "prepare-jboss" target.
     */
    public static final String JBOSS_UNIT_TEST_CONFIGURATION = "unittest";
    
    private static ServerLifecycle server;
    
    protected Connection getConnection(boolean forRestore)
            throws UnitTestDBException {
        try {
            File file = new File("server/default/deploy/hq-ds.xml");
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

    protected void restoreDatabase()
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

    protected void dumpDatabase(Connection conn)
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

    public BaseServerTestCase(String name) {
        super(name);
    }
    
    public void setUp() throws Exception {
        super.setUp();
    }
    
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    protected final void startServer() throws Exception {        
        if (server == null || !server.isStarted()) {
            String jbossHomeDir = System.getenv(JBOSS_HOME_DIR_ENV_VAR);
            
            if (jbossHomeDir == null) {
                throw new IllegalStateException("The "+JBOSS_HOME_DIR_ENV_VAR+
                                " environment variable was not set");
            }
            
            server = new ServerLifecycle(new File(jbossHomeDir), 
                                         JBOSS_UNIT_TEST_CONFIGURATION);
            server.startServer();
        }
        
        assertTrue(server.isStarted());
    }
    
    protected final void stopServer() {
        if (server != null) {
            server.stopServer();
            assertFalse(server.isStarted());
            server = null;
        }
    }
    
}
