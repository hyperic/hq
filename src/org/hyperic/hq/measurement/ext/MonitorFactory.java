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
import org.hyperic.hq.measurement.monitor.MonitorCreateException;

/**
 * XXX: Remove this class.
 */
public class MonitorFactory {
    private static final String logCtx      = MonitorFactory.class.getName();
    private static final String MONITOR_CLASS =
        "org.hyperic.hq.measurement.agent.client.AgentMonitor";

    private static Hashtable    monitors    = new Hashtable();

    public static MonitorInterface newInstance()
        throws MonitorCreateException {

        String monitorClass = MONITOR_CLASS;

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
}
