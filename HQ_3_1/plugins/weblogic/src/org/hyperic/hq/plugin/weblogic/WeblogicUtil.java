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

package org.hyperic.hq.plugin.weblogic;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;

import org.hyperic.hq.plugin.weblogic.jmx.AttributeGetter;
import org.hyperic.hq.plugin.weblogic.jmx.ObjectNameCache;
import org.hyperic.hq.plugin.weblogic.jmx.WeblogicAttributes;

import java.util.Properties;
import java.util.HashMap;

import javax.naming.Context;
import javax.naming.NamingException;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import weblogic.jndi.Environment;
import weblogic.management.MBeanHome;
import weblogic.management.RemoteMBeanServer;

import weblogic.management.runtime.ServerLifeCycleRuntimeMBean;

public class WeblogicUtil {

    private static final boolean useAttrGetter = true;

    private static final String MBEAN_HOME = "MBeanHome";

    //these bean types can only be seen on the admin server.
    private static final String[] ADMIN_MBEAN_TYPES = {
        "Application", "WebAppComponent", "EJBComponent",
    };

    private static final HashMap ADMIN_MBEANS;

    static {
        HashMap beans = new HashMap();
        for (int i=0; i<ADMIN_MBEAN_TYPES.length; i++) {
            beans.put(ADMIN_MBEAN_TYPES[i], Boolean.TRUE);
        }
        ADMIN_MBEANS = beans;
    }

    static Object getRemoteMBeanValue(Metric metric) 
        throws MetricNotFoundException,
        MetricUnreachableException,
        PluginException {

        return getRemoteMBeanValue(metric, metric.getAttributeName());
    }

    //we only cache RemoteMBeanServer handles for measurements.
    //jndi lookup won't have much impact for control or discovery
    private static HashMap serverCache = new HashMap();

    static Object getRemoteMBeanValue(Metric metric, String attributeName) 
        throws MetricNotFoundException,
        MetricUnreachableException,
        PluginException {

        //weblogic 8.1 still uses an old jmxri implementation
        //which does not cache ObjectName parsing.
        ObjectName objName;

        try {
            objName =
                ObjectNameCache.getInstance(metric.getObjectName());
        } catch (MalformedObjectNameException e) {
            //will only happen if hq-plugin.xml has a bogus metric.
            String msg = "Malformed ObjectName '" + metric.getObjectName() + "'"; 
            throw new MetricInvalidException(msg, e);
        }

        Properties props = metric.getProperties();
        Properties objProps = metric.getObjectProperties();
        String home = props.getProperty(MBEAN_HOME);

        /*
         * certain MBeans can only been seen through the local
         * mbean server, others only through the admin.
         * the Metrics do not change, only the jndi name we lookup.
         * this name also needs to be used for the cache key.
         * so we transform the Metric property string once, the first
         * time a value is collected for each metric.
         */
        if (home == null) {
            String type = objProps.getProperty("Type");

            if (ADMIN_MBEANS.get(type) == Boolean.TRUE) {
                home = MBeanHome.ADMIN_JNDI_NAME;
            }
            else {
                home = MBeanHome.LOCAL_JNDI_NAME;
            }

            props.setProperty(MBEAN_HOME, home);
            metric.setPropString(metric.getPropString() +
                              "," + MBEAN_HOME + "=" + home);
        }

        RemoteMBeanServer mServer = null;
        boolean cached = true, retry = false;
        String cacheKey = metric.getPropString();

        synchronized (serverCache) {
            mServer = (RemoteMBeanServer)serverCache.get(cacheKey);
        }

        if (mServer == null) {
            cached = false;
            mServer = getMBeanServer(metric); //jndi lookup

            synchronized (serverCache) {
                serverCache.put(cacheKey, mServer);
            }
        }

        Object value;

        synchronized (mServer) {
            try {
                if (useAttrGetter) {
                    AttributeGetter getter = 
                        AttributeGetter.getInstance(WeblogicAttributes.instance,
                                                    objName);
                    if (getter != null) {
                        return getter.getAttribute(mServer,
                                                   attributeName);
                    }
                }

                value = mServer.getAttribute(objName, attributeName);
            } catch (MBeanException e) {
                String msg = "MBeanException: " + e.getMessage();
                throw new PluginException(msg, e);
            } catch (AttributeNotFoundException e) {
                String msg = "Attribute '" + attributeName + "' " +
                    "not found for '" + objName + "'";
                throw new MetricNotFoundException(msg, e);
            } catch (InstanceNotFoundException e) {
                String msg = "MBean '" + objName + "' not found";
                throw new MetricNotFoundException(msg, e);
            } catch (ReflectionException e) {
                String msg = "ReflectionException: " + e.getMessage();
                throw new PluginException(msg, e);
            } catch (Exception e) {
                //likely weblogic.rmi.extensions.RemoteRuntimeException
                //but compiler won't let us catch that.
                if (cached) {
                    serverCache.remove(cacheKey);
                    WeblogicAuth.clearCache(); //domain credential may have changed.
                    value = null;
                    retry = true;
                    //will try again; server may have been restarted
                }
                else {
                    String msg = "Unknown failure: " + e.getMessage();
                    throw new PluginException(msg, e);
                }
            }
        }

        if (retry) {
            //note we only recurse once.  if we had a cached mServer
            //but got a RemoteRuntimeException
            return getRemoteMBeanValue(metric);
        }

        return value;
    }

    //XXX implement caching MBeanHome
    public static RemoteMBeanServer getMBeanServer(Properties props)
        throws MetricNotFoundException,
        MetricUnreachableException,
        PluginException {

        Environment env = new Environment();

        String adminUrl = props.getProperty(WeblogicMetric.PROP_ADMIN_URL);
        String url = props.getProperty(WeblogicMetric.PROP_SERVER_URL,
                                       adminUrl);

        env.setProviderUrl(url);

        env.setSecurityPrincipal(props.getProperty(
            WeblogicMetric.PROP_ADMIN_USERNAME
        ));

        env.setSecurityCredentials(props.getProperty(
            WeblogicMetric.PROP_ADMIN_PASSWORD, ""
        ));

        Context ctx;

        try {
            ctx = env.getInitialContext();
        } catch (NamingException e) {
            String msg = "Failed to connect to MBeanServer: " + url;
            String cause = e.getMessage();
            String reason;

            if (cause != null) {
                reason = cause;
            }
            else {
                reason = "invalid URL or credentials";
            }
            msg += " (" + reason + ")";

            throw new MetricUnreachableException(msg, e);
        }

        MBeanHome home;
        String mbeanHome =
            props.getProperty(MBEAN_HOME,
                              MBeanHome.LOCAL_JNDI_NAME);

        try {
            home = (MBeanHome)ctx.lookup(mbeanHome);
        } catch (NamingException e) {
            String msg = "Failed to contact MBeanServer: " +
                e.getMessage();
            throw new MetricUnreachableException(msg, e);
        } finally {
            try {
                ctx.close();
            } catch (NamingException e) {
                String msg = "Failed to close MBeanServer context: " +
                    e.getMessage();
                throw new PluginException(msg, e);
            }
        }

        try {
            return home.getMBeanServer();
        } catch (SecurityException e) {
            //this exception is not declared to be thrown, but may happen
            //when an agent is talking to two domains
            final String doc =
                "http://e-docs.bea.com/wls/docs70/secmanage/domain.html#1171534";
            String msg =
                "SecurityException getting MBeanServer (jaas=" + 
                WeblogicProductPlugin.useJAAS() + "): " +
                e.getMessage() + "\n" +
                "Likely cause: domain credential mismatch\n" +
                "See: " + doc;

            throw new PluginException(msg);
        }
    }

    static RemoteMBeanServer getMBeanServer(Metric metric)
        throws MetricNotFoundException,
        MetricUnreachableException,
        PluginException {

        return getMBeanServer(metric.getProperties());
    }

    //e.g. can be used by control
    static RemoteMBeanServer getMBeanServer(ConfigResponse config)
        throws MetricNotFoundException,
        MetricUnreachableException,
        PluginException {

        return getMBeanServer(config.toProperties());
    }

    static Object invoke(Metric metric, String method)
        throws MetricNotFoundException,
        MetricUnreachableException,
        PluginException {

        return invoke(metric, method, new Object[0], new String[0]);
    }

    static Object invoke(Metric metric, String method, 
                         Object[] args, String[] sig)
        throws MetricNotFoundException,
        MetricUnreachableException,
        PluginException {

        ObjectName obj;
        RemoteMBeanServer mServer;

        try {
            mServer = getMBeanServer(metric);
        } catch (PluginException e) {
            throw new PluginException(e.getMessage(), e);
        }

        try {
            obj = ObjectNameCache.getInstance(metric.getObjectName());
        } catch (MalformedObjectNameException e) {
            throw new MetricInvalidException(e.getMessage());
        }

        try {
            return mServer.invoke(obj, method, args, sig);
        } catch (InstanceNotFoundException e) {
            throw new MetricInvalidException(e.getMessage());
        } catch (MBeanException e) {
            throw new PluginException(e.getMessage(), e);
        } catch (ReflectionException e) {
            throw new PluginException(e.getMessage(), e);
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    static double convertStateVal(Object state) {
        if (state instanceof Integer) { //server
            return convertStateVal((Integer)state);
        }
        else if (state instanceof Boolean) { //application
            boolean deployed = ((Boolean)state).booleanValue();
            return deployed ?
                Metric.AVAIL_UP :
                Metric.AVAIL_DOWN;
        }
        else if (state instanceof String) {
            if ("DEPLOYED".equals(state)) { //webapp
                return Metric.AVAIL_UP;
            }
            else if ("running".equalsIgnoreCase((String)state)) { //server
                //6.1  == "Running"
                //7.0+ == "RUNNING"
                return Metric.AVAIL_UP;
            }
            else {
                //exq. XXX need a better check
                //but being able to get any attribute with this
                //ObjectName means the exq is up.
                return Metric.AVAIL_UP;
            }
        }

        return Metric.AVAIL_UNKNOWN;
    }

    static double convertStateVal(Integer state) {
        switch (state.intValue()) {
          case ServerLifeCycleRuntimeMBean.SRVR_RUNNING:
            return Metric.AVAIL_UP;

          case ServerLifeCycleRuntimeMBean.SRVR_SHUTDOWN:
          case ServerLifeCycleRuntimeMBean.SRVR_STARTING:
          case ServerLifeCycleRuntimeMBean.SRVR_STANDBY:
          case ServerLifeCycleRuntimeMBean.SRVR_SUSPENDING:
          case ServerLifeCycleRuntimeMBean.SRVR_RESUMING: 
          case ServerLifeCycleRuntimeMBean.SRVR_SHUTTING_DOWN:
          case ServerLifeCycleRuntimeMBean.SRVR_FAILED:
            return Metric.AVAIL_DOWN;

          case ServerLifeCycleRuntimeMBean.SRVR_UNKNOWN:

          default:
            return Metric.AVAIL_UNKNOWN;
        }
    }
}
