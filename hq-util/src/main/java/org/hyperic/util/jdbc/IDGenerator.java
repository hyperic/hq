/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.util.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hyperic.util.ConfigPropertyException;

public class IDGenerator {

    private int itsDBType = DBUtil.DATABASE_UNKNOWN;
    private String itsSequenceName;
    private int itsSequenceInterval;
    private String itsTableName = null;

    private String itsAlterSQL = null;
    private String itsSelectSQL = null;
    private long itsLastKey = 1;
    private long itsMaxKey = 1;

    // logging context
    private String ctx = null;

    private boolean isInitialized = false;

    private DBUtil dbUtil;

    /**
     * This constructor is for use inside an entity bean.
     * 
     * @param ctx The logging context to use
     * @param theSequenceName name of the database sequence
     * @param theSequenceInterval how many values we should grab at a time from
     *        the db
     * @param theDSName the name of the data source used to connect to the db
     */
    public IDGenerator(String ctx, String theSequenceName, int theSequenceInterval, DBUtil dbUtil) {
        this.ctx = ctx;
        itsSequenceName = theSequenceName;
        itsSequenceInterval = theSequenceInterval;
        itsTableName = getTableName(itsSequenceName);
        isInitialized = false;
        this.dbUtil = dbUtil;
    }

    public synchronized long getNewID() throws ConfigPropertyException, SequenceRetrievalException,
        SQLException {

        if (!isInitialized)
            init();
        if (itsLastKey >= itsMaxKey)
            getBatch();
        return ++itsLastKey;
    }

    private synchronized void getBatch() throws SequenceRetrievalException, SQLException {

        // Go to database and set new values for itsLastKey and itsMaxKey
        Connection conn = null;
        PreparedStatement selectPS = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            selectPS = conn.prepareStatement(itsSelectSQL);

            rs = selectPS.executeQuery();
            if (rs != null && rs.next()) {
                itsLastKey = rs.getLong(1) - 1;
                itsMaxKey = itsLastKey + itsSequenceInterval;
            } else {
                throw new SequenceRetrievalException(
                    "IDGenerator.getBatch: sequence failed to return a value: " + itsSequenceName);
            }
            doAlterSequence(conn);

        } finally {
            DBUtil.closeJDBCObjects(ctx, conn, selectPS, rs);
        }
    }

    private void doAlterSequence(Connection conn) throws SQLException {

        PreparedStatement alterPS = null;
        ResultSet rs = null;
        try {
            switch (itsDBType) {
                case DBUtil.DATABASE_POSTGRESQL_7:
                case DBUtil.DATABASE_POSTGRESQL_8:
                case DBUtil.DATABASE_POSTGRESQL_9:
                    itsAlterSQL = "SELECT setval ('" + itsSequenceName + "', " + itsMaxKey + ")";
                    alterPS = conn.prepareStatement(itsAlterSQL);
                    rs = alterPS.executeQuery();
                    break;

                case DBUtil.DATABASE_ORACLE_8:
                case DBUtil.DATABASE_ORACLE_9:
                case DBUtil.DATABASE_ORACLE_10:
                case DBUtil.DATABASE_ORACLE_11:
                    alterPS = conn.prepareStatement(itsAlterSQL);
                    alterPS.executeUpdate();
                    break;

            }
        } finally {
            DBUtil.closeResultSet(ctx, rs);
            DBUtil.closeStatement(ctx, alterPS);
        }
    }

    private synchronized void init() throws ConfigPropertyException, SQLException {

        if (isInitialized) {
            return;
        }
        isInitialized = true;

        Connection conn = null;
        try {
            conn = getConnection();
            itsDBType = DBUtil.getDBType(conn);
        } finally {
            DBUtil.closeConnection(ctx, conn);
        }

        switch (itsDBType) {
            case DBUtil.DATABASE_POSTGRESQL_7:
            case DBUtil.DATABASE_POSTGRESQL_8:
            case DBUtil.DATABASE_POSTGRESQL_9:
                itsAlterSQL = "SELECT setval ('" + itsSequenceName + "', " + itsMaxKey + ")";
                itsSelectSQL = "SELECT nextval('" + itsSequenceName + "'::text)";

                break;

            case DBUtil.DATABASE_ORACLE_8:
            case DBUtil.DATABASE_ORACLE_9:
            case DBUtil.DATABASE_ORACLE_10:
                itsAlterSQL = "ALTER SEQUENCE " + itsSequenceName + " INCREMENT BY " +
                              itsSequenceInterval;
                itsSelectSQL = "SELECT " + itsSequenceName + ".nextval from DUAL";
                break;

            case DBUtil.DATABASE_MYSQL5:
                itsAlterSQL = "ALTER TABLE " + itsTableName + " AUTO_INCREMENT = " + itsMaxKey;
                // mysql assumes all columns with autoincrement are name ID
                itsSelectSQL = "SELECT MAX(ID) + 1 FROM " + itsTableName;
                break;
        }
    }

    /**
     * Get the name of the table a sequence refers to assumes sequence names
     * will follow the pattern SOME_TABLE_NAME_KEYCOL_SEQ
     * @return SOME_TABLE_NAME
     */
    private static String getTableName(String sequence) {
        String[] tokens = sequence.split("_");
        String sub = new String();
        int i = 0;
        while (i < (tokens.length - 2)) {
            sub += tokens[i] + (i != (tokens.length - 3) ? "_" : "");
            i++;
        }
        return sub;
    }

    private Connection getConnection() throws SQLException {
        return dbUtil.getConnection();
    }
}
