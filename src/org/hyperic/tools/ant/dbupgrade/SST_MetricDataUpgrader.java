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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import org.hyperic.tools.db.TypeMap;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.MeasTabManagerUtil;
import org.hyperic.hq.measurement.shared.MeasRangeObj;
import org.hyperic.hq.measurement.shared.MeasRange;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.jdbc.JDBC;
import org.hyperic.util.TimeUtil;

public class SST_MetricDataUpgrader extends SchemaSpecTask {

    public static final String logCtx = SST_MetricDataUpgrader.class.getName();
    private static final String TAB_COMPAT = MeasTabManagerUtil.OLD_MEAS_TABLE;

    public static final String SCHEMA_MOD_IN_PROGRESS
        = " *** UPGRADE TASK: migrating "+TAB_COMPAT+" to new "+
          "Data Tables ";

    public SST_MetricDataUpgrader () {}

    public void execute () throws BuildException
    {
        Connection conn = null;
        Statement stmt  = null;
        ResultSet rs    = null;
        try
        {
            conn = getConnection();
            stmt = conn.createStatement();
            MeasRangeObj ranges = MeasRangeObj.getInstance();
            log(SCHEMA_MOD_IN_PROGRESS);
            for (Iterator i=ranges.getRanges().iterator(); i.hasNext(); )
            {
                MeasRange range = (MeasRange)i.next();
                long max = range.getMaxTimestamp(),
                     min = range.getMinTimestamp();
                String table = range.getTable();
                String sql = "INSERT into "+table+" (measurement_id, timestamp, value)"+
                             " SELECT measurement_id, timestamp, value"+
                             " FROM "+TAB_COMPAT+
                             " WHERE timestamp BETWEEN "+min+" AND "+max;
                int rows = stmt.executeUpdate(sql);
                log("Moved "+rows+" rows from "+TAB_COMPAT+" to "+table+
                    " min -> "+TimeUtil.toString(min)+" ("+min+")"+
                    " max -> "+TimeUtil.toString(max)+" ("+max+")");
            }
            stmt.execute("TRUNCATE TABLE "+TAB_COMPAT);
        }
        catch (SQLException e) {
            throw new BuildException(logCtx+": " + e.getMessage(), e);
        }
        finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
        }
    }
}
