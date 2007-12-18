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

package org.hyperic.tools.ant.dbupgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.hibernate.dialect.Dialect;
import org.hyperic.hibernate.dialect.HibernateUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.jdbc.DBUtil;

/**
 * Converts an old-style serialized ConfigResponse into a Crispo
 */
public class SST_Crisponizer extends CrispoTask {
    private static final Class LOGCTX = SST_Crisponizer.class;

    private String  _table;
    private String  _column;
    private String  _crispoColumn;
    private Pattern _onlyProperties;
    private String  _rewriteConfigResponse;
    
    public SST_Crisponizer() {}

    public void setTable(String table) {
        _table = table;
    }
    
    public void setColumn(String column) {
        _column = column;
    }
    
    public void setCrispoColumn(String c) {
        _crispoColumn = c;
    }
    
    public void setOnlyProperties(String f) {
        _onlyProperties = Pattern.compile(f);
    }
    
    public void setRewriteConfigResponse(String val) {
        _rewriteConfigResponse = val;
    }
    
    private boolean keyMatchesFilter(String key) {
        return _onlyProperties.matcher(key).matches();
    }
    
    private void updateRowWithCrispo(long fromId, long crispoId) 
        throws SQLException, BuildException
    {
        Statement stmt = null; 
        try {
            String sql = "update " + _table + " set " + _crispoColumn + 
                         "=" + crispoId + " where id=" + fromId;
            stmt = getConnection().createStatement();
            int updateRes = stmt.executeUpdate(sql); 
            if (updateRes != 1) {
                throw new BuildException("Update [" + sql + "] did not " + 
                                         "update 1 row (updated " + updateRes +
                                         ")");
            }
        } finally {
            DBUtil.closeJDBCObjects(LOGCTX, null, stmt, null);
        }
    }
    
    private void rewriteConfigResponse(long rowId, ConfigResponse cr) 
        throws SQLException, BuildException, EncodingException
    {
        PreparedStatement stmt = null;
        
        try {
            String sql = "update " + _table + " set " + _column + 
                         " = ? where id = " + rowId;
            byte[] bytes;
            
            if (_onlyProperties != null) {
                for (Iterator i = new HashSet(cr.getKeys()).iterator(); 
                     i.hasNext(); )
                {
                    String key = (String) i.next();
                    
                    if (keyMatchesFilter(key)) {
                        cr.unsetValue(key);
                    }
                }
            }
            bytes = cr.encode();
            stmt = getConnection().prepareStatement(sql);
            stmt.setBytes(1, bytes);
            int numRows = stmt.executeUpdate();
            if (numRows != 1) {
                throw new BuildException("Updated " + numRows + " instead of 1");
            }
        } finally {
            DBUtil.closeJDBCObjects(LOGCTX, null, stmt, null);
        }
    }

    public void execute()
        throws BuildException
    {
        try {
            _execute();
        } catch(BuildException e) {
            e.printStackTrace();
            throw e;
        } catch(RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    private void _execute()
        throws BuildException
    {
        if (_table == null || _column == null || _crispoColumn == null) {
            throw new BuildException("table, column, and crispoColumn " +
                                     "must be specified");
        }

        checkDBCols();
        Map idMap = new HashMap();
        setCRIds(idMap);
        updateCrispos(idMap);
    }

    private void checkDBCols()
        throws BuildException
    {
        try
        {
            if (!DBUtil.checkColumnExists(LOGCTX.getName(), getConnection(), 
                                          _table, _column)) 
            {
                throw new BuildException("Column containing ConfigResponse (" + 
                                         _column + ") in table (" + _table + 
                                         ") does not exist");
            }
            if (!DBUtil.checkColumnExists(LOGCTX.getName(), getConnection(), 
                                          _table, _crispoColumn)) 
            {
                throw new BuildException("Column to place new crispo ID (" + 
                                         _crispoColumn + ") in table (" + 
                                         _table + ") does not exist"); 
            }
        } catch(SQLException e) {
            throw new BuildException(e.getMessage(), e);
        }
    }

    private void setCRIds(Map idMap)
        throws BuildException
    {
        Statement stmt  = null;
        ResultSet rs    = null;
        try
        {
            Connection conn = getConnection();
            stmt = conn.createStatement();
            
            String sql = "select id, " + _column + " from " + _table;
            rs = stmt.executeQuery(sql);
            int id_col = rs.findColumn("id"),
                col2 = rs.findColumn(_column);
            while (rs.next())
            {
                long fromId = rs.getLong(id_col);
                byte[] b = rs.getBytes(col2);
                ConfigResponse cr;
                if (b == null)
                    cr = new ConfigResponse();
                else
                    cr = ConfigResponse.decode(b);

                idMap.put(new Long(fromId), cr);
            }
        } catch (EncodingException e) {
            throw new BuildException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new BuildException(e.getMessage(), e);
        } finally {
            DBUtil.closeJDBCObjects(LOGCTX, null, stmt, rs);
        }
    }

    private void updateCrispos(Map idMap)
        throws BuildException
    {
        try
        {
            Connection conn = getConnection();
            Dialect dialect = HibernateUtil.getDialect(conn);
            for (Iterator i=idMap.entrySet().iterator(); i.hasNext(); )
            {
                Map.Entry entry = (Map.Entry)i.next();
                long fromId = ((Long)entry.getKey()).longValue();
                ConfigResponse cr = (ConfigResponse)entry.getValue();

                long crispoId = createCrispo(dialect, cr);
                updateRowWithCrispo(fromId, crispoId);
                
                if (_rewriteConfigResponse != null &&
                    "true".equalsIgnoreCase(_rewriteConfigResponse) ||
                    "t".equalsIgnoreCase(_rewriteConfigResponse) ||
                    "y".equalsIgnoreCase(_rewriteConfigResponse) ||
                    "yes".equalsIgnoreCase(_rewriteConfigResponse))
                {
                    rewriteConfigResponse(fromId, cr);
                }
                log(_table + " (id=" + fromId + ") now has " + _crispoColumn + 
                    "=" + crispoId);
            }

        } catch (EncodingException e) {
            throw new BuildException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new BuildException(e.getMessage(), e);
        }
    }
}
