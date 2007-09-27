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
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.servlet.client.JMXRemote;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.StringConfigOption;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JRunServiceControlPlugin 
    extends ControlPlugin
{
    private static Log log = LogFactory.getLog("JRunServiceControlPlugin");

    private static final String PROP_SERVICE_NAME = "serviceName";

    private String jmxUrl;
    private JMXRemote jmxRemote;
    
    private static final String actions[] = {"start", "stop", "reload"};
    private static List commands          = Arrays.asList(actions);

    private String serviceName;

    private int result;
    private String errorStr;

    public JRunServiceControlPlugin() {
        setName(ServletProductPlugin.WEBAPP_NAME);
    }

    public ConfigSchema getConfigSchema(TypeInfo info,
                                        ConfigResponse productConfig)
    {
        StringConfigOption serviceName = 
            new StringConfigOption(PROP_SERVICE_NAME, 
                                   "Service name ( examples )",
                                   "examples");

        ConfigSchema config = new ConfigSchema();
        config.addOption(serviceName);

        return config;
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
        return commands;
    }

    // Initialization
    public void configure(ConfigResponse config)
        throws PluginException
    {
        super.configure(config);

        try {
            // We don't need these defaults, but better safe than sorry.
            this.serviceName = config.getValue(PROP_SERVICE_NAME,
                                               "examples");
            
            this.jmxUrl= config.getValue(JMXRemote.PROP_JMX_URL);
            jmxRemote=new JMXRemote();
            jmxRemote.setJmxUrl(jmxUrl);
            // jmxRemote.setUser( user );
            // jmxRemote.setPassword( password );
                        
            try {
                jmxRemote.init();
            } catch (Exception e) {
                throw new PluginException("Invalid jmxUrl " + 
                                                 jmxUrl, e);
            }
        } catch(Throwable ex) {
            log.error("Error initializing ", ex);
        }
    }

    public void doAction(String action) {
        
        int res = -1;

        log.debug(getName() + " doAction="+action);
        
        if("restart".equals(action)) {
            // restart uses the same trick as start - touch web.xml 
            action="start";
        }
        
        if("stop".equals(action) || "start".equals(action)) {
            try {
                String objName = "ServletEngineService:service=" + serviceName;
                jmxRemote.invoke(objName, action);
                //Object obj = jmxRemote.getRemoteMBeanValue("");
                res = RESULT_SUCCESS;
            } catch (java.lang.reflect.InvocationTargetException e) {
                // This is ok.  happens when we try to stop a service that is
                // already stopped, or start a service that has already been
                // started
                // 
            } catch( MetricInvalidException idsnex ) {
                String remote = idsnex.getRemoteMessage();
                if(remote != null &&
                   remote.indexOf("InvocationTargetException") > 0) {
                    return; // An error occured.. WTF
                }
                log.error("Action error " + serviceName + " " + 
                          action + " " + remote + 
                          idsnex.toString());
                return;
            } catch(Exception ex) {
                log.error("Error in action " + ex);
            }
        }
        
        setResult(res);
    }
}
