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

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.AdminClientFactory;
import com.ibm.websphere.management.exception.ConnectorException;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;


/**
 * WebSphere JMX Utils for version 5.0 +
 * These utils simply wrap javax.management.* and WebSphere
 * interfaces.
 */
public class WebsphereUtil {

    private static Log log = LogFactory.getLog("WebSphereUtil");

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
        
        synchronized (mServer) {
            try {
                return mServer.getAttribute(objName,
                                            metric.getAttributeName());
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
            } catch (ConnectorException e) {
                String msg = "ConnectorException: " + e.getMessage();
                throw new PluginException(msg, e);
            }
        }
    }

    //XXX cache
    public static AdminClient getMBeanServer(Properties cfg)
        throws MetricUnreachableException {

        Properties props = WebspherePMI.getAdminProperties(cfg);

        try {
            return AdminClientFactory.createAdminClient(props);
        } catch (ConnectorException e) {
            throw new MetricUnreachableException(e.getMessage(), e);
        }
    }

    public static AdminClient getMBeanServer(Metric metric)
        throws MetricUnreachableException {

        return getMBeanServer(metric.getProperties());
    }

    //only use this for 1 metric at the moment, to
    //verify the admin server has been given proper
    //configuration.
    public static double getMBeanCount(Metric metric)
        throws MetricUnreachableException,
        MetricNotFoundException {

        double count = 0;
        AdminClient mServer = getMBeanServer(metric);
        ObjectName query;
        
        try {
            String queryName =
                mServer.getDomainName() + ":" +
                metric.getObjectPropString();
            query = new ObjectName(queryName);
        } catch (MalformedObjectNameException e) {
            throw new MetricInvalidException(metric.getObjectName());
        } catch (ConnectorException e) {
            throw new MetricUnreachableException(metric.getObjectName() +
                                                 ": " + e.getMessage(), e);
        }
        
        try {
            Set beans = mServer.queryNames(query, null);
            for (Iterator it=beans.iterator(); it.hasNext();) {
                ObjectName name = (ObjectName)it.next();
                try {
                    mServer.getAttribute(name, metric.getAttributeName());
                    count++;
                } catch (AttributeNotFoundException e) {
                    throw new MetricNotFoundException(metric.getObjectName());
                } catch (InstanceNotFoundException e) {
                    throw new MetricNotFoundException(metric.getObjectName());
                } catch (Exception e) {
                    //e.g. unauthorized
                    throw new MetricUnreachableException(metric.getObjectName() +
                                                         ": " + e.getMessage(), e);
                }
            }
        } catch (ConnectorException e) {
            throw new MetricUnreachableException(metric.getObjectName(), e);
        }

        if (count == 0) {
            throw new MetricNotFoundException(metric.getObjectName() +
                                              " (Invalid node name?)");
        }

        return count;
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

        ObjectName scope;

        try {
            scope = new ObjectName(domain + ":" +
                                   metric.getObjectPropString() + ",*");
        } catch (MalformedObjectNameException e) {
            throw new MetricInvalidException(metric.getObjectName());
        }

        Set beans;

        try {
            beans = mServer.queryNames(scope, null);
        } catch (ConnectorException e) {
            log.debug(scope + ": " + e, e);
            return false;
        }

        if (log.isDebugEnabled()) {
            log.debug(scope + ": returned " + beans.size() + " beans");
        }

        switch (beans.size()) {
          case 1:
            return true;
          case 0:
            return false;
          default:
            break;
        }

        String msg = metric.getObjectPropString() +
            " matched " + beans.size() + " MBeans";

        throw new MetricInvalidException(msg);
    }

    /**
     * Wrapper around javax.management.MBeanServer.invoke
     * @param objectName Used to construct a javax.management.ObjectName
     * which is required by MBeanServer.invoke
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

        ObjectName obj;
        try {
            obj = new ObjectName(objectName);
        } catch (MalformedObjectNameException e) {
            throw new PluginException(e);
        }

        try {
            AdminClient mServer = getMBeanServer(props);
            if (obj.isPattern()) {
                Set beans =
                    mServer.queryNames(obj, null);
                if (beans.size() == 1) {
                    ObjectName fullName =
                        (ObjectName)beans.iterator().next();
                    log.debug(obj + " resolved to: " + fullName);
                    obj = fullName;
                }
                else {
                    throw new PluginException(obj + ": returned " +
                                              beans.size() + " beans");
                }
            }
            return mServer.invoke(obj, method, args, sig);
        } catch (MetricUnreachableException e) {
            throw new PluginException(e.getMessage(), e);
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
