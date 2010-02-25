/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.db2jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricValue;

/**
 *
 * @author laullon
 */
public class Measurement extends CachedJDBCMeasurement {

    private static final String KEY = "key";
    private static final String EXEC_TIME_ATTR = "QueryExecTime";
    private static final Hashtable cache = new Hashtable();

    protected String getQuery(Metric metric) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("*******************");
            getLog().debug("** MT " + metric);
            getLog().debug("** DN " + metric.getDomainName());
            getLog().debug("** OP " + metric.getObjectProperties());
            getLog().debug("** ON " + metric.getObjectName());
            getLog().debug("** AT " + metric.getAttributeName());
            getLog().debug("** PR " + metric.getProperties());
            getLog().debug("*******************");
        }
        String sql = metric.getObjectProperties().getProperty("sql");
        if (sql == null) {
            String func = metric.getObjectProperties().getProperty("func") + "('" + metric.getProperties().getProperty("database");
            String where = metric.getObjectProperties().getProperty("where") != null ? " where " + metric.getObjectProperties().getProperty("where") : "";
            sql = "SELECT * FROM TABLE(" + func + "', -2)) as t" + where;
        }
        return sql;
    }

    protected void getDriver() throws ClassNotFoundException {
        try {
            Class.forName("com.ibm.db2.jcc.DB2Driver");
        } catch (ClassNotFoundException ex) {
            getLog().error(ex.getMessage(), ex);
            throw ex;
        }
    }

    protected Connection getConnection(String url, String user, String pass) throws SQLException {
        getLog().debug("[getConnection] url='" + url + "' user='" + user + "' pass='******'");
        return DriverManager.getConnection(url, user, pass);
    }

    Map processResulSet(ResultSet rs, Metric metric) throws MetricNotFoundException {
        Map res = new HashMap();
        String prefix = "";
        try {
            if (rs.next()) {
                if (metric.getObjectProperty(KEY) != null) {
                    prefix = rs.getString(metric.getObjectProperty(KEY)) + ".";
                    res.put(prefix + AVAIL_ATTR, new MetricValue(1));
                }
                ResultSetMetaData md = rs.getMetaData();
                for (int c = 1; c <= md.getColumnCount(); c++) {
                    String key = md.getColumnLabel(c);
                    double val = Double.NaN;
                    switch (md.getColumnType(c)) {
                        case Types.DECIMAL:
                        case Types.BIGINT:
                        case Types.INTEGER:
                        case Types.SMALLINT:
                            val = rs.getDouble(c);
                            break;
                        case Types.TIMESTAMP:
                            Timestamp ts = rs.getTimestamp(c);
                            val = (ts != null) ? ts.getTime() : Double.NaN;
                            break;
                        case Types.CHAR:
                        case Types.VARCHAR:
                            String v = rs.getString(c);
                            if (v != null) {
                                val = (v.equalsIgnoreCase("yes") ? Metric.AVAIL_UP : (v.equalsIgnoreCase("no") ? Metric.AVAIL_DOWN : Double.NaN));
                                if (new Double(val).equals(new Double(Double.NaN))) {
                                    res.put("raw." + key, v);
                                    if (getLog().isTraceEnabled()) {
                                        getLog().trace("key='raw." + key + "'\tvalue='" + v + "'");
                                    }
                                }
                            }
                            break;
                        default:
                            getLog().debug(key + "==>" + md.getColumnClassName(c));
                            assert false : key + "==>" + "(type='" + md.getColumnType(c) + "')" + md.getColumnClassName(c);
                    }
                    key = prefix + key;
                    if (getLog().isTraceEnabled()) {
                        getLog().trace("key='" + key + "'\tvalue='" + val + "'");
                    }
                    res.put(key, new MetricValue(val));
                }
            } else { // NO ROWS
                throw new MetricNotFoundException("No row selectd");
            }
            // +1 rows
            assert !rs.next() : getQuery(metric);
            try {
                postProcessResults(res);
            } catch (Throwable e) {
                assert false : e;
                getLog().debug("Error '" + e.getMessage() + "'", e);
            }
        } catch (SQLException e) {
            throw new MetricNotFoundException(e.getMessage());
        }
        return res;
    }

    protected void postProcessResults(Map results) {
    }
}
