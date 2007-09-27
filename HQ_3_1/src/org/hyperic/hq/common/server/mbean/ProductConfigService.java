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

package org.hyperic.hq.common.server.mbean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.security.auth.login.AppConfigurationEntry;

import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManagerUtil;
import org.hyperic.hq.product.server.MBeanUtil;
import org.hyperic.util.ConfigPropertyException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ProductConfigService - used to do any special setup tasks that
 * relate to the data stored in the configuration tables.
 *
 * @jmx:mbean name="hyperic.jmx:type=Service,name=ProductConfig"
 */
public class ProductConfigService 
    implements ProductConfigServiceMBean {

    private static final String AUTH_REALM =
        HQConstants.ApplicationName;
    private static final String AUTH_METHOD = "addAppConfig";
    private static final String AUTH_OBJECTNAME = 
        "jboss.security:service=XMLLoginConfig";
    
    protected Log _log = LogFactory.getLog(ProductConfigService.class);

    public ProductConfigService() {}

    /**
     * @jmx:managed-operation
     */
    public void stop()
    {
        _log.info("Stopping ProductConfigService");
    }

    /**
     * @jmx:managed-operation
     */
    public void start() {
        _log.info("Starting " + this.getClass().getName());
        _log.info("Initializing Hyperic Auth Providers");

        try {
            Properties conf = getConfig();
            _log.info("Enabling Hyperic JAAS Providers");
            registerJAASModules(conf);
        } catch (Exception e) {
            // Shouldn't happen
            _log.fatal("Error initializing " + this.getClass().getName(), e);
        }
    }

    /**
     * @jmx:managed-operation
     */
    public void restart() {
        _log.info("Restarting " + this.getClass().getName());
        stop();
        start();
    }

    /**
     * @jmx:managed-operation
     */
    public void init() {}

    /**
     * @jmx:managed-operation
     */
    public void destroy() {}

    private void registerJAASModules(Properties conf) 
        throws SystemException 
    {
        MBeanServer server = MBeanUtil.getMBeanServer();
        ArrayList configEntries = new ArrayList();
        AppConfigurationEntry ace;
        Map configOptions;

        try {
            configOptions = getJdbcOptions(conf);
            ace = new AppConfigurationEntry(
                HQConstants.JDBCJAASProviderClass,
                AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT,
                configOptions);
            // We always add the JDBC provider to the auth config
            _log.info("Enabling Hyperic JDBC JAAS Provider");
            configEntries.add(ace);

            AppConfigurationEntry[] config = (AppConfigurationEntry[])
                configEntries.toArray(new AppConfigurationEntry[0]);

            ObjectName objName = new ObjectName(AUTH_OBJECTNAME);
            server.invoke(objName, AUTH_METHOD,
                          new Object[] { AUTH_REALM, config },
                          new String[] { "java.lang.String",
                                         config.getClass().getName() });
        } catch (Exception e) {
            throw new SystemException("Error Registering Hyperic JAAS " +
                                         "Modules", e);
        }
    }

    private Properties getConfig() throws ConfigPropertyException 
    {
        try {
            return ServerConfigManagerUtil.getLocalHome().create().getConfig();
        } catch (javax.naming.NamingException e) {
            throw new SystemException("Naming error in ProductConfigService", e);
        } catch (javax.ejb.CreateException e) {
            throw new SystemException("Error in ProductConfigService", e);
        }
    }

    private Map getJdbcOptions(Properties conf)
        throws ApplicationException, SystemException
    {
        Map configOptions = new HashMap();

        // We always store passwords encoded.  Don't allow the end user
        // to change this behavior.
        configOptions.put("hashAlgorithm", "MD5");
        configOptions.put("hashEncoding", "base64");

        return configOptions;
    }
}
