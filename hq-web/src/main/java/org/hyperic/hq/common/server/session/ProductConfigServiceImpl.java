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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.security.auth.login.AppConfigurationEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.util.ConfigPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ProductConfigService - used to do any special setup tasks that
 * relate to the data stored in the configuration tables.
 *
 * 
 */

@Service("productConfigService")
public class ProductConfigServiceImpl implements ProductConfigService {

    private static final String AUTH_REALM =
        HQConstants.ApplicationName;
    private static final String AUTH_METHOD = "addAppConfig";
    private static final String AUTH_OBJECTNAME = 
        "jboss.security:service=XMLLoginConfig";
    
    protected Log log = LogFactory.getLog(ProductConfigServiceImpl.class);
    
    private MBeanServer mbeanServer;
    
    private ServerConfigManager serverConfigManager;

    @Autowired
    public ProductConfigServiceImpl(MBeanServer mbeanServer, ServerConfigManager serverConfigManager) {
        this.mbeanServer = mbeanServer;
        this.serverConfigManager = serverConfigManager;
    }

   

    /**
     * 
     */
    public void start() {
        log.info("Starting " + this.getClass().getName());
        log.info("Initializing Hyperic Auth Providers");

        try {
            Properties conf = getConfig();
            log.info("Enabling Hyperic JAAS Providers");
            registerJAASModules(conf);
        } catch (Exception e) {
            // Shouldn't happen
            log.fatal("Error initializing " + this.getClass().getName(), e);
        }
    }

    /**
     * 
     */

    public void restart() {
        log.info("Restarting " + this.getClass().getName());
        start();
    }

   

    private void registerJAASModules(Properties conf) 
        throws SystemException 
    {
        
        ArrayList<AppConfigurationEntry> configEntries = new ArrayList<AppConfigurationEntry>();
       
       
        try {
            Map<String, String>  configOptions = getJdbcOptions(conf);
            AppConfigurationEntry ace = new AppConfigurationEntry(
                HQConstants.JDBCJAASProviderClass,
                AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT,
                configOptions);
            // We always add the JDBC provider to the auth config
            log.info("Enabling Hyperic JDBC JAAS Provider");
            configEntries.add(ace);

            AppConfigurationEntry[] config = 
                configEntries.toArray(new AppConfigurationEntry[0]);

            ObjectName objName = new ObjectName(AUTH_OBJECTNAME);
            mbeanServer.invoke(objName, AUTH_METHOD,
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
            return serverConfigManager.getConfig();
        } catch (Exception e) {
            throw new SystemException("Error in ProductConfigService", e);
        }
    }

    private Map<String,String> getJdbcOptions(Properties conf)
        throws ApplicationException, SystemException
    {
        Map<String, String> configOptions = new HashMap<String, String>();

        // We always store passwords encoded.  Don't allow the end user
        // to change this behavior.
        configOptions.put("hashAlgorithm", "MD5");
        configOptions.put("hashEncoding", "base64");

        return configOptions;
    }
}
