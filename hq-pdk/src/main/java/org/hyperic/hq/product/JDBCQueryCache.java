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

package org.hyperic.hq.product;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.jdbc.DBUtil;

/**
 * JDBCQueryCache is a simple caching mechanism to be used with
 * JDBCMeasurementPlugins.
 * 
 * example:
 * 
 * mysql> select Value, Default from sys_table;
 * +-----------------------------------+----------+---------+
 * | Variable_name                     | Value    | Default |
 * +-----------------------------------+----------+---------+
 * | DB_Max_Memory                     | 8192     | 1024    |
 * | DB_Max_Connections                | 400      | 10      |
 * ...
 * ...
 * 
 * <code>
 * String query = "select Value, Default from sys_table";
 * JDBCQueryCache cache = new JDBCQueryCache(query, "Variable_name", 5000);
 * Double val = Double.valueOf(cache.get("DB_Max_Memory", "Value").toString());
 * val = Double.valueOf(cache.get("DB_Max_Connections", "Default").toString());
 * </code>
 */
public class JDBCQueryCache {
    private static final String _logCtx = JDBCQueryCache.class.getName();
    private final Log _log = LogFactory.getLog(_logCtx);
    private final Map _cache = new HashMap();
    private final String _query, _queryKey;
    private long _last = -1l;
    private final long _cacheTimeout;

    public JDBCQueryCache(String query, String queryKey, long cacheTimeout) {
        _query = query;
        _queryKey = queryKey;
        _cacheTimeout = cacheTimeout;
    }
    
    /**
     * Explicitly clears any cached value
     */
    public void clearCache() {
        _cache.clear();
    }
    
    /**
     * Explicitly sets the expire time of the cache to expireTime.  Cache will
     * not repopulate until System.currentTimeMillis() <= expireTime.
     */
    public void setExpireTime(long expireTime) {
        _last = expireTime - _cacheTimeout;
    }

    /**
     * @return Object representation of the *only* row and column value
     *  or null if it does not exist
     * @throws JDBCQueryCacheException if there are 0 or > 1 rows in the cache.
     */
    public Object getOnlyRow(Connection conn, String column)
        throws SQLException, JDBCQueryCacheException {
        final long now = System.currentTimeMillis();
        if (_cache.isEmpty() || (now - _cacheTimeout) > _last) {
            repopulateCache(conn);
        }
        final Set keys = _cache.keySet();
        if (keys.size() > 1) {
            throw new JDBCQueryCacheException(
                "cache contains more than one row");
        } else if (keys.size() <= 0) {
            throw new JDBCQueryCacheException(
                "cache does not contain any results");
        }
        List list = null;
        for (final Iterator it = _cache.entrySet().iterator(); it.hasNext();) {
            final Map.Entry entry = (Map.Entry)it.next();
            list = (List)entry.getValue();
            break;
        }
        if (list == null) {
            return null;
        }
        for (final Iterator it = list.iterator(); it.hasNext();) {
            final NameValuePair pair = (NameValuePair) it.next();
            if (pair.getName().equals(column)) {
                return pair.getValue();
            }
        }
        throw new JDBCQueryCacheException(
            "column " + column + " not found.");
    }

    /**
     * @return Object representation of the row key/column value or null if it 
     * does not exist
     */
    public Object get(Connection conn, String key, String column)
        throws JDBCQueryCacheException, SQLException {
        long now = System.currentTimeMillis();
        if (_cache.isEmpty() || (now - _cacheTimeout) > _last) {
            repopulateCache(conn);
        }
        List list = (List) _cache.get(key);
        if (list == null) {
            return null;
        }
        for (Iterator it = list.iterator(); it.hasNext();) {
            NameValuePair pair = (NameValuePair) it.next();
            if (pair.getName().equalsIgnoreCase(column)) {
                return pair.getValue();
            }
        }
        throw new JDBCQueryCacheException("key " + key + ", column "
            + column + " not found.");
    }

    private void repopulateCache(Connection conn)
        throws SQLException, JDBCQueryCacheException
    {
        Statement stmt = null;
        ResultSet rs = null;
        if (_log.isDebugEnabled()) {
            final String msg = "re-populating JDBCQueryCache for " + _query +
                " with queryKey of " + _queryKey;
            _log.debug(msg);
        }
        try {
            _cache.clear();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(_query);
            final List columns = getQueryColumns(rs);
            final boolean debug = _log.isDebugEnabled();
            while (rs.next()) {
                int i = 1;
                final List vals = new ArrayList();
                String key = null;
                for (final Iterator it = columns.iterator(); it.hasNext(); i++) {
                    final String column = (String) it.next();
                    if (column.equalsIgnoreCase(_queryKey)) {
                        key = rs.getString(i);
                    } else {
                        String tmp = rs.getString(i);
                        tmp = (rs.wasNull()) ? null : tmp;
                        if (debug) {
                            _log.debug("adding nameValuePair="
                                + column + "/" + tmp);
                        }
                        vals.add(new NameValuePair(column, tmp));
                    }
                }
                if (key == null) {
                    throw new JDBCQueryCacheException("queryKey, " + _queryKey
                        + " was not represented in the query");
                }
                if (debug) _log.debug("adding key=" + key);
                _cache.put(key, vals);
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            DBUtil.closeJDBCObjects(_logCtx, null, stmt, rs);
            // even if a failure occurs we want to set the _last update time.
            // this will alleviate the situation where there are a bunch
            // of subsequent failures.  These failures could potentially hold
            // up the ScheduleThread
            _last = System.currentTimeMillis();
        }
    }

    private List getQueryColumns(ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int size = rsmd.getColumnCount();
        List rtn = new ArrayList(size);
        for (int i = 1; i <= size; i++) {
            rtn.add(rsmd.getColumnLabel(i));
        }
        return rtn;
    }

    private class NameValuePair {
        private final String _name;
        private final Object _value;
        public NameValuePair(String name, Object value) {
            _name = name;
            _value = value;
        }
        public String getName() {
            return _name;
        }
        public Object getValue() {
            return _value;
        }
    }
}