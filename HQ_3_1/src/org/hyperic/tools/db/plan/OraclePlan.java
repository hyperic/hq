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

package org.hyperic.tools.db.plan;

import java.sql.Connection;
import java.sql.SQLException;

public class OraclePlan extends Plan
{
    private static final int DB_ERR_NO_TABLE = 942;
    
    private static final String CREATE_PLAN_TABLE =
        "create table PLAN_TABLE (" +
            "statement_id    varchar2(30)," +
            "timestamp       date," +
            "remarks         varchar2(80)," +
            "operation       varchar2(30)," +
            "options         varchar2(255)," +
            "object_node     varchar2(128)," +
            "object_owner    varchar2(30)," +
            "object_name     varchar2(30)," +
            "object_instance numeric," +
            "object_type     varchar2(30)," +
            "optimizer       varchar2(255)," +
            "search_columns  number," +
            "id      numeric," +
            "parent_id   numeric," +
            "position    numeric," +
            "cost        numeric," +
            "cardinality numeric," +
            "bytes       numeric," +
            "other_tag       varchar2(255)," +
            "partition_start varchar2(255)," +
                "partition_stop  varchar2(255)," +
                "partition_id    numeric," +
            "other       long," +
            "distribution    varchar2(30)," +
            "cpu_cost    numeric," +
            "io_cost     numeric," +
            "temp_space  numeric," +
                "access_predicates varchar2(4000)," +
                "filter_predicates varchar2(4000))";

    protected void printPlan(Connection conn, String cmd) throws SQLException {
        try {
            conn.createStatement().execute("DELETE FROM PLAN_TABLE");
            conn.commit();
        } catch(SQLException e) {
            if(e.getErrorCode() == DB_ERR_NO_TABLE) {
                conn.createStatement().execute(CREATE_PLAN_TABLE);
            }
        }
        
        conn.createStatement().execute("EXPLAIN PLAN INTO PLAN_TABLE FOR " + cmd);

        DBPlan.printResults(
            conn.createStatement().executeQuery("SELECT * FROM PLAN_TABLE") );
        System.out.println();
    }
}
