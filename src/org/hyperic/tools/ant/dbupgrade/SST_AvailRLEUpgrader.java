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

package org.hyperic.tools.ant.dbupgrade;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.hyperic.hq.measurement.shared.MeasRange;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.jdbc.DBUtil;

public class SST_AvailRLEUpgrader extends SchemaSpecTask {

    public static final String logCtx = SST_AvailRLEUpgrader.class.getName();
    private static final String TAB_AVAIL_RLE = "HQ_AVAIL_DATA_RLE";

    public static final String SCHEMA_MOD_IN_PROGRESS
        = " *** UPGRADE TASK: updating "+TAB_AVAIL_RLE+" endtimes";

    public SST_AvailRLEUpgrader () {}

    public void execute() throws BuildException {
        Statement stmt  = null;
        ResultSet rs    = null;
        try
        {
            Connection conn = getConnection();
            stmt = conn.createStatement();
            log(SCHEMA_MOD_IN_PROGRESS);
            rs = stmt.executeQuery("select startime, endtime, measurement_id, "
                                        + "resource_id, availval from "
                                        + TAB_AVAIL_RLE +
                                  " order by resource_id, startime");
            
            ArrayList avails = new ArrayList();
            
            while (rs.next()) {
                avails.add(new AvailData(rs.getInt(1), rs.getInt(2),
                                         rs.getInt(3), rs.getInt(4),
                                         rs.getDouble(5)));
            }
            
            // Close the result set
            DBUtil.closeResultSet(logCtx, rs);
            
            // Fill in the end time for each
            AvailData prev = null;
            int count = 0;
            
            for (Iterator it = avails.iterator(); it.hasNext(); ) {
                AvailData current = (AvailData) it.next();
                if (prev == null || prev.getResource() != current.getResource())
                {
                    // check old resource
                    if (count > 1) {            // Only need to delete multiples
                        deleteRow(stmt, prev);
                        stmt.execute("insert into " + TAB_AVAIL_RLE +
                                     "(startime, measurement_id, " +
                                      "resource_id, availval) values (" +
                                      prev.getStartTime() + ", " +
                                      prev.getMetric() + ", " +
                                      prev.getResource() + ", " +
                                      prev.getAvailVal() + ")");
                    }
                    
                    // set new resource
                    prev = current;
                    count = 1;
                }
                else if (prev.getAvailVal() != current.getAvailVal()) {
                    deleteRow(stmt, prev);      // In case there were multiples
                    stmt.execute("insert into " + TAB_AVAIL_RLE +
                                 "(startime, endtime, measurement_id, " +
                                  "resource_id, availval) values (" +
                                  prev.getStartTime() + ", " +
                                  current.getStartTime() + ", " +
                                  prev.getMetric() + ", " +
                                  prev.getResource() + ", " +
                                  prev.getAvailVal() + ")");
                    
                    // set new row
                    prev = current;
                    count = 1;
                }
                else if (prev.getStartTime() != current.getStartTime()) {
                    // Delete it
                    deleteRow(stmt, current);
                }
                else {
                    // Same time, same resource
                    count++;
                }
            }
        }
        catch (SQLException e) {
            throw new BuildException(logCtx+": " + e.getMessage(), e);
        }
        finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
        }
    }

    private void deleteRow(Statement stmt, AvailData ad)
        throws SQLException {
        stmt.execute("delete from " + TAB_AVAIL_RLE + " where resource_id = " +
                     ad.getResource() + " and startime = " + ad.getStartTime());
    }
    
    private class AvailData {
        int _starttime;
        int _endtime;
        int _metric_id;
        int _resource_id;
        double _availval;
        
        public AvailData(int starttime, int endtime, int metric_id,
                         int resource_id, double availval) {
            super();
            _starttime = starttime;
            _endtime = endtime;
            _metric_id = metric_id;
            _resource_id = resource_id;
            _availval = availval;
        }

        public int getStartTime() {
            return _starttime;
        }

        public int getEndTime() {
            return _endtime;
        }

        public int getMetric() {
            return _metric_id;
        }

        public int getResource() {
            return _resource_id;
        }

        public double getAvailVal() {
            return _availval;
        }
        
        
    }
}
