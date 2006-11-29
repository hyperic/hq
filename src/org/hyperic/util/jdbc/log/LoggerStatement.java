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

package org.hyperic.util.jdbc.log;

import java.sql.*;

public class LoggerStatement implements Statement {

    public static final String NULL = "NULL";

    protected LoggerConnection itsConn = null;
    protected Statement itsStmt = null;

    public LoggerStatement ( LoggerConnection conn, 
                             Statement stmt ) {
        itsConn = conn;
        itsStmt = stmt;
    }

    protected void doLogging (String sql) {
        itsConn.notifyListeners(sql);
    }

    protected String shortSql (String sql) {
        if (sql.length() < 16) return sql;
        return sql.substring(0,15) + "...";
    }

    public ResultSet executeQuery(String s) throws SQLException {
        doLogging(s);
        if ( !itsConn.getExecutionEnabled(LoggerConnection.M_QUERIES) ) return null;
        long start = System.currentTimeMillis();
        ResultSet rs = itsStmt.executeQuery(s);
        long duration = System.currentTimeMillis() - start;
        StringBuffer msg = new StringBuffer();
        msg.append("DONE (t=").append(duration)
            .append("): ").append(shortSql(s));
        doLogging(msg.toString());
        return rs;
    }

    public int executeUpdate(String s) throws SQLException {
        doLogging(s);
        if ( !itsConn.getExecutionEnabled(LoggerConnection.M_UPDATES) ) return 0;
        long start = System.currentTimeMillis();
        int rval = itsStmt.executeUpdate(s);
        long duration = System.currentTimeMillis() - start;
        StringBuffer msg = new StringBuffer();
        msg.append("DONE (rval=").append(rval)
            .append(", t=").append(duration)
            .append("): ").append(shortSql(s));
        doLogging(msg.toString());
        return rval;
    }

    public void close() throws SQLException { 
        itsStmt.close();
    }

    public int getMaxFieldSize() throws SQLException {
        return itsStmt.getMaxFieldSize();
    }

    public void setMaxFieldSize(int i) throws SQLException {
        itsStmt.setMaxFieldSize(i);
    }

    public int getMaxRows() throws SQLException {
        return itsStmt.getMaxRows();
    }

    public void setMaxRows(int i) throws SQLException {
        itsStmt.setMaxRows(i);
    }

    public void setEscapeProcessing(boolean flag) throws SQLException {
        itsStmt.setEscapeProcessing(flag);
    }

    public int getQueryTimeout() throws SQLException {
        return itsStmt.getQueryTimeout();
    }

    public void setQueryTimeout(int i) throws SQLException {
        itsStmt.setQueryTimeout(i);
    }

    public void cancel() throws SQLException {
        itsStmt.close();
    }

    public SQLWarning getWarnings() throws SQLException {
        return itsStmt.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        itsStmt.clearWarnings();
    }

    public void setCursorName(String s) throws SQLException {
        itsStmt.setCursorName(s);
    }

    public boolean execute(String s) throws SQLException {
        doLogging(s);
        int type = LoggerConnection.guessStatementType(s);
        if ( !itsConn.getExecutionEnabled(type) ) return false;
        long start = System.currentTimeMillis();
        boolean rval = itsStmt.execute(s);
        long duration = System.currentTimeMillis() - start;
        StringBuffer msg = new StringBuffer();
        msg.append("DONE (rval=").append(rval)
            .append(", t=").append(duration)
            .append("): ").append(shortSql(s));
        doLogging(msg.toString());
        return rval;
    }

    public ResultSet getResultSet() throws SQLException {
        ResultSet rs = itsStmt.getResultSet();
        return rs;
    }

    public int getUpdateCount() throws SQLException {
        return itsStmt.getUpdateCount();
    }

    public boolean getMoreResults() throws SQLException {
        return itsStmt.getMoreResults();
    }

    public void setFetchDirection(int i) throws SQLException {
        itsStmt.setFetchDirection(i);
    }

    public int getFetchDirection() throws SQLException {
        return itsStmt.getFetchDirection();
    }

    public void setFetchSize(int i) throws SQLException {
        itsStmt.setFetchSize(i);
    }

    public int getFetchSize() throws SQLException {
        return itsStmt.getFetchSize();
    }

    public int getResultSetConcurrency() throws SQLException {
        return itsStmt.getResultSetConcurrency();
    }

    public int getResultSetType() throws SQLException {
        return itsStmt.getResultSetType();
    }

    public void addBatch(String s) throws SQLException {
        itsStmt.addBatch(s);
    }

    public void clearBatch() throws SQLException {
        itsStmt.clearBatch();
    }

    public int[] executeBatch() throws SQLException {
        // We don't support batches yet.
        return itsStmt.executeBatch();
    }

    public Connection getConnection() throws SQLException {
        return itsConn;
    }

    public boolean getMoreResults(int i) throws SQLException {
        return itsStmt.getMoreResults(i);
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return itsStmt.getGeneratedKeys();
    }

    public int executeUpdate(String s, int i) throws SQLException {
        doLogging(s);
        if ( !itsConn.getExecutionEnabled(LoggerConnection.M_UPDATES) ) return 0;
        long start = System.currentTimeMillis();
        int rval = itsStmt.executeUpdate(s, i);
        long duration = System.currentTimeMillis() - start;
        StringBuffer msg = new StringBuffer();
        msg.append("DONE (rval=").append(rval)
            .append(", t=").append(duration)
            .append("): ").append(shortSql(s));
        doLogging(msg.toString());
        return rval;
    }

    public int executeUpdate(String s, int ai[]) throws SQLException {
        doLogging(s);
        if ( !itsConn.getExecutionEnabled(LoggerConnection.M_UPDATES) ) return 0;
        long start = System.currentTimeMillis();
        int rval = itsStmt.executeUpdate(s, ai);
        long duration = System.currentTimeMillis() - start;
        StringBuffer msg = new StringBuffer();
        msg.append("DONE (rval=").append(rval)
            .append(", t=").append(duration)
            .append("): ").append(shortSql(s));
        doLogging(msg.toString());
        return rval;
    }

    public int executeUpdate(String s, String as[]) throws SQLException {
        doLogging(s);
        if ( !itsConn.getExecutionEnabled(LoggerConnection.M_UPDATES) ) return 0;
        long start = System.currentTimeMillis();
        int rval = itsStmt.executeUpdate(s, as);
        long duration = System.currentTimeMillis() - start;
        StringBuffer msg = new StringBuffer();
        msg.append("DONE (rval=").append(rval)
            .append(", t=").append(duration)
            .append("): ").append(shortSql(s));
        doLogging(msg.toString());
        return rval;
    }

    public boolean execute(String s, int i) throws SQLException {
        doLogging(s);
        int type = LoggerConnection.guessStatementType(s);
        if ( !itsConn.getExecutionEnabled(type) ) return false;
        long start = System.currentTimeMillis();
        boolean rval = itsStmt.execute(s, i);
        long duration = System.currentTimeMillis() - start;
        StringBuffer msg = new StringBuffer();
        msg.append("DONE (rval=").append(rval)
            .append(", t=").append(duration)
            .append("): ").append(shortSql(s));
        doLogging(msg.toString());
        return rval;
    }

    public boolean execute(String s, int ai[]) throws SQLException {
        doLogging(s);
        int type = LoggerConnection.guessStatementType(s);
        if ( !itsConn.getExecutionEnabled(type) ) return false;
        long start = System.currentTimeMillis();
        boolean rval = itsStmt.execute(s, ai);
        long duration = System.currentTimeMillis() - start;
        StringBuffer msg = new StringBuffer();
        msg.append("DONE (rval=").append(rval)
            .append(", t=").append(duration)
            .append("): ").append(shortSql(s));
        doLogging(msg.toString());
        return rval;
    }

    public boolean execute(String s, String as[]) throws SQLException {
        doLogging(s);
        int type = LoggerConnection.guessStatementType(s);
        if ( !itsConn.getExecutionEnabled(type) ) return false;
        long start = System.currentTimeMillis();
        boolean rval = itsStmt.execute(s, as);
        long duration = System.currentTimeMillis() - start;
        StringBuffer msg = new StringBuffer();
        msg.append("DONE (rval=").append(rval)
            .append(", t=").append(duration)
            .append("): ").append(shortSql(s));
        doLogging(msg.toString());
        return rval;
    }

    public int getResultSetHoldability() throws SQLException {
        return itsStmt.getResultSetHoldability();
    }
}
