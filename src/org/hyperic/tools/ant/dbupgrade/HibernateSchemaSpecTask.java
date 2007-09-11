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
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.tools.ant.BuildException;
import org.hibernate.dialect.Dialect;
import org.hyperic.hibernate.HibernateUtil;
import org.hyperic.util.jdbc.DBUtil;

public abstract class HibernateSchemaSpecTask extends SchemaSpecTask {
    private String  _logCtx;
    
    protected HibernateSchemaSpecTask(String logCtx) {
        _logCtx = logCtx;
    }
    
    protected Dialect getDialect() {
        try {
            return HibernateUtil.getDialect(getConnection());
        } catch(Exception e) {
            throw new RuntimeException("Unable to get dialect", e);
        }
    }
    
    protected int executeSQL(String sql) {
        Statement stmt = null;
        
        try {
            Connection conn = getConnection();
            stmt = conn.createStatement();
            
            log(">>>>>>  Executing update [" + sql + "]");
            return stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new BuildException(e.getMessage(), e);
        } finally {
            DBUtil.closeJDBCObjects(_logCtx, null, stmt, null);
        }
    }
    
    protected int executeSQL(String[] statements) {
        int res = 0;
        
        for (int i=0; i<statements.length; i++ ) {
            res += executeSQL(statements[i]);
        }
        return res;
    }
}
