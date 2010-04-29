/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], Hyperic, Inc.
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

package org.hyperic.hq.db;

import java.sql.Connection;
import java.sql.Statement;
import java.util.zip.GZIPInputStream;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.unittest.server.UnitTestDBException;
import org.springframework.beans.factory.annotation.Autowired;

public class DatabasePopulator {

    private final Log log = LogFactory.getLog(DatabasePopulator.class);

    @Autowired
    private DataSource dataSource;

    private String file;

    public void setSchemaFile(String file) {
        this.file = file;
    }

    /**
     * Restore the unit test database to the original state specified by the
     * <code>test-dbsetup</code> Ant target.
     * 
     * @throws UnitTestDBException
     */
    public final void restoreDatabase() throws UnitTestDBException {
        log.info("Restoring unit test database from dump file.");
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = dataSource.getConnection();
            IDatabaseConnection idbConn = new DatabaseConnection(conn);
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            if (DBUtil.isPostgreSQL(conn)) {
                stmt.execute("set constraints all deferred");
            } else if (DBUtil.isOracle(conn)) {
                stmt.execute("alter session set constraints = deferred");
            } else if (DBUtil.isMySQL(conn)) {
                stmt.execute("set FOREIGN_KEY_CHECKS=0");
            }
            IDataSet dataset = new FlatXmlDataSet(new GZIPInputStream(getClass()
                .getResourceAsStream(file)));
            DatabaseOperation.CLEAN_INSERT.execute(idbConn, dataset);
        } catch (Exception e) {
            throw new UnitTestDBException(e);
        } finally {
            DBUtil.closeJDBCObjects(DatabasePopulator.class.getName(), conn, stmt, null);
        }
    }

}
