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
    private final Map _cache = new HashMap();
    private final String _query, _queryKey;
    private long _last = -1l;
    private final long _cacheTimeout;

    public JDBCQueryCache(String query, String queryKey, long cacheTimeout) {
        _query = query;
        _queryKey = queryKey;
        _cacheTimeout = cacheTimeout;
    }

    public Object get(Connection conn, String key, String column)
        throws SQLException, JDBCQueryCacheException {
        long now = System.currentTimeMillis();
        if (_cache.size() == 0 || (now - _cacheTimeout) > _last) {
            repopulateCache(conn);
        }
        List list = (List) _cache.get(key);
        for (Iterator it = list.iterator(); it.hasNext();) {
            NameValuePair pair = (NameValuePair) it.next();
            if (pair.getName().equals(column)) {
                return pair.getValue();
            }
        }
        throw new JDBCQueryCacheException(
            "key " + key + ", column " + column + " not found.");
    }

    private void repopulateCache(Connection conn)
        throws SQLException, JDBCQueryCacheException
    {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            _cache.clear();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(_query);
            List columns = getQueryColumns(rs);
            while (rs.next()) {
                int i = 1;
                List vals = new ArrayList();
                String key = null;
                for (Iterator it = columns.iterator(); it.hasNext(); i++) {
                    String column = (String) it.next();
                    if (column.equals(_queryKey)) {
                        key = rs.getString(i);
                    } else {
                        vals.add(new NameValuePair(column, rs.getObject(i)));
                    }
                }
                if (key == null) {
                    throw new JDBCQueryCacheException("queryKey, " + _queryKey
                        + " was not represented in the query");
                }
                _cache.put(key, vals);
            }
            _last = System.currentTimeMillis();
        } finally {
            DBUtil.closeJDBCObjects(_logCtx, null, stmt, rs);
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