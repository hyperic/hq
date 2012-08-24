/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2012], Hyperic, Inc.
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
package org.hyperic.hq.product;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.ConnectionHandle;
import com.jolbox.bonecp.hooks.AbstractConnectionHook;
import com.jolbox.bonecp.hooks.AcquireFailConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
        final String key = url + "_" + user + "_" + password;
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
                config.setLazyInit(false); // do not use lazyinit, it will prevent for check config options.
                config.setAcquireRetryAttempts(0);

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
                + " c=" + cache.size() + " (" + key + ")");
        return conn;
    }

    public static void shutdown() {
        Iterator<String> keys = cache.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            BoneCP pool = cache.get(key);
            pool.shutdown();
        }
    }

    public static void resetPool(String url, String user, String password) {
        final String key = url + "_" + user + "_" + password;
        BoneCP pool = cache.get(key);
        if (pool != null) {
            pool.shutdown();
            cache.remove(key);
        }
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

        @Override
        public boolean onAcquireFail(Throwable t, AcquireFailConfig acquireConfig) {
            log.debug("[onAcquireFail] "+t,t);
            return false;
        }
    }
}
