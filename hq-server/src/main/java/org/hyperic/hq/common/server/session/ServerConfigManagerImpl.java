/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.mapping.Table;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.ConfigProperty;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.shared.MeasTabManagerUtil;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for setting/getting the server configuration
 */
@Service("serverConfigManager")
public class ServerConfigManagerImpl implements ServerConfigManager {

    private static final String SQL_VACUUM = "VACUUM ANALYZE {0}";

    private static final int DEFAULT_COST = 15;

    private final DBUtil dbUtil;

    private final AuthzSubjectManager authzSubjectManager;

    private static final String[] APPDEF_TABLES = { "EAM_PLATFORM",
                                                   "EAM_SERVER",
                                                   "EAM_SERVICE",
                                                   "EAM_CONFIG_RESPONSE",
                                                   "EAM_AGENT",
                                                   "EAM_IP",
                                                   "EAM_RESOURCE",
                                                   "EAM_CPROP_KEY",
                                                   "EAM_AUDIT",
                                                   "EAM_AIQ_SERVER",
                                                   "EAM_AIQ_PLATFORM",
                                                   "EAM_RESOURCE_EDGE",
                                                   "EAM_RES_GRP_RES_MAP" };

    private static final String[] DATA_TABLES = { "EAM_MEASUREMENT_DATA_1D",
                                                 "EAM_MEASUREMENT_DATA_6H",
                                                 "EAM_MEASUREMENT_DATA_1H",
                                                 "HQ_METRIC_DATA_COMPAT",
                                                 "EAM_METRIC_PROB",
                                                 "EAM_REQUEST_STAT",
                                                 "EAM_ALERT_ACTION_LOG",
                                                 "EAM_ALERT_CONDITION_LOG",
                                                 "EAM_ALERT",
                                                 "EAM_EVENT_LOG",
                                                 "EAM_CPROP",
                                                 "EAM_MEASUREMENT",
                                                 "EAM_SRN",
                                                 "HQ_AVAIL_DATA_RLE" };

    public static final String LOG_CTX = "org.hyperic.hq.common.server.session.ServerConfigManagerImpl";
    protected final Log log = LogFactory.getLog(LOG_CTX);
    private final ConfigPropertyDAO configPropertyDAO;
    private final ServerConfigAuditFactory serverConfigAuditFactory;
    private final ServerConfigCache serverConfigCache;

    @Autowired
    public ServerConfigManagerImpl(DBUtil dbUtil, AuthzSubjectManager authzSubjectManager,
                                   ConfigPropertyDAO configPropertyDAO,
                                   ServerConfigAuditFactory serverConfigAuditFactory,
                                   ServerConfigCache serverConfigCache) {
        this.dbUtil = dbUtil;
        this.authzSubjectManager = authzSubjectManager;
        this.configPropertyDAO = configPropertyDAO;
        this.serverConfigAuditFactory = serverConfigAuditFactory;
        this.serverConfigCache = serverConfigCache;
    }

    private void createChangeAudit(AuthzSubject subject, String key, String oldVal, String newVal) {
        if (key.equals(HQConstants.BaseURL)) {
            serverConfigAuditFactory.updateBaseURL(subject, newVal, oldVal);
        } else if (key.equals(HQConstants.EmailSender)) {
            serverConfigAuditFactory.updateFromEmail(subject, newVal, oldVal);
        } else if (key.equals(HQConstants.DataMaintenance)) {
            int oldHours = (int) (Long.parseLong(oldVal) / 60 / 60 / 1000);
            int newHours = (int) (Long.parseLong(newVal) / 60 / 60 / 1000);
            serverConfigAuditFactory.updateDBMaint(subject, newHours, oldHours);
        } else if (key.equals(HQConstants.DataPurgeRaw)) {
            int oldDays = (int) (Long.parseLong(oldVal) / 24 / 60 / 60 / 1000);
            int newDays = (int) (Long.parseLong(newVal) / 24 / 60 / 60 / 1000);
            serverConfigAuditFactory.updateDeleteDetailed(subject, newDays, oldDays);
        } else if (key.equals(HQConstants.AlertPurge)) {
            int oldPurge = (int) (Long.parseLong(oldVal) / 24 / 60 / 60 / 1000);
            int newPurge = (int) (Long.parseLong(newVal) / 24 / 60 / 60 / 1000);
            serverConfigAuditFactory.updateAlertPurgeInterval(subject, newPurge, oldPurge);
        } else if (key.equals(HQConstants.EventLogPurge)) {
            int oldPurge = (int) (Long.parseLong(oldVal) / 24 / 60 / 60 / 1000);
            int newPurge = (int) (Long.parseLong(newVal) / 24 / 60 / 60 / 1000);
            serverConfigAuditFactory.updateEventPurgeInterval(subject, newPurge, oldPurge);
        } else if (key.equals(HQConstants.AlertsEnabled)) {
            boolean oldEnabled = oldVal.equals("true");
            boolean newEnabled = newVal.equals("true");
            serverConfigAuditFactory.updateAlertsEnabled(subject, newEnabled, oldEnabled);
        } else if (key.equals(HQConstants.AlertNotificationsEnabled)) {
            boolean oldEnabled = oldVal.equals("true");
            boolean newEnabled = newVal.equals("true");
            serverConfigAuditFactory.updateAlertNotificationsEnabled(subject, newEnabled,
                oldEnabled);
        } else if (key.equals(HQConstants.HIERARCHICAL_ALERTING_ENABLED)) {
            boolean oldEnabled = oldVal.equals("true");
            boolean newEnabled = newVal.equals("true");
            serverConfigAuditFactory.updateHierarchicalAlertingEnabled(subject, newEnabled,
                oldEnabled);
        }
    }

    private void createChangeAudits(AuthzSubject subject, Collection<ConfigProperty> allProps,
                                    Properties newProps) {
        Properties oldProps = new Properties();

        for (ConfigProperty prop : allProps) {

            String val = prop.getValue();

            if (val == null) {
                val = prop.getDefaultValue();
            }

            if (val == null) {
                val = "";
            }

            oldProps.put(prop.getKey(), val);
        }

        for (Map.Entry<Object, Object> newEnt : newProps.entrySet()) {
            String newKey = (String) newEnt.getKey();
            String newVal = (String) newEnt.getValue();
            String oldVal = (String) oldProps.get(newKey);

            if (oldVal == null || !oldVal.equals(newVal)) {
                if (oldVal == null) {
                    oldVal = "";
                }
                createChangeAudit(subject, newKey, oldVal, newVal);
            }
        }
    }

    /**
     * Set the server configuration
     * 
     * @throws ConfigPropertyException - if the props object is missing a key
     *         that's currently in the database
     * 
     */
    @Transactional
    public void setConfig(AuthzSubject subject, Properties newProps) throws ApplicationException,
        ConfigPropertyException {
        setConfig(subject, null, newProps);
    }

    @Transactional
    public void deleteConfig(AuthzSubject subject, Set<String> toDelete) {
        Collection<ConfigProperty> allProps = serverConfigCache.getProps(null);
        for (ConfigProperty configProp : allProps) {

            // check if the props object has a key matching
            String key = configProp.getKey();
            if (toDelete.contains(key)) {
                configPropertyDAO.remove(configProp);
                serverConfigCache.remove(key);
            }
        }
    }
    /**
     * Set the server Configuration
     * @param prefix The config prefix to use when setting properties. The
     *        prefix is used for namespace protection and property scoping.
     * @param newProps The Properties to set.
     * @throws ConfigPropertyException - if the props object is missing a key
     *         that's currently in the database
     * 
     */
    @Transactional
    public void setConfig(AuthzSubject subject, String prefix, Properties newProps)
        throws ApplicationException, ConfigPropertyException {

        Properties tempProps = new Properties();
        tempProps.putAll(newProps);

        // get all properties
        Collection<ConfigProperty> allProps = serverConfigCache.getProps(prefix);

        createChangeAudits(subject, allProps, newProps);
        for (ConfigProperty configProp : allProps) {

            // check if the props object has a key matching
            String key = configProp.getKey();
            if (newProps.containsKey(key)) {
                tempProps.remove(key);
                String propValue = (String) newProps.get(key);
                // delete null values from prefixed properties
                if (prefix != null && (propValue == null || propValue.equals("NULL"))) {
                    configPropertyDAO.remove(configProp);
                    serverConfigCache.remove(key);
                } else {
                    // non-prefixed properties never get deleted.
                    configProp.setValue(propValue);  
                    serverConfigCache.put(key, propValue);
                    // Fix Bug 1285064/HQ-4793: Save the property in DB 
                    configPropertyDAO.save(configProp);
                    if (HQConstants.HQGUID.equals(key)){
                        // Came from read only method so need to flush the session
                        configPropertyDAO.flushSession();
                        if (log.isDebugEnabled()) log.debug("Saving HQ-GUID in DB [" + propValue + "]");
                    }
                }
            } else if (prefix == null) {
                // Bomb out if props are missing for non-prefixed properties
                throw new ConfigPropertyException("Updated configuration missing required key: " +
                                                  key);
            }
        }

        // create properties that are still left in tempProps
        if (tempProps.size() > 0) {
            Enumeration propsToAdd = tempProps.propertyNames();
            while (propsToAdd.hasMoreElements()) {
                String key = (String) propsToAdd.nextElement();
                String propValue = tempProps.getProperty(key);
                // create the new property
                configPropertyDAO.create(prefix, key, propValue, propValue);
                serverConfigCache.put(key, propValue);
            }
        }
    }

    /**
     * Run an analyze command on all non metric tables. The metric tables are
     * handled seperately using analyzeHqMetricTables() so that only the tables
     * that have been modified are analyzed.
     * 
     * @return The time taken in milliseconds to run the command.
     * 
     */
    @Transactional
    public long analyzeNonMetricTables() {

        HQDialect dialect = (HQDialect) ((SessionFactoryImplementor) Bootstrap.getBean(SessionFactory.class))
            .getDialect();
        long duration = 0;

        Connection conn = null;
        try {
            conn = dbUtil.getConnection();
       
            for (Iterator i = Bootstrap.getBean(LocalSessionFactoryBean.class).getConfiguration().getTableMappings(); i.hasNext();) {
                Table t = (Table) i.next();

                if (t.getName().toUpperCase().startsWith("EAM_MEASUREMENT_DATA") ||
                    t.getName().toUpperCase().startsWith("HQ_METRIC_DATA")) {
                    continue;
                }

                String sql = dialect.getOptimizeStmt(t.getName(), 0);
                duration += doCommand(conn, sql, null);
            }
        } catch (SQLException e) {
            log.error("Error analyzing table", e);
        } finally {
            DBUtil.closeConnection(LOG_CTX, conn);
        }

        return duration;
    }

    /**
     * Run an analyze command on both the current measurement data slice and the
     * previous data slice if specified.
     * 
     * @param analyzePrevMetricDataTable tells method to analyze previous metric
     *        data table as well as the current.
     * @return The time taken in milliseconds to run the command.
     * 
     */
    @Transactional
    public long analyzeHqMetricTables(boolean analyzePrevMetricDataTable) {
        long systime = System.currentTimeMillis();
        String currMetricDataTable = MeasTabManagerUtil.getMeasTabname(systime);
        long prevtime = MeasTabManagerUtil.getPrevMeasTabTime(systime);
        String prevMetricDataTable = MeasTabManagerUtil.getMeasTabname(prevtime);

        long duration = 0;
        HQDialect dialect = (HQDialect) ((SessionFactoryImplementor) Bootstrap.getBean(SessionFactory.class))
            .getDialect();

        Connection conn = null;
        try {
            String sql;
            conn = dbUtil.getConnection();
            sql = dialect.getOptimizeStmt(currMetricDataTable, DEFAULT_COST);
            duration += doCommand(conn, sql, null);
            if (analyzePrevMetricDataTable) {
                sql = dialect.getOptimizeStmt(prevMetricDataTable, DEFAULT_COST);
                duration += doCommand(conn, sql, null);
            }
        } catch (SQLException e) {
            log.error("Error analyzing metric tables", e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeConnection(LOG_CTX, conn);
        }
        return duration;
    }

    private long doCommand(Connection conn, String sql, String table) throws SQLException {
        Statement stmt = null;
        StopWatch watch = new StopWatch();

        if (table == null) {
            table = "";
        }

        sql = StringUtil.replace(sql, "{0}", table);

        if (log.isDebugEnabled()) {
            log.debug("Execute command: " + sql);
        }

        try {
            stmt = conn.createStatement();
            stmt.execute(sql);
            return watch.getElapsed();
        } finally {
            DBUtil.closeStatement(LOG_CTX, stmt);
        }
    }

    /**
     * Generate new GUID and set it in properties and DB
     * This method is called only once
     * 
     * @param props server config properties
     * @return HQ-QUID
     * @author tgoldman
     */
    @Transactional
    private synchronized String generateNewGUID(Properties props){
        // Fix bug 1285064 / HQ-4793 
        String hqGUID;
        
        // Generate new GUID and set it to cache and DB
        if((hqGUID = GUIDGenerator.createGUID()) == null) {
            hqGUID = "unknown";
        }else {
            props.setProperty(HQConstants.HQGUID, hqGUID);
            try {
                setConfig(authzSubjectManager.getOverlordPojo(), props);
            }catch(Exception e) {
                throw new SystemException(e);
            }
        }
        log.info("Generating a new HQ-GUID [" + hqGUID + "]");
        
        return hqGUID;
    }

    /**
     * Save the HQ-QUID value in server configuration table
     * 
     * @param guidValue HQ-GUID
     */
    @Transactional
    private synchronized void saveGUID(String guidValue,  ConfigProperty configProp){
        // Fix Bug 1285064 / HQ-4793: Save the property in DB
        
        configProp.setValue(guidValue);
        configPropertyDAO.save(configProp);
        configPropertyDAO.flushSession();
        
        if (log.isDebugEnabled()) log.debug("Saving the GUID in DB [" + guidValue + "]");
    }
            
     
    /**
     * Get all the {@link ConfigProperty}s
     * 
     */
    @Transactional(readOnly = true)
    public Collection<ConfigProperty> getConfigProperties() {

        return configPropertyDAO.findAll();
    }

    /**
     * Gets the GUID for this HQ server instance. The GUID is persistent for the
     * duration of an HQ install and is created upon the first call of this
     * method. If for some reason it can't be determined, 'unknown' will be
     * returned.
     * 
     * 
     */
    @Transactional(readOnly = true)
    public String getGUID() {
        Properties p;
        String res;

        try {
            p = getConfig();
        }catch(Exception e) {
            throw new SystemException(e);
        }

        res = p.getProperty(HQConstants.HQGUID);
        if(res == null || res.trim().length() == 0) {
            // Fix bug 1285064 / HQ-4793: generate new GUID and save it to cache and DB
            res = generateNewGUID(p);
        }
        
        if (log.isDebugEnabled()) log.debug("HQ-GUID value is [" + res + "]");
        
        return res;
    }

    @Transactional(readOnly = true)
    public Properties getConfig() throws ConfigPropertyException {
        return serverConfigCache.getConfig();
    }

    @Transactional(readOnly = true)
    public Properties getConfig(String prefix) throws ConfigPropertyException {
        return serverConfigCache.getConfig(prefix);
    }
    
    @Transactional(readOnly = true)
    public String getPropertyValue(String name) {
        return serverConfigCache.getProperty(name);
    }        

    /**
     * 
     * @return major part of the server version - x.x or x.x.x. 
     * If pattern fails to match - returns the full server version.
     */
    @Transactional(readOnly = true)
    public String getServerMajorVersion() {
        String serverVersion = serverConfigCache.getProperty(HQConstants.ServerVersion);
        return extractMajorVersion(serverVersion);
    }    

    private String extractMajorVersion(String version) {
        String majorVersion = version;
        Pattern regex = Pattern.compile("^(\\d+)(.\\d+.\\d+)?");
        Matcher regexMatcher = regex.matcher(version);
        if (regexMatcher.find() && (regexMatcher.groupCount() > 0)) {
            majorVersion = regexMatcher.group(1); // just the first digits before .
                                                  // group(0) is the whole expression
        }
        return majorVersion;
    }

}
