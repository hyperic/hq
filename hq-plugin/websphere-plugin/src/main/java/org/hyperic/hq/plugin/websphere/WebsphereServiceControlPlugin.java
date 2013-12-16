/**
 * NOTE: This copyright does *not* cover user programs that use HQ program
 * services by normal system calls through the application program interfaces
 * provided as part of the Hyperic Plug-in Development Kit or the Hyperic Client
 * Development Kit - this is merely considered normal use of the program, and
 * does *not* fall under the heading of "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc. This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify it under the terms
 * version 2 of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 */
package org.hyperic.hq.plugin.websphere;

import com.ibm.websphere.management.AdminClient;
import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MBeanUtil;

/**
 * Control plugin for services in WebSphere. Makes JMX
 * invoke/getAttribute/setAttribute calls using the AdminClient MBeanServer
 * wrapper
 *
 * @author Jennifer Hickey
 *
 */
public class WebsphereServiceControlPlugin extends ControlPlugin {

    private static final Log log = LogFactory.getLog(WebsphereServiceControlPlugin.class);

    @Override
    public void doAction(String action, String[] args) throws PluginException {
        invokeMethod(action, args);
    }

    protected Object invoke(AdminClient mServer, String objectName, String method, Object[] args, String[] sig)
            throws MetricUnreachableException, MetricNotFoundException, PluginException {

        ObjectName obj;
        try {
            obj = new ObjectName(objectName);
        } catch (MalformedObjectNameException e1) {
            throw new PluginException("Unable to create an ObjectName from " + objectName);
        }

        MBeanInfo info;
        try {
            info = mServer.getMBeanInfo(obj);
        } catch (Exception e1) {
            throw new PluginException("Unable to obtain MBeanInfo from " + objectName);
        }

        if (sig.length == 0) {
            MBeanUtil.OperationParams params = MBeanUtil.getOperationParams(info, method, args);
            if (params.isAttribute) {
                if (method.startsWith("set")) {
                    try {
                        mServer.setAttribute(obj, new Attribute(method.substring(3), params.arguments[0]));
                    } catch (AttributeNotFoundException e) {
                        throw new MetricNotFoundException(e.getMessage(), e);
                    } catch (Exception e) {
                        throw new PluginException(e);
                    }
                    return null;
                } else {
                    try {
                        return mServer.getAttribute(obj, method.substring(3));
                    } catch (AttributeNotFoundException e) {
                        throw new MetricNotFoundException(e.getMessage(), e);
                    } catch (Exception e) {
                        throw new PluginException(e);
                    }
                }
            }
            sig = params.signature;
            args = params.arguments;
        }

        try {
            return mServer.invoke(obj, method, args, sig);
        } catch (Exception e) {
            throw new PluginException(e);
        }
    }

    protected void invokeMethod(ControlPlugin plugin, String objectName, String action, String[] args) {
        log.debug("invoking " + action + " " + MBeanUtil.anyToString(args));

        try {
            String result = null;
            Object obj = invoke(WebsphereUtil.getMBeanServer(plugin.getConfig().toProperties()), objectName, action,
                    args, new String[0]);
            if (obj != null) {
                result = MBeanUtil.anyToString(obj);
            }
            log.debug(objectName + "." + action + "() returned: " + obj);
            plugin.setResult(RESULT_SUCCESS);
            if (result != null) {
                plugin.setMessage(result);
            }
        } catch (PluginException e) {
            log.error(e.getMessage(), e);
            plugin.setMessage(e.getMessage());
            plugin.setResult(RESULT_FAILURE);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            // anything not explicitly thrown by invoke
            // needs to have the full stack trace logged for debugging.
            plugin.setMessage(e.getMessage());
            plugin.setResult(RESULT_FAILURE);
        }
    }

    protected void invokeMethod(String action, String[] args) {
        String objectName = getTypeProperty("OBJECT_NAME");
        objectName = Metric.translate(objectName, getConfig());
        invokeMethod(this, objectName, action, args);
    }
}
