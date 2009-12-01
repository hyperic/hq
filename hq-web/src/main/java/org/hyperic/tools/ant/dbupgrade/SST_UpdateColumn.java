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
import java.sql.ResultSetMetaData;

import org.apache.tools.ant.BuildException;
import org.hyperic.util.jdbc.DBUtil;

/**
 * Ant task to modifiy attributes of a given column.
 */
public class SST_UpdateColumn extends SchemaSpecTask {

    private String table = null;
    private String column = null;
    private String modifyCmd = null;

    public SST_UpdateColumn () {}

    public void setTable(String t) {
        table = t;
    }

    public void setColumn(String c) {
        column = c;
    }

    public void setModifyCmd(String m) {
        modifyCmd = m;
    }

    public void execute() throws BuildException {

        validateAttributes();

        Connection c = getConnection();
        PreparedStatement ps = null;

        String updateColumnSql 
            = "ALTER TABLE " + table + " MODIFY (" + column + 
            " " + modifyCmd + ")";

        try {
            // Check to see if the column exists.
            boolean foundColumn = DBUtil.checkColumnExists(_ctx, c, 
                                                           table, column);
            if ( !foundColumn ) {
                throw new BuildException("Cannot alter table: column " + column
                                         + " does not exist in table " + table);
            }

            ps = c.prepareStatement(updateColumnSql);
            log(">>>>> Updating " + table + "." + column +
                " " + modifyCmd);
            ps.executeUpdate();
        } catch ( Exception e ) {
            throw new BuildException("Error updating " + table + "." + column 
                                     + ": " + e, e);
        } finally {
            DBUtil.closeStatement(_ctx, ps);
        }
    }

    private void validateAttributes () throws BuildException {
        if (table == null)
            throw new BuildException("SchemaSpec: update: No 'table' " +
                                     "attribute specified.");
        if (column == null)
            throw new BuildException("SchemaSpec: update: No 'column' " +
                                     "attribute specified.");
        if (modifyCmd == null)
            throw new BuildException("SchemaSpec: update: No " +
                                     "'modifyCmd' attribute specified.");
    }
}
