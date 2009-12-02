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
import java.sql.Statement;

import org.apache.tools.ant.BuildException;
import org.hyperic.hq.install.InstallDBUtil;
import org.hyperic.util.jdbc.DBUtil;

public class SST_DropTable extends SchemaSpecTask {

    private String _table = null;
    private String _targetDB = null;

    public SST_DropTable () {}

    public void setTargetDB (String t) {
        _targetDB = t;
    }

    public void setTable (String t) {
        _table = t;
    }

    public void execute () throws BuildException {

        validateAttributes();

        Connection conn = getConnection();
        Statement stmt = null;
        String sql = "DROP TABLE " + _table + " CASCADE ";
        
        try
        {
            if (!targetDbIsValid(_targetDB)) {
                return;
            }
            if (DBUtil.isOracle(conn)) {
                sql += "CONSTRAINTS";
            }
            
            boolean foundTable = InstallDBUtil.checkTableExists(conn, _table);
            
            if ( !foundTable ) {
                log(">>>>> Not dropping table: " + _table
                    + " because it does not exist");
                return;
            }
            
            stmt = conn.createStatement();

            log(">>>>> Dropping table " + _table);
            stmt.execute(sql);

        } catch ( Exception e ) {
            throw new BuildException("Error dropping table " 
                                     + _table + ": " + e, e);
        } finally {
            DBUtil.closeStatement(_ctx, stmt);
        }
        
    }

    private void validateAttributes () throws BuildException {
        if ( _table == null )
            throw new BuildException("SchemaSpec: dropTable: No 'table' attribute specified.");
    }
}
