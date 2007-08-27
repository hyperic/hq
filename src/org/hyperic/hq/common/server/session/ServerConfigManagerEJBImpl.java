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

package org.hyperic.hq.common.server.session;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.ConfigProperty;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.ServerConfigManagerLocal;
import org.hyperic.hq.common.shared.ServerConfigManagerUtil;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.dao.ConfigPropertyDAO;
import org.hyperic.hq.measurement.shared.MeasTabManagerUtil;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.timer.StopWatch;
import org.safehaus.uuid.EthernetAddress;
import org.safehaus.uuid.UUIDGenerator;
import org.hibernate.mapping.Table;

/**
 * This class is responsible for setting/getting the server 
 * configuration
 * @ejb:bean name="ServerConfigManager"
 *      jndi-name="ejb/common/ServerConfigManager"
 *      local-jndi-name="LocalServerConfigManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class ServerConfigManagerEJBImpl implements SessionBean {
    private Log _log = LogFactory.getLog(ServerConfigManagerEJBImpl.class);

    private final String SQL_VACUUM  = "VACUUM ANALYZE {0}";
    //only for the metric data tables
    private final String SQL_REINDEX = "REINDEX TABLE {0}";

    private final int DEFAULT_COST = 15;

    private final String[] APPDEF_TABLES
        = { "EAM_PLATFORM", "EAM_SERVER", "EAM_SERVICE", "EAM_CONFIG_RESPONSE",
            "EAM_AGENT", "EAM_IP", "EAM_MEASUREMENT_ARG", "EAM_MEASUREMENT", 
            "EAM_SRN", "EAM_RESOURCE", "EAM_CPROP_KEY" };

    private final String[] DATA_TABLES 
        = { "EAM_MEASUREMENT_DATA_1D", "EAM_MEASUREMENT_DATA_6H",
            "EAM_MEASUREMENT_DATA_1H", "HQ_METRIC_DATA_COMPAT",
            "EAM_METRIC_PROB", "EAM_REQUEST_STAT",
            "EAM_ALERT_ACTION_LOG", "EAM_ALERT_CONDITION_LOG",
            "EAM_ALERT", "EAM_EVENT_LOG", "EAM_CPROP" };
                         
    public final String logCtx
        = "org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl";
    protected Log log = LogFactory.getLog(logCtx);

    /**
     * Get the "root" server configuration, that means those keys that have 
     * the NULL prefix.
     * @return Properties
     * @ejb:interface-method
     */
    public Properties getConfig() throws ConfigPropertyException {
        return getConfig(null);
    }

    /**
     * Get the server configuration
     * @param prefix The prefix of the configuration to retrieve.
     * @return Properties
     * @ejb:interface-method
     */
    public Properties getConfig(String prefix) throws ConfigPropertyException {

        try {
            ConfigPropertyDAO dao =
                DAOFactory.getDAOFactory().getConfigPropertyDAO();
            Collection allProps = getProps(dao, prefix);
            Properties props = new Properties();
            String key;

            for(Iterator i = allProps.iterator(); i.hasNext();) {
                ConfigProperty configProp = (ConfigProperty)i.next();
                key = configProp.getKey();
                // Check if the key has a value
                if (configProp.getValue() != null &&
                    configProp.getValue().length() != 0) {
                    props.setProperty(key, configProp.getValue());
                } else {
                    // Use defaults
                    if (configProp.getDefaultValue() != null) {
                        props.setProperty(key, configProp.getDefaultValue());
                    } else {
                        // Otherwise return an empty key.  We dont want to
                        // prune any keys from the config.
                        props.setProperty(key, "");
                    }
                } 
            }

            return props;
        } catch (FinderException e) {
            throw new ConfigPropertyException("Unable to find config property");
        }
    }

    /**
     * Set the server configuration
     *
     * @throws ConfigPropertyException - if the props object is missing
     * a key that's currently in the database
     * @ejb:interface-method
     */
    public void setConfig(Properties newProps) throws ApplicationException, 
        ConfigPropertyException {

        setConfig(null, newProps);
    }

    /**
     * Set the server Configuration
     * @param prefix The config prefix to use when setting properties.  The prefix
     * is used for namespace protection and property scoping.
     * @param newProps The Properties to set.
     * @throws ConfigPropertyException - if the props object is missing
     * a key that's currently in the database
     * @ejb:interface-method
     */
    public void setConfig(String prefix, Properties newProps)
        throws ApplicationException, ConfigPropertyException {
                
        Collection allProps;
        String key;
        String propValue;
        Properties tempProps = new Properties();
        tempProps.putAll(newProps);
        try {
            ConfigPropertyDAO ccLH =
                DAOFactory.getDAOFactory().getConfigPropertyDAO();
            // get all properties
            allProps = getProps(ccLH, prefix);
            // iterate over ejbs
            for(Iterator i = allProps.iterator(); i.hasNext();) {
                ConfigProperty ejb = (ConfigProperty)i.next();

                // check if the props object has a key matching
                key = ejb.getKey();
                if(newProps.containsKey(key)) {
                    tempProps.remove(key);
                    propValue = (String)newProps.get(key);
                    // delete null values from prefixed properties
                    if ( prefix != null &&
                         (propValue == null || propValue.equals("NULL")) ) {
                        ccLH.remove(ejb);
                    } else {
                        // non-prefixed properties never get deleted.
                        ejb.setValue(propValue);
                    }
                } else if ( prefix == null ) {
                    // Bomb out if props are missing for non-prefixed properties
                    throw new ConfigPropertyException(
                        "Updated configuration missing required key: " + key);
                }
            }

            // create properties that are still left in tempProps
            if (tempProps.size() > 0 ) {
                Enumeration propsToAdd = tempProps.propertyNames();
                while ( propsToAdd.hasMoreElements() ) {
                    key = (String) propsToAdd.nextElement();
                    propValue = tempProps.getProperty(key);
                    // create the new property
                    ccLH.create(prefix, key, propValue, propValue);
                }
            }
        } catch (FinderException e) {
            throw new ApplicationException("Unable to find config property", e);
        }
    }

    /**
     * Run an analyze command on all non metric tables.  The metric tables are
     * handled seperately using analyzeHqMetricTables() so that only the tables
     * that have been modified are analyzed.
     *
     * @return The time taken in milliseconds to run the command.
     * @ejb:transaction type="NOTSUPPORTED"
     * @ejb:interface-method
     */
    public long analyzeNonMetricTables() {

        HQDialect dialect = Util.getHQDialect();
        long duration = 0;

        Connection conn  = null;
        try {
            conn = DBUtil.getConnByContext(getInitialContext(),
                                           HQConstants.DATASOURCE);

            for (Iterator i = Util.getTableMappings(); i.hasNext();) {                
                Table t = (Table)i.next();

                if (t.getName().toUpperCase().startsWith("EAM_MEASUREMENT_DATA") ||
                    t.getName().toUpperCase().startsWith("HQ_METRIC_DATA") ||
                    t.getName().toUpperCase().startsWith("HQ_SEQUENCE")) {
                    continue;
                }
                
                String sql = dialect.getOptimizeStmt(t.getName(),
                                                     DEFAULT_COST);
                duration += doCommand(conn, sql, null);
            }
        } catch(SQLException e){
            log.error("Error analyzing table", e);
        } catch (NamingException e) {
            throw new SystemException(e);
        } finally{
            DBUtil.closeConnection(logCtx, conn);
        }

        return duration;
    }

    /**
     * Run an analyze command on both the current measurement data slice as
     * well as the previous data slice.
     *
     * @return The time taken in milliseconds to run the command.
     * @ejb:transaction type="NOTSUPPORTED"
     * @ejb:interface-method
     */
    public long analyzeHqMetricTables()
    {
        long systime = System.currentTimeMillis();
        String currMetricDataTable = MeasTabManagerUtil.getMeasTabname(systime);
        long prevtime = MeasTabManagerUtil.getPrevMeasTabTime(systime);
        String prevMetricDataTable = MeasTabManagerUtil.getMeasTabname(prevtime);

        long duration = 0;
        HQDialect dialect = Util.getHQDialect();

        Connection conn = null;
        try {
            conn = DBUtil.getConnByContext(getInitialContext(),
                                           HQConstants.DATASOURCE);
            String sql = dialect.getOptimizeStmt(currMetricDataTable,
                                                 DEFAULT_COST);
            duration += doCommand(conn, sql, null);
            sql = dialect.getOptimizeStmt(prevMetricDataTable,
                                          DEFAULT_COST);
            duration += doCommand(conn, sql, null);
        } catch (SQLException e) {
            log.error("Error analyzing metric tables", e);
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeConnection(logCtx, conn);
        }
        return duration;
    }

    /**
     * Run a REINDEX command on all HQ data tables
     * @return The time it took to re-index in milliseconds, or -1 if the
     * database is not PostgreSQL.
     * @ejb:transaction type="NOTSUPPORTED"
     * @ejb:interface-method
     */
    public long reindex() {
        Connection conn = null;
        try {
            conn = DBUtil.getConnByContext(getInitialContext(),
                                           HQConstants.DATASOURCE);
            long duration = 0;
            if (DBUtil.isOracle(conn)) {
                log.info("Oracle should update statistics for the " +
                         "cost-based optimizer");
            }
            else {
                if (DBUtil.getDBType(conn) == DBUtil.DATABASE_POSTGRESQL_7) {
                    for (int i = 0; i < DATA_TABLES.length; i++) {
                        duration += doCommand(conn, SQL_REINDEX, DATA_TABLES[i]);
                    }
                }
                else if (DBUtil.getDBType(conn) == DBUtil.DATABASE_POSTGRESQL_8)
                    log.info("PostgreSQL 8.0 and later does not require " +
                             "re-indexing");
                else
                    return -1;
            }

            return duration;
        } catch (SQLException e) {
            log.error("Error creating database connection", e);
            throw new SystemException("Error reindexing database", e);
        } catch (NamingException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeConnection(logCtx, conn);
        }
    }

    /**
     * Run database-specific cleanup routines -- on PostgreSQL we
     * do a VACUUM ANALYZE.  On other databases we just return -1.
     * Since 3.1 we do not want to vacuum the hq_metric_data tables,
     * only the compressed eam_measurement_xxx tables.
     * 
     * @return The time it took to vaccum, in milliseconds, or -1 if the 
     * database is not PostgreSQL.
     * @ejb:transaction type="NOTSUPPORTED"
     * @ejb:interface-method
     */
    public long vacuum() {
        Connection conn = null;
        long duration = 0;
        try {
            conn = DBUtil.getConnByContext(getInitialContext(),
                                           HQConstants.DATASOURCE);
            if (!DBUtil.isPostgreSQL(conn))
                return -1;

            for (int i = 0; i < DATA_TABLES.length; i++) {
                duration += doCommand(conn, SQL_VACUUM, DATA_TABLES[i]);
            }

            return duration;
        } catch (SQLException e) {
            log.error("Error vacuuming database: " + e.getMessage(), e);
            return duration;
        } catch (NamingException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeConnection(logCtx, conn);
        }
    }

    /**
     * Run database-specific cleanup routines on appdef tables -- on PostgreSQL 
     * we do a VACUUM ANALYZE against the relevant appdef, authz and measurement
     * tables.  On other databases we just return -1.
     * @return The time it took to vaccum, in milliseconds, or -1 if the 
     * database is not PostgreSQL.
     * @ejb:transaction type="NOTSUPPORTED"
     * @ejb:interface-method
     */
    public long vacuumAppdef()
    {
        Connection conn = null;
        long duration = 0;
        try {
            conn = DBUtil.getConnByContext(getInitialContext(),
                                           HQConstants.DATASOURCE);
            if (!DBUtil.isPostgreSQL(conn))
                return -1;

            for (int i = 0; i < APPDEF_TABLES.length; i++) {
                duration +=
                    doCommand(conn, SQL_VACUUM, APPDEF_TABLES[i]);
            }
            return duration;
        } catch (SQLException e) {
            log.error("Error vacuuming database: " + e.getMessage(), e);
            return duration;
        } catch (NamingException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeConnection(logCtx, conn);
        }
    }

    private long doCommand(Connection conn, String sql, String table)
        throws SQLException
    {
        Statement stmt = null;
        StopWatch watch = new StopWatch();

        if (table == null)
            table = "";
        
        sql = StringUtil.replace(sql, "{0}", table);
        
        if (log.isDebugEnabled()) {
            log.debug("Execute command: " + sql);
        }

        try {
            stmt = conn.createStatement();
            stmt.execute(sql);
            return watch.getElapsed();
        } finally {
            DBUtil.closeStatement(logCtx, stmt);
        }
    }

    private Collection getProps(ConfigPropertyDAO ccLH,
                                String prefix) throws FinderException {
        if ( prefix == null ) {
            return ccLH.findAll();
        } else {
            return ccLH.findByPrefix(prefix);
        }
    }

    /**
     * Gets the GUID for this HQ server instance.  The GUID is persistent for
     * the duration of an HQ install and is created upon the first call of
     * this method.  If for some reason it can't be determined, 'unknown'
     * will be returned.
     * 
     * @ejb:interface-method
     */
    public String getGUID() {
        Properties p;
        String res;
        
        try {
            p = getConfig();
        } catch(Exception e) {
            throw new SystemException(e);
        }
        
        res = p.getProperty("HQ-GUID");
        if (res == null || res.trim().length() == 0) {
            if ((res = GUIDGenerator.createGUID()) == null)
                return "unknown";  
            p.setProperty("HQ-GUID", res);
            try {
                setConfig(p);
            } catch(Exception e) {
                throw new SystemException(e);
            }
        }
        return res;
    }

    private static InitialContext ic = null;
    protected InitialContext getInitialContext() {
        if (ic == null) {
            try {
                ic = new InitialContext();
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return ic;
    }

    public static ServerConfigManagerLocal getOne() {
        try {
            return ServerConfigManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    public void ejbCreate() { }
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
    public void setSessionContext(SessionContext c) {}
}
