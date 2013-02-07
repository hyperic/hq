/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2013], Hyperic, Inc.
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
package org.hyperic.hq.plugin.jboss7;

import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.jboss7.objects.DataSource;
import org.hyperic.hq.product.PluginException;

public class DataSourceCollector extends JBoss7DefaultCollector {

    private static final Log log = LogFactory.getLog(DataSourceCollector.class);

    @Override
    public void collect(JBossAdminHttp admin) {
        String ds = (String) getProperties().get("datasource");
        try {
            DataSource datasource = admin.getDatasource(ds, true, getPlugin().getTypeInfo().getVersion());
            setAvailability(datasource.isEnabled());
            setValue("ActiveCount", datasource.getActiveCount());
            setValue("AvailableCount", datasource.getAvailableCount());
            setValue("AverageBlockingTime", datasource.getAverageBlockingTime());
            setValue("AverageCreationTime", datasource.getAverageCreationTime());
            setValue("CreatedCount", datasource.getCreatedCount());
            setValue("DestroyedCount", datasource.getDestroyedCount());
            setValue("MaxCreationTime", datasource.getMaxCreationTime());
            setValue("MaxUsedCount", datasource.getMaxUsedCount());
            setValue("MaxWaitCount", datasource.getMaxWaitCount());
            setValue("MaxWaitTime", datasource.getMaxWaitTime());
            setValue("PreparedStatementCacheAccessCount", datasource.getPreparedStatementCacheAccessCount());
            setValue("PreparedStatementCacheAddCount", datasource.getPreparedStatementCacheAddCount());
            setValue("PreparedStatementCacheCurrentSize", datasource.getPreparedStatementCacheCurrentSize());
            setValue("PreparedStatementCacheDeleteCount", datasource.getPreparedStatementCacheDeleteCount());
            setValue("PreparedStatementCacheHitCount", datasource.getPreparedStatementCacheHitCount());
            setValue("PreparedStatementCacheMissCount", datasource.getPreparedStatementCacheMissCount());
            setValue("TimedOut", datasource.getTimedOut());
            setValue("TotalBlockingTime", datasource.getTotalBlockingTime());
            setValue("TotalCreationTime", datasource.getTotalCreationTime());
        } catch (PluginException ex) {
            setAvailability(false);
            log.debug(ex.getMessage(), ex);
        }
    }

    @Override
    public Log getLog() {
        return log;
    }
}
