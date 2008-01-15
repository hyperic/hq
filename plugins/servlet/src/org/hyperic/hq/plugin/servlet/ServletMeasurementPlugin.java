/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

import java.util.HashMap;
import java.util.Map;
import java.io.File;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.servlet.client.JMXRemote;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.StringConfigOption;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ServletMeasurementPlugin 
    extends MeasurementPlugin
{
    private static Log log = LogFactory.getLog("ServletMeasurementPlugin");

    // avoid creating the object each time
    private static final Double DOUBLE_ZERO = new Double(0);

    // Used for replacement in hq-plugin.xml.  Windows requires pathnames
    // to be slightly different for xcopy.
    private static String[][] PLATFORM_HELP_PROPS = {
        {
            "webapp.dir", "", "\\hyperic-hq"
        }
    };
    
    public ServletMeasurementPlugin() {
        setName(ServletProductPlugin.NAME);
    }

    public ServletMeasurementPlugin(String name) {
        setName(name);
    }

    protected String[][] getPlatformHelpProperties() {
        return PLATFORM_HELP_PROPS;
    }

    public String getHelp(TypeInfo info, Map props) {
        String installpath =
            (String)props.get(ServletProductPlugin.PROP_INSTALLPATH);

        if ((installpath != null) && //dump-plugin-info will be null
            (installpath.indexOf("jbossweb") != -1 ||
             installpath.indexOf("jboss-web") != -1))
        {
            //<help name="Tomcat 5.0 JBoss embedded" ...>
            String helpName = info.getName() + " JBoss embedded";

            //File jbossDir = new File(installpath).getParentFile().getParentFile();
            File jbossDir = new File(installpath);
            File parent = jbossDir.getParentFile();
            if (parent != null) {
                jbossDir = parent;
                parent = jbossDir.getParentFile();
                if (parent != null) {
                    jbossDir = parent;
                }
            }

            File logDir = new File(jbossDir, "log");
            Map helpProps = new HashMap();
            helpProps.putAll(props);
            helpProps.put("logdir", logDir.getAbsolutePath());

            return getPluginXMLHelp(info, helpName, helpProps);
        } else {
            //Regular tomcat
            File logDir = new File(installpath, "logs");
            Map helpProps = new HashMap();
            helpProps.putAll(props);
            helpProps.put("logdir", logDir.getAbsolutePath());

            return super.getHelp(info, helpProps);
        }
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config)
    {
        int type = info.getType();
        String name = info.getName();
        ConfigSchema schema=new ConfigSchema();
        
        log.debug("GetConfigSchema " + type + " " + name + " " + config);
        
        if(type == TypeInfo.TYPE_SERVICE) {
            StringConfigOption host = 
                new StringConfigOption(JMXRemote.PROP_HOST,
                                       "Name of virtual host", 
                                       "localhost");
            /**
             * Since the servlet api doesn't provide the context path on init,
             * we use getRealPath() and extract the last component - so it's 
             * "cam" or "ROOT", even if the app is mapped to the root context.
             */
            StringConfigOption context = 
                new StringConfigOption(JMXRemote.PROP_CONTEXT,
                                       "Name of the servlet context. This " +
                                       "is the same with the name of the " +
                                       "directory where the application " +
                                       "is deployed.", "/examples");
            
            if (name.endsWith(ServletProductPlugin.WEBAPP_NAME) ||
                name.equals( "Web Application") ) {
                schema.addOption(host);
                schema.addOption(context);
            }
        }

        return schema;
    }

    /** 
     * Get the value from the monitored product. You can override either
     * this method or getValue(). Metric will parse the Metric and 
     * provde name/value access to attributes. 
     * 
     * The result object will be converted to Double, and the current time
     * will be set. 
     * 
     * @return the object - either as Double or a convertible type. 
     * @throws MetricInvalidException if it can't be retrieved ( for any reasons ). 
     * @throws MetricUnreachableExcpetion if the server cannot be contacted
     */               
    public Object getRemoteValue(Metric metric) 
        throws MetricInvalidException, MetricUnreachableException
    {
        JMXRemote remote = getJMXRemote(metric);
        return remote.getRemoteMBeanValue(metric);
    }

    /** 
     * Similar to getRemoteValue, but will return ZERO on error
     */ 
    public Object getAvailability(Metric metric) 
        throws MetricInvalidException 
    {
        JMXRemote remote = getJMXRemote(metric);
        return remote.getAvailability(metric);
    }
    
    private JMXRemote getJMXRemote(Metric metric) 
        throws MetricInvalidException
    {
        try {
            return JMXRemote.getInstance(metric.getProperties());
        } catch(PluginException ex) {
            String msg =
                "Can't initialize remote connector: " + ex.getMessage();
            throw new MetricInvalidException(msg, ex);
        }
    }

    /**
     * Base implementation for getValue. Will convert the Metric to a Metric
     * and delegate to getRemoteValue.
     * 
     * You can either override this method or getRemoteValue.
     */     
    public MetricValue getValue(Metric metric)
        throws PluginException,
               MetricInvalidException, MetricNotFoundException, 
               MetricUnreachableException
    {
        MetricValue mValue = null;
        
        try {
            Object obj = getRemoteValue(metric);
            Double val = DOUBLE_ZERO;
            
            if(obj instanceof Double) { 
                val = (Double)obj;
            } else if(obj != null) {
                val = new Double(obj.toString());
            }
            if ((int)val.doubleValue() == -1) {
                //e.g. process shared mem on win32
                mValue = new MetricValue(Double.NaN);
            }
            else {
                mValue = new MetricValue(val, System.currentTimeMillis());
            }
        } catch(MetricInvalidException e) {
            throw e;
        } catch (MetricUnreachableException e) {
            throw e;
        } catch (Exception e) {
            throw new MetricInvalidException(e.getMessage(), e);
        }

        return mValue;
    }
}
