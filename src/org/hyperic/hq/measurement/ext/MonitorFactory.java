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

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.server.MBeanUtil;

/**
 */

public class MonitorFactory {
    private static final Log log = LogFactory.getLog(MonitorFactory.class);
    private static final String logCtx      = MonitorFactory.class.getName();
    
    private static MBeanServer  mServer     = null;
    private static ObjectName   propName    = null;

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
