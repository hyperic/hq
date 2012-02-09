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
package org.hyperic.hq.plugin.jboss7.objects;

import java.util.Map;

public class ThreadsInfo {

    private String threadCount;
    private String peakThreadCount;
    private String totalStartedThreadCount;
    private String daemonThreadCount;
    private String currentThreadCpuTime;
    private String currentThreadUserTime;
    private Map<String, ThreadsInfo> domainResults;

    public String getThreadCount() {
        return domainResults == null ? threadCount : domainResults.get("step-1").threadCount;
    }

    public String getPeakThreadCount() {
        return domainResults == null ? threadCount : domainResults.get("step-1").threadCount;
    }

    public String getTotalStartedThreadCount() {
        return domainResults == null ? totalStartedThreadCount : domainResults.get("step-1").totalStartedThreadCount;
    }

    public String getDaemonThreadCount() {
        return domainResults == null ? daemonThreadCount : domainResults.get("step-1").daemonThreadCount;
    }

    public String getCurrentThreadCpuTime() {
        return domainResults == null ? currentThreadCpuTime : domainResults.get("step-1").currentThreadCpuTime;
    }

    public String getCurrentThreadUserTime() {
        return domainResults == null ? currentThreadUserTime : domainResults.get("step-1").currentThreadUserTime;
    }

    @Override
    public String toString() {
        return "ThreadsInfo{" + "threadCount=" + threadCount + ", peakThreadCount=" + peakThreadCount + ", totalStartedThreadCount=" + totalStartedThreadCount + ", daemonThreadCount=" + daemonThreadCount + ", currentThreadCpuTime=" + currentThreadCpuTime + ", currentThreadUserTime=" + currentThreadUserTime + ", domainResults=" + domainResults + '}';
    }
}
