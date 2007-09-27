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
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class LoggerConnection implements Connection {

    public static final String PROP_DEFAULT_LISTENER = "jdbcLogListener";
    public static final String PROP_SQLONLY          = "jdbcLogSqlOnly";
    public static final String PROP_VERBOSE          = "jdbcLogVerbose";
    public static final String PROP_LOGTX            = "jdbcLogTx";

    public static final int M_NONE    = 0;
    public static final int M_QUERIES = 1<<0;
    public static final int M_UPDATES = 1<<1;
    public static final int M_CALLS   = 1<<2;
    public static final int M_ALL     = M_QUERIES | M_UPDATES | M_CALLS;

    /** Note - these are sensitive to changes to the values of the
     * TRANSACTION_XXX constants in java.sql.Connection.  However,
     * it's highly unlikely that those constants will ever change. */
    public static final String[] TX_ISOLATION_LEVELS
        = { "NONE", "READ_UNCOMMITTED", "READ_COMMITTED", 
            "REPEATABLE_READ", "SERIALIZABLE" };

    private List itsListeners = null;
    private Connection itsConn = null;
    private int itsExecMask = M_ALL;
    private int itsLogMask  = M_ALL;
    private boolean itsVerbose = false;
    private boolean itsSqlOnly = false;
    private boolean itsLogTx = false;
    private LoggerDriver itsDriver = null;

    // command history tracking
    private int historySize;
    private LogHistory[] history;
    private int historyIndex;
    private long beginUseTime;
    
    /** For debug use only */
    public LoggerConnection ( Connection c ) {
        this(new LoggerDriver(), c);
    }

    public LoggerConnection ( LoggerDriver driver, Connection c ) {
        itsConn = c;
        itsListeners = new Vector();
        itsDriver = driver;

        itsVerbose     = Boolean.getBoolean(PROP_VERBOSE);
        itsSqlOnly     = Boolean.getBoolean(PROP_SQLONLY);
        itsLogTx       = Boolean.getBoolean(PROP_LOGTX);

        historySize = 128; // make this configurable
        history = new LogHistory[historySize];
        historyIndex = 0;

        itsExecMask = M_ALL;
        itsLogMask = M_NONE;

        String defaultListenerClass
            = System.getProperty(PROP_DEFAULT_LISTENER);
        if ( defaultListenerClass != null ) {
            LoggerListener listener;
            try {
                listener = (LoggerListener) Class.forName(defaultListenerClass).newInstance();
                itsListeners.add(listener);
                listener.init(itsVerbose, itsSqlOnly);

            } catch ( Exception e ) {
                System.err.println("Error loading default LoggerListener: " + e);
                e.printStackTrace();
            }
        }
    }

    protected LoggerDriver getDriver () { return itsDriver; }

    public void addListener ( LoggerListener listener ) {
        itsListeners.add(listener);
    }

    public static int guessStatementType ( String sql ) {
        if ( sql.trim().substring(0, 6).equalsIgnoreCase("SELECT") ) {
            return LoggerConnection.M_QUERIES;
        }
        return LoggerConnection.M_UPDATES;
    }

    public void notifyListeners ( String sql ) {
        if (historySize > 0) {
            synchronized (history) {
                history[historyIndex % historySize] = new LogHistory(sql);
                historyIndex++;
            }
        }
        int type = guessStatementType(sql);
        if ( (itsLogMask & type) != 0 ) {
            int size = itsListeners.size();
            for ( int i=0; i<size; i++ ) {
                ((LoggerListener) itsListeners.get(i)).logSQL(sql);
            }
        }
    }

    public int getHistorySize () { return historySize; }
    public LogHistory getHistory (int offset) {
        synchronized (history) {
            return history[(historyIndex+offset) % historySize];
        }
    }

    public int getExecutionMask ( int execMask ) {
        return itsExecMask;
    }
    public boolean getExecutionEnabled ( int type ) {
        return ( (itsExecMask & type) != 0 );
    }
    public void setExecutionMask ( int execMask ) {
        itsExecMask = execMask;
    }
    public void setLogMask ( int logMask ) {
        itsLogMask = logMask;
    }

    public Statement createStatement() throws SQLException {
        Statement s = itsConn.createStatement();
        return new LoggerStatement(this, s);
    }

    public PreparedStatement prepareStatement(String s) throws SQLException {
        if ( itsVerbose && (!itsSqlOnly) ) {
            notifyListeners("prepareStatement: " + s);
        }
        PreparedStatement ps = itsConn.prepareStatement(s);
        return new LoggerPreparedStatement(this, ps, s);
    }

    public CallableStatement prepareCall(String s) throws SQLException {
        CallableStatement cs = itsConn.prepareCall(s);
        return cs;
    }

    public String nativeSQL(String s) throws SQLException {
        return itsConn.nativeSQL(s);
    }

    public void setAutoCommit(boolean flag) throws SQLException {
        if ( itsLogTx ) {
            notifyListeners("setAutoCommmit(" + itsConn + "," + flag + ")");
        }
        itsConn.setAutoCommit(flag);
    }

    public boolean getAutoCommit() throws SQLException {
        return itsConn.getAutoCommit();
    }

    public void commit() throws SQLException {
        if ( itsLogTx ) {
            notifyListeners("commit(" + itsConn + ")");
        }
        itsConn.commit();
    }

    public void rollback() throws SQLException {
        if ( itsLogTx ) {
            notifyListeners("rollback(" + itsConn + ")");
        }
        itsConn.rollback();
    }

    public static final String CONNECTION_BEGIN_USE
        = " ----- BEGIN CONNECTION USE ----- ";
    public static final String CONNECTION_END_USE
        = " ----- END CONNECTION USE";
    public void beginUse () {
        notifyListeners(CONNECTION_BEGIN_USE);
        beginUseTime = System.currentTimeMillis();
    }
    public void endUse () {
        long duration = System.currentTimeMillis() - beginUseTime;
        beginUseTime = -1;
        StringBuffer msg = new StringBuffer();
        msg.append(CONNECTION_END_USE)
            .append(" (total time=").append(duration).append(") ----- ");
        notifyListeners(msg.toString());
    }
    public boolean isInUse () { return beginUseTime != -1; }

    public void close() throws SQLException {
        itsConn.close();
    }

    public boolean isClosed() throws SQLException {
        return itsConn.isClosed();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return itsConn.getMetaData();
    }

    public void setReadOnly(boolean flag) throws SQLException {
        itsConn.setReadOnly(flag);
    }

    public boolean isReadOnly() throws SQLException {
        return itsConn.isReadOnly();
    }

    public void setCatalog(String s) throws SQLException {
        itsConn.setCatalog(s);
    }

    public String getCatalog() throws SQLException {
        return itsConn.getCatalog();
    }

    public void setTransactionIsolation(int i) throws SQLException {
        if ( itsLogTx ) {
            notifyListeners("setTxIsolation(" + itsConn + "," 
                            + TX_ISOLATION_LEVELS[i] + ")");
        }
        itsConn.setTransactionIsolation(i);
    }

    public int getTransactionIsolation() throws SQLException {
        return itsConn.getTransactionIsolation();
    }

    public SQLWarning getWarnings() throws SQLException {
        return itsConn.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        itsConn.clearWarnings();
    }

    public Statement createStatement(int i, int j) throws SQLException {
        Statement s = itsConn.createStatement(i, j);
        return s;
    }

    public PreparedStatement prepareStatement(String s, int i, int j) throws SQLException {
        PreparedStatement ps = itsConn.prepareStatement(s, i, j);
        return ps;
    }

    public CallableStatement prepareCall(String s, int i, int j) throws SQLException {
        CallableStatement cs = itsConn.prepareCall(s, i, j);
        return cs;
    }

    public Map getTypeMap() throws SQLException {
        return itsConn.getTypeMap();
    }

    public void setTypeMap(Map map) throws SQLException {
        itsConn.setTypeMap(map);
    }

    public void setHoldability(int i) throws SQLException {
        itsConn.setHoldability(i);
    }

    public int getHoldability() throws SQLException {
        return itsConn.getHoldability();
    }

    public Savepoint setSavepoint() throws SQLException {
        Savepoint sp = itsConn.setSavepoint();
        if ( itsLogTx ) {
            notifyListeners("setSavepoint(" + itsConn + ")==>" + sp);
        }
        return sp;
    }

    public Savepoint setSavepoint(String s) throws SQLException {
        Savepoint sp = itsConn.setSavepoint(s);
        if ( itsLogTx ) {
            notifyListeners("setSavepoint(" + itsConn + "," + s + ")==>" + sp);
        }
        return sp;
    }

    public void rollback(Savepoint sp) throws SQLException {
        if ( itsLogTx ) {
            notifyListeners("rollback(" + itsConn + "," + sp + ")");
        }
        itsConn.rollback(sp);
    }

    public void releaseSavepoint(Savepoint sp) throws SQLException {
        if ( itsLogTx ) {
            notifyListeners("releaseSavepoint(" + itsConn + "," + sp + ")");
        }
        itsConn.releaseSavepoint(sp);
    }

    public Statement createStatement(int i, int j, int k) throws SQLException {
        Statement s = itsConn.createStatement(i, j, k);
        return new LoggerStatement(this, s);
    }

    public PreparedStatement prepareStatement(String s, int i, int j, int k) throws SQLException {
        PreparedStatement ps = itsConn.prepareStatement(s, i, j, k);
        return new LoggerPreparedStatement(this, ps, s);
    }

    public CallableStatement prepareCall(String s, int i, int j, int k) throws SQLException {
        CallableStatement cs = itsConn.prepareCall(s, i, j, k);
        return cs;
    }

    public PreparedStatement prepareStatement(String s, int i) throws SQLException {
        PreparedStatement ps = itsConn.prepareStatement(s, i);
        return new LoggerPreparedStatement(this, ps, s);
    }

    public PreparedStatement prepareStatement(String s, int ai[]) throws SQLException {
        PreparedStatement ps = itsConn.prepareStatement(s, ai);
        return new LoggerPreparedStatement(this, ps, s);
    }

    public PreparedStatement prepareStatement(String s, String as[]) throws SQLException {
        PreparedStatement ps = itsConn.prepareStatement(s, as);
        return new LoggerPreparedStatement(this, ps, s);
    }

}
