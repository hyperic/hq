package org.hyperic.hq.plugin.jboss7.objects;

import java.util.Map;

public class ThreadsInfo {

    private String threadCount;
    private String peakThreadCount;
    private String totalStartedThreadCount;
    private String daemonThreadCount;
    private String currentThreadCpuTime;
    private String currentThreadUserTime;
    private Map<String,ThreadsInfo> domainResults;

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
