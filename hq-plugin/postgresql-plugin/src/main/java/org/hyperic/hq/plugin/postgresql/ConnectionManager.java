/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.postgresql;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.ConnectionHandle;
import com.jolbox.bonecp.hooks.AbstractConnectionHook;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author laullon
 */
public class ConnectionManager {

    private final static Map<String, BoneCP> cache = new HashMap<String, BoneCP>();
    private static Log log = LogFactory.getLog(ConnectionManager.class);

    public static Connection getConnection(String url, String user, String password) throws SQLException {
        final String key = url + user + password;
        BoneCP pool = cache.get(key);
        if (pool == null) {
            synchronized (cache) {
                BoneCPConfig config = new BoneCPConfig();
                config.setJdbcUrl(url);
                config.setUsername(user);
                config.setPassword(password);

                config.setPartitionCount(1);
                config.setMinConnectionsPerPartition(1);
                config.setMaxConnectionsPerPartition(10);
                config.setIdleMaxAge(10, TimeUnit.MINUTES);
                config.setAcquireIncrement(1);
                config.setLazyInit(true);

                if (log.isDebugEnabled()) {
                    config.setCloseConnectionWatch(true);
                    config.setCloseConnectionWatchTimeout(1, TimeUnit.MINUTES);
                    config.setConnectionHook(new ConnectionHook(url));
                }

                pool = new BoneCP(config);
                cache.put(key, pool);
            }
        }
        final Connection conn = pool.getConnection();
        log.debug("[getConnection] u/f/t=" + pool.getStatistics().getTotalLeased()
                + "/" + pool.getStatistics().getTotalFree()
                + "/" + pool.getStatistics().getTotalCreatedConnections()
                + " c="+cache.size()+" (" + url + ")");
        return conn;
    }

    private static class ConnectionHook extends AbstractConnectionHook {

        private final String url;

        public ConnectionHook(String url) {
            this.url = url;
        }

        @Override
        public void onAcquire(ConnectionHandle connection) {
            log.debug("[onAcquire] on '" + url + "'");
        }

        @Override
        public void onDestroy(ConnectionHandle connection) {
            log.debug("[onDestroy] on '" + url + "'");
        }
    }
}
