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
package org.hyperic.hq.plugin.postgresql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.lather.ControlSendCommandResult_args;
import org.hyperic.hq.product.JDBCControlPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.jdbc.DBUtil;

public abstract class ResourceControl extends JDBCControlPlugin {

    private static Log log = LogFactory.getLog(ResourceControl.class);
    protected String db;

    protected final Class getDriver() throws ClassNotFoundException {
        return Class.forName(ResourceMeasurement.JDBC_DRIVER);
    }

    protected final Connection getConnection(String url, String user, String password) throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public final void configure(ConfigResponse config) throws PluginException {
        log.debug("[configure] config=" + config);

        String DB = config.getValue(PostgreSQL.PROP_DB);
        String schema = config.getValue(PostgreSQL.PROP_SCHEMA);
        this.url = PostgreSQL.prepareUrl(config.toProperties(), DB);
        this.user = config.getValue(PostgreSQL.PROP_USER);
        this.password = config.getValue(PostgreSQL.PROP_PASS);

        this.db = config.getValue(PostgreSQL.PROP_DB);
        this.table = schema + "." + config.getValue(PostgreSQL.PROP_TABLE);
        this.index = schema + "." + config.getValue(PostgreSQL.PROP_INDEX);
    }

    @Override
    public final void doAction(String action) throws PluginException {
        doAction(action, new String[0]);
    }

    @Override
    public final void execute(String query) throws PluginException {
        Connection conn = null;
        Statement stmt = null;

        setResult(RESULT_FAILURE);

        try {
            conn = getConnection(url, user, password);
            stmt = conn.createStatement();
            stmt.executeQuery(query);

            setResult(RESULT_SUCCESS);
        } catch (SQLException e) {
            // Error running control command.
            setMessage("Error executing query:'" + query + "' > " + e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(getLog(), conn, stmt, null);
        }
    }
}
