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

package org.hyperic.hq.plugin.servlet;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;

import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.servlet.client.JMXRemote;

import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Tomcat41ServiceControlPlugin 
    extends ControlPlugin
{
    private static Log log = LogFactory.getLog("Tomcat41ServiceControlPlugin");

    private static final String PROP_SERVICE_NAME  = "serviceName";
    private static final String PROP_DOCBASE       = "docBase";

    private String jmxUrl;
    private JMXRemote jmxRemote;
    
    private static final String actions[] = {"start", "stop", "reload"};
    private static List commands          = Arrays.asList(actions);

    private String installPrefix;
    private String serviceName;
    private String docBase;

    private int result;
    private String errorStr;

    public Tomcat41ServiceControlPlugin() {
        setName(ServletProductPlugin.WEBAPP_NAME);
    }

    public ConfigSchema getConfigSchema(TypeInfo info, 
                                        ConfigResponse productConfig)
    {
        SchemaBuilder schema = new SchemaBuilder(productConfig);
        
        schema.add(PROP_SERVICE_NAME, "Service name (//HOST/CONTEXT_PATH)",
                   "//localhost/examples");

        // XXX: These may become required
        ConfigOption opt = schema.add(PROP_DOCBASE, 
                                      "WAR or Directory URL", "");
        opt.setOptional(true);

        return schema.getSchema();
    }

    public int getResult()
    {    
        return this.result;
    }

    public void setResult(int result)
    {
        this.result = result;
    }
    public String getErrorStr()
    {
        return this.errorStr;
    }   

    public void setErrorStr(String errorStr)
    {
        this.errorStr = errorStr;
    }   

    // ControlPlugin specific methods

    public List getActions() {
        return this.commands;
    }

    // Initialization
    public void configure(ConfigResponse config)
        throws PluginException
    {
        super.configure(config);

        try {
            this.log.debug("Config: " + config.toProperties());

            // We don't need these defaults, but better safe than sorry.
            this.serviceName = config.getValue(PROP_SERVICE_NAME,
                                               "//localhost/examples");
            this.installPrefix = 
                config.getValue(ProductPlugin.PROP_INSTALLPATH);

            this.docBase = config.getValue(PROP_DOCBASE);

            this.jmxUrl= config.getValue(JMXRemote.PROP_JMX_URL);
            jmxRemote = new JMXRemote();
            jmxRemote.setJmxUrl(jmxUrl);
            // jmxRemote.setUser( user );
            // jmxRemote.setPassword( password );
                        
            try {
                jmxRemote.init();
            } catch (Exception e) {
                throw new PluginException("Invalid jmxUrl " + jmxUrl, e);
            }
        } catch(Throwable ex) {
            this.log.error( "Error initializing ", ex);
        }
    }
    
    private String getObjectName(String serviceName) 
        throws PluginException
    {
        if (ServletProductPlugin.isJBossEmbeddedVersion(getTypeInfo(),
                                                        this.installPrefix)) {
            String contextName = 
                serviceName.substring(serviceName.lastIndexOf("/") + 1) +
                ".war";

            if (getTypeInfo().getVersion().equals(ServletProductPlugin.
                                                  TOMCAT_VERSION_41)) {
                // Probably JBoss 3.2.3, cannot get this to work due to NPE
                // return "jboss.management.local:J2EEApplication=null," +
                // J2EEServer=Local,j2eeType=WebModule,name=" + contextName;
                throw new PluginException("Webapp control for embedded " +
                                          "Tomcat 4.1 is not supported.");
            }

            //This seems to work fine on JBoss 3.2.7 and 4.0.2
            Manifest mBeanInfo;
            try { 
                mBeanInfo = jmxRemote.getRemoteInfo();
            } catch (Exception e) {
                throw new PluginException("Unable to get remote MBeans: " +
                                          e.getMessage());
            }
            Set objectNames = mBeanInfo.getEntries().keySet();
            for (Iterator i = objectNames.iterator(); i.hasNext();) {
                String objectName = (String)i.next();
                if (objectName.startsWith("jboss.web.deployment:") &&
                    (objectName.indexOf("war=" + contextName) != -1)) {
                    return objectName;
                }
            }
            
            throw new PluginException("Unable to determine ObjectName");

        } else {
            // Regular Tomcat
            return "Catalina:j2eeType=WebModule,name=" + serviceName +
                ",J2EEApplication=none,J2EEServer=none";
        }
    }

    public void doAction(String action) {
        String err;
        int res = -1;

        log.debug(getName() + " doAction="+action);

        try {
            String objectName = getObjectName(serviceName);
            
            jmxRemote.invoke(objectName, action);

            res = RESULT_SUCCESS;
        } catch (java.lang.reflect.InvocationTargetException e) {
            // This is ok.  happens when we try to stop a service that is
            // already stopped, or start a service that has already been
            // started
            // 
        } catch( MetricInvalidException idsnex ) {
            String remote=idsnex.getRemoteMessage();
            if(remote != null &&
                ((remote.indexOf("InvocationTargetException") > 0) ||
                 (remote.indexOf("LifecycleException") > 0))) {
                log.warn("Lifecycle exception: service " + serviceName +
                         " already started/stopped");
                setResult(RESULT_SUCCESS);
                return; // XXX: probably not needed, we check this above
            }
            err = "Error invoking method " + action + ": " + remote;
            setMessage(err);
            log.error(err);
            return;
        } catch( Exception ex ) {
            err = "Error in action: " + ex;
            setMessage(err);
        }

        setResult(res);
    }
}
