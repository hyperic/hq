/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.jboss.jbossas7;

import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;

/**
 *
 * @author administrator
 */
public class DataSourceCollector extends JBoss7DefaultCollector {

    private static final Log log = LogFactory.getLog(DataSourceCollector.class);
    private static final String[] metrics = {
        "ActiveCount",
        "AvailableCount",
        "AverageBlockingTime",
        "AverageCreationTime",
        "CreatedCount",
        "DestroyedCount",
        "MaxCreationTime",
        "MaxUsedCount",
        "MaxWaitCount",
        "MaxWaitTime",
        "PreparedStatementCacheAccessCount",
        "PreparedStatementCacheAddCount",
        "PreparedStatementCacheCurrentSize",
        "PreparedStatementCacheDeleteCount",
        "PreparedStatementCacheHitCount",
        "PreparedStatementCacheMissCount",
        "TimedOut",
        "TotalBlockingTime",
        "TotalCreationTime"
    };

    @Override
    public void collect(JBossAdminHttp admin) {
        String ds = (String) getProperties().get("datasource");
        try {
            Map<String, String> datasource = admin.getDatasource(ds, true);
            setAvailability(datasource.get("enabled").equalsIgnoreCase("true"));
            for(String metric:metrics){
                setValue(metric, datasource.get(metric));
            }
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
