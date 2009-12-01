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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.hyperic.util.jdbc.DBUtil;

public class SST_Prepend extends SST_Update {

    private String pkColumn = null;

    public SST_Prepend () {}

    public void setPkColumn ( String pk ) {
        this.pkColumn = pk;
    }

    public void execute () throws BuildException {

        validateAttributes();

        Connection c = getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;

        String selectSql
            = "SELECT " + pkColumn + ", " + column + " FROM " + table;
        if ( where != null ) { selectSql += " WHERE " + where; }

        String updateSql
            = "UPDATE " + table + " SET " + column + " = ? WHERE ID = ?";

        Map pkMap = new HashMap();

        try {
            // Check to see if the column exists.
            boolean foundColumn = DBUtil.checkColumnExists(_ctx, c, 
                                                           table, column);
            if ( !foundColumn ) {
                throw new BuildException("Cannot update: column " + column
                                         + " does not exist in table " + table);
            }

            // Grab the data to update
            ps = c.prepareStatement(selectSql);

            String whereDebug = (where==null) ? "" : " where " + where;
            log(">>>>> Searching for PKs in " 
                + table + "." + pkColumn + whereDebug);

            rs = ps.executeQuery();
            while (rs.next()) {
                pkMap.put(rs.getObject(1), rs.getString(2));
            }
            DBUtil.closeResultSet(_ctx, rs);
            DBUtil.closeStatement(_ctx, ps);

            ps = c.prepareStatement(updateSql);
            Iterator i = pkMap.keySet().iterator();
            Object pk;
            String val;
            while (i.hasNext()) {
                pk = i.next();
                val = value + pkMap.get(pk).toString();
                ps.setString(1, val);
                ps.setObject(2, pk);
                log(">>>>> Updating " + table + "." + column 
                    + "=" + val);
                ps.executeUpdate();
            }
        } catch ( Exception e ) {
            throw new BuildException("Error updating " + table + "." + column 
                                     + ": " + e, e);
        } finally {
            DBUtil.closeResultSet(_ctx, rs);
            DBUtil.closeStatement(_ctx, ps);
        }
        
    }

    protected void validateAttributes () throws BuildException {
        super.validateAttributes();
        if ( pkColumn == null )
            throw new BuildException("SchemaSpec: update: No 'pkColumn' attribute specified.");
        if ( columnType != null ) 
            throw new BuildException("SchemaSpec: update: 'columnType' attribute not supported.");
    }
}
