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
import java.util.List;

import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.PluginException;

import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.servlet.client.JMXRemote;

import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Tomcat40ServiceControlPlugin 
    extends ControlPlugin
{
    private static Log log = LogFactory.getLog("Tomcat40ServiceControlPlugin");

    private static final String PROP_SERVICE_NAME = "serviceName";
    private static final String PROP_DOCBASE      = "docBase";

    private String jmxUrl;
    private JMXRemote jmxRemote;
    
    private static final String actions[] = { "start", "stop", "reload",
                                              "install", "remove" };
    private static List commands          = Arrays.asList(actions);

    private String serviceName;
    private String docBase;

    private int result;
    private String errorStr;

    public Tomcat40ServiceControlPlugin() {
        setName(ServletProductPlugin.WEBAPP_NAME);
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse productConfig) {

        SchemaBuilder schema = new SchemaBuilder(productConfig);
        
        schema.add(PROP_SERVICE_NAME, "Service name",
                   "/examples");

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
                                               "/examples");

            this.docBase = config.getValue(PROP_DOCBASE);

            this.jmxUrl= config.getValue(JMXRemote.PROP_JMX_URL);
            jmxRemote=new JMXRemote();
            jmxRemote.setJmxUrl(jmxUrl);
            jmxRemote.setUser(config.getValue(JMXRemote.PROP_JMX_USER));
            jmxRemote.setPassword(config.getValue(JMXRemote.PROP_JMX_PASS));
                        
            try {
                jmxRemote.init();
            } catch (Exception e) {
                throw new PluginException("Invalid jmxUrl " + jmxUrl, e);
            }
            
            this.log.info("Service control plugin for " + jmxUrl + " " + 
                           serviceName);
        } catch(Throwable ex) {
            this.log.error( "Error initializing ", ex);
        }
    }

    public void doAction(String action) {
        
        int res = RESULT_FAILURE;

        String appPath = jmxRemote.getJmxWebappPath();
        String url;
        
        if (action.equals("install")) {
            if (docBase == null || docBase.length() == 0) {
                setMessage("Context path to deploy not specified");
                setResult(RESULT_FAILURE);
                return;
            }
            url = appPath + "manager/install?path=" + serviceName + 
                "&war=" + docBase;
        } else {
            // Normal action
            url = appPath + "manager/" + action + "?path=" + serviceName;
        }

        this.log.debug(getName() + " doAction=" + action + " " + url);

        try {
            jmxRemote.openUrl(url);
            res = RESULT_SUCCESS;
        } catch (Exception e) {
            log.info("Error performing control", e);
            // XXX: More fine grained error reporting needed here
            setMessage(e.getMessage());
            setResult(res);
            return;
        }

        setResult(res);
    }
}
