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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.sql.*;

import org.hyperic.util.StringUtil;

public class LoggerPreparedStatement extends LoggerStatement implements PreparedStatement {

    private String itsSQL = null;
    private PreparedStatement itsPS = null;
    private Object[] itsArgs = null;
    private int itsMaxIndex = 0;

    public LoggerPreparedStatement ( LoggerConnection conn, 
                                     PreparedStatement ps,
                                     String sql ) {
        super(conn, ps);
        itsPS = ps;
        itsSQL = sql;
        itsArgs = new Object[24];
    }

    private void expandArgsStorage () {
        Object[] newArgs = new Object[ (int) ((double)itsMaxIndex * 1.5) ];
        for ( int i=1; i<itsArgs.length; i++ ) {
            newArgs[i] = itsArgs[i];
        }
        itsArgs = newArgs;
    }

    public String getProcessedSQL () {

        // XXX This is cheesy but will work as long as the only '?' chars
        // in the query are there for substituting variables (in other words,
        // if it also contains a literal '?', then this will break)
        StringTokenizer st = new StringTokenizer(itsSQL, "?");
        String sql = ""; 
        for ( int i=1; st.hasMoreTokens() && i<=itsMaxIndex; i++ ) {
            sql += st.nextToken();
            if ( i < itsArgs.length ) sql += formatSQLString(itsArgs[i]);
        }
        // Add the rest of the stuff (there should really be only 1 more
        // token, unless there were not enough args to substitute for).
        while ( st.hasMoreTokens() ) sql += st.nextToken();
        
        return sql;
    }

    private void doProcessedLogging () {
        doLogging(getProcessedSQL());
    }

    private String formatSQLString ( Object o ) {
        if ( o == null ) return LoggerStatement.NULL;
        if ( o instanceof String ||
             o instanceof URL ) {
            String s = (String) o;
            return "'" + StringUtil.replace(s, "'", "\\'") + "'";
        }
        if ( o instanceof Date ||
             o instanceof Time ||
             o instanceof Timestamp ) {
            return "'" + o.toString() + "'";
        }
        return o.toString();
    }

    public ResultSet executeQuery() throws SQLException {
        doProcessedLogging();
        if ( !itsConn.getExecutionEnabled(LoggerConnection.M_QUERIES) ) return null;
        long start = System.currentTimeMillis();
        ResultSet rs = itsPS.executeQuery();
        long duration = System.currentTimeMillis() - start;
        StringBuffer msg = new StringBuffer();
        msg.append("DONE (t=").append(duration)
            .append("): ").append(shortSql(itsSQL));
        doLogging(msg.toString());
        return rs;
    }

    public int executeUpdate() throws SQLException {
        doProcessedLogging();
        if ( !itsConn.getExecutionEnabled(LoggerConnection.M_UPDATES) ) return 0;
        long start = System.currentTimeMillis();
        int rval = itsPS.executeUpdate();
        long duration = System.currentTimeMillis() - start;
        StringBuffer msg = new StringBuffer();
        msg.append("DONE (rval=").append(rval)
            .append(", t=").append(duration)
            .append("): ").append(shortSql(itsSQL));
        doLogging(msg.toString());
        return rval;
    }

    public void setNull(int i, int j) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = LoggerStatement.NULL;
        itsPS.setNull(i, j);
    }

    public void setBoolean(int i, boolean flag) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = new Boolean(flag);
        itsPS.setBoolean(i, flag);
    }

    public void setByte(int i, byte byte0) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = new Byte(byte0);
        itsPS.setByte(i, byte0);
    }

    public void setShort(int i, short word0) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = new Short(word0);
        itsPS.setShort(i, word0);
    }

    public void setInt(int i, int j) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = new Integer(j);
        itsPS.setInt(i, j);
    }

    public void setLong(int i, long l) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = new Long(l);
        itsPS.setLong(i, l);
    }

    public void setFloat(int i, float f) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = new Float(f);
        itsPS.setFloat(i, f);
    }

    public void setDouble(int i, double d) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = new Double(d);
        itsPS.setDouble(i, d);
    }

    public void setBigDecimal(int i, BigDecimal bigdecimal) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = bigdecimal;
        itsPS.setBigDecimal(i, bigdecimal);
    }

    public void setString(int i, String s) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = s;
        itsPS.setString(i, s);
    }

    public void setBytes(int i, byte abyte0[]) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = abyte0;
        itsPS.setBytes(i, abyte0);
    }

    public void setDate(int i, Date date) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = date;
        itsPS.setDate(i, date);
    }

    public void setTime(int i, Time time) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = time;
        itsPS.setTime(i, time);
    }

    public void setTimestamp(int i, Timestamp timestamp) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = timestamp;
        itsPS.setTimestamp(i, timestamp);
    }

    public void setAsciiStream(int i, InputStream inputstream, int j) throws SQLException {
        // We don't support recording stream data right now...
        itsPS.setAsciiStream(i, inputstream, j);
    }

    /**
     * @deprecated Method setUnicodeStream is deprecated
     */

    public void setUnicodeStream(int i, InputStream inputstream, int j) throws SQLException {
        // We don't support recording stream data right now...
        itsPS.setUnicodeStream(i, inputstream, j);
    }

    public void setBinaryStream(int i, InputStream inputstream, int j) throws SQLException {
        // We don't support recording stream data right now...
        itsPS.setBinaryStream(i, inputstream, j);
    }

    public void clearParameters() throws SQLException {
        for ( int i=0; i<itsArgs.length; i++ ) {
            itsArgs[i] = null;
        }
        itsPS.clearParameters();
    }

    public void setObject(int i, Object obj, int j, int k) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = obj;
        itsPS.setObject(i, obj, j, k);
    }

    public void setObject(int i, Object obj, int j) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = obj;
        itsPS.setObject(i, obj,  j);
    }

    public void setObject(int i, Object obj) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = obj;
        itsPS.setObject(i, obj);
    }

    public boolean execute() throws SQLException {
        doProcessedLogging();
        int type = LoggerConnection.guessStatementType(itsSQL);
        if ( !itsConn.getExecutionEnabled(type) ) return false;
        long start = System.currentTimeMillis();
        boolean rval = itsPS.execute();
        long duration = System.currentTimeMillis() - start;
        StringBuffer msg = new StringBuffer();
        msg.append("DONE (rval=").append(rval)
            .append(", t=").append(duration)
            .append("): ").append(shortSql(itsSQL));
        doLogging(msg.toString());
        return rval;
    }

    public void addBatch() throws SQLException {
        itsPS.addBatch();
    }

    public void setCharacterStream(int i, Reader reader, int j) throws SQLException {
        itsPS.setCharacterStream(i, reader, j);
    }

    public void setRef(int i, Ref ref) throws SQLException {
        itsPS.setRef(i, ref);
    }

    public void setBlob(int i, Blob blob) throws SQLException {
        itsPS.setBlob(i, blob);
    }

    public void setClob(int i, Clob clob) throws SQLException {
        itsPS.setClob(i, clob);
    }

    public void setArray(int i, Array array) throws SQLException {
        itsPS.setArray(i, array);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return itsPS.getMetaData();
    }

    public void setDate(int i, Date date, Calendar calendar) throws SQLException {
        itsPS.setDate(i, date, calendar);
    }

    public void setTime(int i, Time time, Calendar calendar) throws SQLException {
        itsPS.setTime(i, time, calendar);
    }

    public void setTimestamp(int i, Timestamp timestamp, Calendar calendar) throws SQLException {
        itsPS.setTimestamp(i, timestamp, calendar);
    }

    public void setNull(int i, int j, String s) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = LoggerStatement.NULL;
        itsPS.setNull(i, j, s);
    }

    public void setURL(int i, URL url) throws SQLException {
        if ( i > itsMaxIndex ) itsMaxIndex = i;
        if ( i>=itsArgs.length ) expandArgsStorage();
        itsArgs[i] = url;
        itsPS.setURL(i, url);
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        return itsPS.getParameterMetaData();
    }
}
