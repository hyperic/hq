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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

public abstract class ResourceMeasurement extends JDBCMeasurementPlugin {

    protected static final String JDBC_DRIVER = "org.postgresql.Driver";

    @Override
    protected final void getDriver() throws ClassNotFoundException {
        Class.forName(JDBC_DRIVER);
    }

    @Override
    protected final Connection getConnection(String url, String user, String password) throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public final ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {
        return new ConfigSchema();
    }

    @Override
    protected final String getDefaultURL() {
        return null;
    }

    @Override
    protected final void initQueries() {
    }

    @Override
    protected abstract String getQuery(Metric metric);

    @Override
    public final MetricValue getValue(Metric metric) throws PluginException, MetricUnreachableException, MetricInvalidException, MetricNotFoundException {
        MetricValue res;
        if (metric.getDomainName().equals("collector")) {
            res = Collector.getValue(this, metric);
        } else {
            res = super.getValue(metric);
        }
        return res;
    }
}
