/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2011], Hyperic, Inc.
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
package org.hyperic.hq.plugin.jboss7;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.jboss7.objects.ServerMemory;
import org.hyperic.hq.plugin.jboss7.objects.ThreadsInfo;
import org.hyperic.hq.product.PluginException;

public class JBossHostControllerCollector extends JBoss7DefaultCollector {

    private static final Log log = LogFactory.getLog(JBossHostControllerCollector.class);

    @Override
    public void collect(JBossAdminHttp admin) {
        try {
            setAvailability(true);
            ThreadsInfo th = admin.getThreadsInfo();
            setValue("thread-count", th.getThreadCount());
            setValue("peak-thread-count", th.getPeakThreadCount());
            setValue("total-started-thread-count", th.getTotalStartedThreadCount());
            setValue("daemon-thread-count", th.getDaemonThreadCount());
            setValue("current-thread-cpu-time", th.getCurrentThreadCpuTime());
            setValue("current-thread-user-time", th.getCurrentThreadUserTime());
        } catch (PluginException ex) {
            setAvailability(false);
            log.debug(ex.getMessage(), ex);
        }

        try {
            ServerMemory sm = admin.getServerMemory();
            setValue("h.used.p", sm.getHeapMemoryUsage().getUsedPercentage());
            setValue("h.init", sm.getHeapMemoryUsage().getInit());
            setValue("h.used", sm.getHeapMemoryUsage().getUsed());
            setValue("h.committed", sm.getHeapMemoryUsage().getCommitted());
            setValue("h.max", sm.getHeapMemoryUsage().getMax());
            setValue("nh.used.p", sm.getNonHeapMemoryUsage().getUsedPercentage());
            setValue("nh.init", sm.getNonHeapMemoryUsage().getInit());
            setValue("nh.used", sm.getNonHeapMemoryUsage().getUsed());
            setValue("nh.committed", sm.getNonHeapMemoryUsage().getCommitted());
            setValue("nh.max", sm.getNonHeapMemoryUsage().getMax());
        } catch (PluginException ex) {
            log.debug(ex.getMessage(), ex);
        }
    }

    @Override
    public Log getLog() {
        return log;
    }
}
