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
package org.hyperic.hq.plugin.websphere;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.AdminClientFactory;
import com.ibm.websphere.management.exception.ConnectorException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;

/**
 * WebSphere JMX Utils for version 5.0 + These utils simply wrap
 * javax.management.* and WebSphere interfaces.
 */
public class WebsphereUtil {

    private static HashMap cache = new HashMap();
    private static Log log = LogFactory.getLog("WebSphereUtil");
    // WAS doesn't expose these anywhere we've found yet
    private static final String TRUST_STORE = "javax.net.ssl.trustStore";
    private static final String KEY_STORE = "javax.net.ssl.keyStore";
    private static final String TRUST_STORE_PWD = "javax.net.ssl.trustStorePassword";
    private static final String KEY_STORE_PWD = "javax.net.ssl.keyStorePassword";
    private static final HashMap beans = new HashMap();

    public static Object getRemoteMBeanValue(Metric metric)
            throws MetricNotFoundException,
            MetricUnreachableException,
            PluginException {

        return getRemoteMBeanValue(metric, metric.getAttributeName());
    }

    public static Object getRemoteMBeanValue(Metric metric, String attributeName)
            throws MetricNotFoundException,
            MetricUnreachableException,
            PluginException {

        AdminClient mServer = getMBeanServer(metric);

        ObjectName objName;

        try {
            objName = new ObjectName(metric.getObjectName());
        } catch (MalformedObjectNameException e) {
            throw new MetricInvalidException(e.getMessage(), e);
        }

        try {
            return mServer.getAttribute(objName,
                    metric.getAttributeName());
        } catch (MBeanException e) {
            String msg = "MBeanException: " + e.getMessage();
            throw new PluginException(msg, e);
        } catch (AttributeNotFoundException e) {
            String msg = "Attribute '" + attributeName + "' "
                    + "not found for '" + objName + "'";
            throw new MetricNotFoundException(msg, e);
        } catch (InstanceNotFoundException e) {
            String msg = "MBean '" + objName + "' not found";
            throw new MetricNotFoundException(msg, e);
        } catch (ReflectionException e) {
            String msg = "ReflectionException: " + e.getMessage();
            throw new PluginException(msg, e);
        } catch (ConnectorException e) {
            String msg = "ConnectorException: " + e.getMessage();
            throw new PluginException(msg, e);
        }
    }

    //XXX cache
    public static AdminClient getMBeanServer(Properties cfg)
            throws MetricUnreachableException {

        Properties props = getAdminProperties(cfg);

        if (log.isDebugEnabled()) {
            log.debug("Attempting to create admin client with props "
                    + props + " from config " + cfg);
        }

        AdminClient mServer;
        WebsphereStopWatch timer = new WebsphereStopWatch();

        // http://blogs.sun.com/sunabl/entry/websphere_application_server_and_jvm ????
        // XXX test it with solaris version.
        try {
            mServer = AdminClientFactory.createAdminClient(props);
        } catch (LinkageError e) {
            log.error("Incorrect JVM ??? !!!", e);
            throw new MetricUnreachableException(e.getMessage(), e);
        } catch (ConnectorException ex) {
            Throwable e = ex;
            while ((e = (Throwable) e.getCause()) != null) {
                if (e instanceof LinkageError) {
                    throw new MetricUnreachableException("!!! Incorrect JVM !!!", e);
                }
            }
            throw new MetricUnreachableException(ex.getMessage(), ex);
        }

        if (log.isDebugEnabled() && timer.isTooLong()) {
            log.debug("createAdminClient took: "
                    + timer.getElapsedSeconds() + " seconds");
        }

        return mServer;
    }

    public static Properties getAdminProperties(Properties cfg) {
        log.debug("[getAdminProperties] cfg=" + cfg);
        String host =
                cfg.getProperty(WebsphereProductPlugin.PROP_ADMIN_HOST,
                "localhost");
        String port =
                cfg.getProperty(WebsphereProductPlugin.PROP_ADMIN_PORT,
                "8880");

        Properties props = new Properties();

        props.setProperty(AdminClient.CONNECTOR_TYPE,
                AdminClient.CONNECTOR_TYPE_SOAP);

        props.setProperty(AdminClient.CONNECTOR_HOST, host);

        props.setProperty(AdminClient.CONNECTOR_PORT, port);

        String user = cfg.getProperty(AdminClient.USERNAME, "");
        String pass = cfg.getProperty(AdminClient.PASSWORD, "");

        // user and pass cannot be null because getProperty() is called
        // with default values
        if (!user.equals("") && !pass.equals("")) {
            props.setProperty(AdminClient.USERNAME, user);
            props.setProperty(AdminClient.PASSWORD, pass);
            props.setProperty(AdminClient.CONNECTOR_SECURITY_ENABLED, "true");

            // Set the ssl props, if they're available.  As of this writing, there are no
            // publicly-available constants for the prop keys, so they're kept in this source file.
            String trustStore = cfg.getProperty(TRUST_STORE);
            String keyStore = cfg.getProperty(KEY_STORE);
            String trustStorePwd = cfg.getProperty(TRUST_STORE_PWD);
            String keyStorePwd = cfg.getProperty(KEY_STORE_PWD);
            if (trustStore != null && keyStore != null
                    && trustStorePwd != null && keyStorePwd != null) {
                props.setProperty(TRUST_STORE, trustStore);
                props.setProperty(KEY_STORE, keyStore);
                props.setProperty(TRUST_STORE_PWD, trustStorePwd);
                props.setProperty(KEY_STORE_PWD, keyStorePwd);
            }
        }

        log.debug("[getAdminProperties] props=" + props);
        return props;
    }

    public static AdminClient getMBeanServer(Metric metric)
            throws MetricUnreachableException {

        String key = metric.getPropString();
        AdminClient mServer =
                (AdminClient) cache.get(key);

        if (mServer != null) {
            try {
                mServer.getDomainName();
            } catch (ConnectorException e) {
                mServer = null;
                log.debug("getMBeanServer stale connection: " + key);
            }
        }

        if (mServer == null) {
            mServer = getMBeanServer(metric.getProperties());
            cache.put(key, mServer);
            log.debug("getMBeanServer caching: " + key);
        }

        return mServer;
    }

    //only use this for 1 metric at the moment, to
    //verify the admin server has been given proper
    //configuration.
    public static double getMBeanCount(AdminClient mServer,
            ObjectName query,
            String attr)
            throws MetricUnreachableException,
            MetricNotFoundException {

        double count = 0;

        try {
            Set beansSet = mServer.queryNames(query, null);
            for (Iterator it = beansSet.iterator(); it.hasNext();) {
                ObjectName name = (ObjectName) it.next();
                try {
                    if (attr != null) {
                        mServer.getAttribute(name, attr);
                    }
                    count++;
                } catch (AttributeNotFoundException e) {
                    throw new MetricNotFoundException(name.toString());
                } catch (InstanceNotFoundException e) {
                    throw new MetricNotFoundException(name.toString());
                } catch (Exception e) {
                    //e.g. unauthorized
                    throw new MetricUnreachableException(name
                            + ": " + e.getMessage(), e);
                }
            }
        } catch (ConnectorException e) {
            throw new MetricUnreachableException(query.toString(), e);
        }

        if (count == 0) {
            throw new MetricNotFoundException(query
                    + " (Invalid node name?)");
        }

        return count;
    }

    public static ObjectName resolve(AdminClient mServer, ObjectName name)
            throws PluginException {

        if (!name.isPattern()) {
            return name;
        }
        try {
            Set beansSet = mServer.queryNames(name, null);
            if (beansSet.size() != 1) {
                String msg =
                        name + " query returned "
                        + beansSet.size() + " results";
                throw new PluginException(msg);
            }

            ObjectName fullName =
                    (ObjectName) beansSet.iterator().next();

            if (log.isDebugEnabled()) {
                log.debug(name + " resolved to: " + fullName);
            }

            return fullName;
        } catch (Exception e) {
            String msg =
                    "resolve(" + name + "): " + e.getMessage();
            throw new PluginException(msg, e);
        }
    }

    public static boolean isRunning(Metric metric) {
        //for the moment avail == 1 MBean is registered.
        AdminClient mServer;
        String domain;

        try {
            mServer = getMBeanServer(metric);
            domain = mServer.getDomainName();
        } catch (MetricUnreachableException e) {
            log.debug(metric + ": " + e, e);
            return false;
        } catch (ConnectorException e) {
            log.debug(metric + ": " + e, e);
            return false;
        }

        String query =
                domain + ":"
                + metric.getObjectPropString() + ",*";

        ObjectName name = (ObjectName) beans.get(query);
        if (name == null) {
            try {
                name = new ObjectName(query);
            } catch (MalformedObjectNameException e) {
                throw new MetricInvalidException(metric.getObjectName());
            }
            try {
                name = resolve(mServer, name);
            } catch (PluginException e) {
                return false;
            }
            log.debug("isRunningCache " + query + "-->" + name);
            beans.put(query, name);
            return true;
        }

        try {
            mServer.getMBeanInfo(name);
            return true;
        } catch (Exception e) {
            beans.remove(query);
            return false;
        }
    }

    /**
     * Wrapper around javax.management.MBeanServer.invoke
     *
     * @param objectName Used to construct a javax.management.ObjectName which
     * is required by MBeanServer.invoke
     * @param props Properties for the AdminClient connection, defined by
     * WebsphereProductPlugin.getConfigSchema.
     * @param method Name of the method to invoke on the server.
     * @param args Arguments passed to MBeanServer.invoke
     * @param sig Argument signatures passed to MBeanServer.invoke
     * @throws PluginException on any error.
     */
    public static Object invoke(String objectName,
            Properties props,
            String method,
            Object[] args, String[] sig)
            throws PluginException {

        AdminClient mServer;
        try {
            mServer = getMBeanServer(props);
        } catch (MetricUnreachableException e) {
            throw new PluginException(e.getMessage(), e);
        }

        return invoke(mServer, objectName, method, args, sig);
    }

    public static Object invoke(AdminClient mServer,
            String objectName,
            String method,
            Object[] args, String[] sig)
            throws PluginException {

        ObjectName obj;
        try {
            obj = new ObjectName(objectName);
        } catch (MalformedObjectNameException e) {
            throw new PluginException(e);
        }

        try {
            if (obj.isPattern()) {
                obj = resolve(mServer, obj);
            }
            return mServer.invoke(obj, method, args, sig);
        } catch (InstanceNotFoundException e) {
            throw new PluginException(e.getMessage(), e);
        } catch (MBeanException e) {
            throw new PluginException(e.getMessage(), e);
        } catch (ReflectionException e) {
            throw new PluginException(e.getMessage(), e);
        } catch (ConnectorException e) {
            throw new PluginException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }
}
