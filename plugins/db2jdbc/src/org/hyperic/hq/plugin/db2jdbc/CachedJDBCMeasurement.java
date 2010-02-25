/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.db2jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.jdbc.DBUtil;

/**
 *
 * @author laullon
 */
public abstract class CachedJDBCMeasurement extends JDBCMeasurementPlugin {

    private static final String EXEC_TIME_ATTR = "QueryExecTime";
    private static final Hashtable cache = new Hashtable();

    protected void initQueries() {
    }

    protected String getDefaultURL() {
        return "";
    }

    public MetricValue getValue(Metric metric) throws MetricUnreachableException, MetricNotFoundException {
        Map values;
        String key = getCacheKey(metric);
        CacheEntry ce = (CacheEntry) cache.get(key);
        if (!isCacheEntryValid(ce, metric)) {
            try {
                values = executeQuery(metric);
                cache.put(key, new CacheEntry(values));
            } catch (MetricNotFoundException ex) {
                if (getLog().isDebugEnabled()) {
                    getLog().error("Metric: '" + metric + "' Error='" + ex.getMessage() + "'", ex);
                }
                if (metric.getAttributeName().endsWith(AVAIL_ATTR)) {
                    values = new HashMap();
                    values.put(metric.getAttributeName(), new MetricValue(Metric.AVAIL_DOWN));
                    cache.put(key, new CacheEntry(values));
                } else {
                    throw ex;
                }
            }

        } else {
            values = (Map) ce.getEntry();
        }
        MetricValue res = (MetricValue) values.get(metric.getAttributeName());
        if (res == null) {
            throw new MetricNotFoundException(metric.getAttributeName() + " => NULL");
        }
        return res;
    }

    protected final Map executeQuery(Metric metric) throws MetricNotFoundException {
        Map res = new HashMap();
        res.put(AVAIL_ATTR, new MetricValue(Metric.AVAIL_DOWN));

        String query = getQuery(metric);
        if (query == null) {
            throw new IllegalArgumentException("No SQL query");
        }
        getLog().debug("[executeQuery] query='" + query + "'");

        String url = metric.getProperties().getProperty(PROP_URL);
        String user = metric.getProperties().getProperty(PROP_USER);
        String pass = metric.getProperties().getProperty(PROP_PASSWORD);

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        PreparedStatement tps = null;
        try {
            conn = getCachedConnection(url, user, pass);
            tps = conn.prepareStatement("select 1 from sysibm.sysdummy1");
            tps.execute();
            getLog().debug("** cn - " + url + user + pass + " - " + conn);
        } catch (SQLException e) {
            PreparedStatement tps2 = null;
            try {
                getLog().debug("conection closed '" + e.getMessage() + "'... reconectinig (" + url + ")");
                removeCachedConnection(url, user, pass);
                conn = getCachedConnection(url, user, pass);
                tps2 = conn.prepareStatement("select 1 from sysibm.sysdummy1");
                tps2.execute();
            } catch (SQLException ex) {
                removeCachedConnection(url, user, pass);
                throw new MetricNotFoundException(ex.getMessage());
            } finally {
                if (tps2 != null) {
                    try {
                        tps2.close();
                    } catch (SQLException ex) {
                        removeCachedConnection(url, user, pass);
                        throw new MetricNotFoundException(ex.getMessage());
                    }
                }
            }
        } finally {
            if (tps != null) {
                try {
                    tps.close();
                } catch (SQLException ex) {
                    removeCachedConnection(url, user, pass);
                    throw new MetricNotFoundException(ex.getMessage());
                }
            }
        }

        try {
            ps = conn.prepareStatement(query);
            long startTime = System.currentTimeMillis();

            rs = ps.executeQuery();
            long totalTime = System.currentTimeMillis() - startTime;
            res.put(EXEC_TIME_ATTR, new MetricValue(totalTime));

            res.put(AVAIL_ATTR, new MetricValue(Metric.AVAIL_UP));
            if (getLog().isTraceEnabled()) {
                getLog().trace("key='" + AVAIL_ATTR + "'\tvalue='" + res.get(AVAIL_ATTR) + "'(" + res.get(AVAIL_ATTR).getClass() + ")");
            }

            res.putAll(processResulSet(rs, metric));
        } catch (SQLException ex) {
            getLog().error("Error fetching metrics", ex);
            removeCachedConnection(url, user, pass);
            throw new MetricNotFoundException(ex.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(getLog(), null, ps, rs);
        }
        return res;
    }

    abstract Map processResulSet(
            ResultSet rs, Metric metric) throws MetricNotFoundException;

    private String getCacheKey(
            Metric metric) {
        return getQuery(metric) + metric.getProperties().toString();
    }

    private boolean isCacheEntryValid(CacheEntry ce, Metric metric) {
        long now = System.currentTimeMillis() / 1000;
        if (getLog().isDebugEnabled()) {
            getLog().debug("-->getValue(" + metric + ")");
            if (ce == null) {
                getLog().debug("== ce==null");
            } else if (metric.getAttributeName().equals(AVAIL_ATTR)) {
                getLog().debug("== AVAIL_ATTR");
            } else if (metric.getAttributeName().equals(EXEC_TIME_ATTR)) {
                getLog().debug("== EXEC_TIME_ATTR");
            } else if ((now - ce.getTime()) > 60) {
                getLog().debug("== time='" + (now - ce.getTime()) + "'");
            }

        }
        boolean invalid = (ce == null) ||
                ((now - ce.getTime()) > 60) ||
                metric.getAttributeName().endsWith(AVAIL_ATTR) ||
                metric.getAttributeName().equals(EXEC_TIME_ATTR);

        if (getLog().isDebugEnabled()) {
            if (invalid) {
                getLog().debug("*** NO CACHE ***");
            } else {
                getLog().debug("*** CACHE HIT ***");
            }

        }

        return !invalid;
    }

    private final class CacheEntry {

        private long time;
        private Object entry;

        public CacheEntry(Object entry) {
            this.entry = entry;
            time = System.currentTimeMillis() / 1000;
        }

        public Object getEntry() {
            return entry;
        }

        public long getTime() {
            return time;
        }
    }
}
