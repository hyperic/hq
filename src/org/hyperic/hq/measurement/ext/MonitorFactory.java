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

package org.hyperic.hq.measurement.ext;

import java.util.Hashtable;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hyperic.hq.measurement.monitor.MonitorCreateException;
import org.hyperic.hq.product.server.MBeanUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 */

public class MonitorFactory {
    private static final Log log = LogFactory.getLog(MonitorFactory.class);
    private static final String logCtx      = MonitorFactory.class.getName();

    private static Hashtable    monitors    = new Hashtable();
    private static Properties   monitorCls  = new Properties();
    
    private static MBeanServer  mServer     = null;
    private static ObjectName   propName    = null;

    public static MonitorInterface newInstance(String protocol)
        throws MonitorCreateException {

        String monitorClass = getMonitorClass(protocol);

        // See if we already have an instance cached
        MonitorInterface monitor =
            (MonitorInterface) monitors.get(monitorClass);
        if (monitor != null)
            return monitor;

        try {
            Class c = Class.forName(monitorClass);
            monitor = (MonitorInterface) c.newInstance();
            monitors.put(monitorClass, monitor);
            return monitor;
        } catch (ClassNotFoundException e) {
            throw new MonitorCreateException(logCtx, e);
        } catch (InstantiationException e) {
            throw new MonitorCreateException(logCtx, e);
        } catch (IllegalAccessException e) {
            throw new MonitorCreateException(logCtx, e);
        }
    }

    private static String getMonitorClass(String protocol) {
        final String spider = "covalent-eam";

        if (!monitorCls.containsKey(spider)) {
            try {
                monitorCls.setProperty(protocol,
                                       getProperty(spider + ".monitor"));
            } catch (PropertyNotFoundException e) {
                // Should never happen
                log.error(e);
            }
        }

        try {
            monitorCls.setProperty(protocol,
                                   getProperty(protocol + ".monitor"));
        } catch (PropertyNotFoundException e) {
            // Failsafe, dump it to the Spider agent, heh
            protocol = spider;
        }

        // Look up the protocol
        return monitorCls.getProperty(protocol);
    }

    public static String getProperty(String prop)
        throws PropertyNotFoundException {
        try {
            if (mServer == null) {
                mServer = MBeanUtil.getMBeanServer();
            
                propName = new ObjectName(
                    "jboss:type=Service,name=MeasurementSystemProperties");
            }
            
            Object obj = mServer.invoke(
                            propName, "get", 
                            new Object[] { prop, null },
                            new String[] { String.class.getName(),
                                           String.class.getName() });

            if (obj == null) {
                throw new PropertyNotFoundException(prop + " not found");
            }

            return (String) obj;
        } catch (Exception e) {
            throw new PropertyNotFoundException(e);
        }
    }

}
