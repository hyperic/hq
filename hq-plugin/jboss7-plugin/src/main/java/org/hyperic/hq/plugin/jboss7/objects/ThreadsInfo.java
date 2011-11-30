package org.hyperic.hq.plugin.jboss7.objects;

public class ThreadsInfo {
    private String threadCount;
    private String peakThreadCount;
    private String totalStartedThreadCount;
    private String daemonThreadCount;
    private String currentThreadCpuTime;
    private String currentThreadUserTime;

    /**
     * @return the threadCount
     */
    public String getThreadCount() {
        return threadCount;
    }

    /**
     * @param threadCount the threadCount to set
     */
    public void setThreadCount(String threadCount) {
        this.threadCount = threadCount;
    }

    /**
     * @return the peakThreadCount
     */
    public String getPeakThreadCount() {
        return peakThreadCount;
    }

    /**
     * @param peakThreadCount the peakThreadCount to set
     */
    public void setPeakThreadCount(String peakThreadCount) {
        this.peakThreadCount = peakThreadCount;
    }

    /**
     * @return the totalStartedThreadCount
     */
    public String getTotalStartedThreadCount() {
        return totalStartedThreadCount;
    }

    /**
     * @param totalStartedThreadCount the totalStartedThreadCount to set
     */
    public void setTotalStartedThreadCount(String totalStartedThreadCount) {
        this.totalStartedThreadCount = totalStartedThreadCount;
    }

    /**
     * @return the daemonThreadCount
     */
    public String getDaemonThreadCount() {
        return daemonThreadCount;
    }

    /**
     * @param daemonThreadCount the daemonThreadCount to set
     */
    public void setDaemonThreadCount(String daemonThreadCount) {
        this.daemonThreadCount = daemonThreadCount;
    }

    /**
     * @return the currentThreadCpuTime
     */
    public String getCurrentThreadCpuTime() {
        return currentThreadCpuTime;
    }

    /**
     * @param currentThreadCpuTime the currentThreadCpuTime to set
     */
    public void setCurrentThreadCpuTime(String currentThreadCpuTime) {
        this.currentThreadCpuTime = currentThreadCpuTime;
    }

    /**
     * @return the currentThreadUserTime
     */
    public String getCurrentThreadUserTime() {
        return currentThreadUserTime;
    }

    /**
     * @param currentThreadUserTime the currentThreadUserTime to set
     */
    public void setCurrentThreadUserTime(String currentThreadUserTime) {
        this.currentThreadUserTime = currentThreadUserTime;
    }

    @Override
    public String toString() {
        return "ThreadsInfo{" + "threadCount=" + threadCount + ", peakThreadCount=" + peakThreadCount + ", totalStartedThreadCount=" + totalStartedThreadCount + ", daemonThreadCount=" + daemonThreadCount + ", currentThreadCpuTime=" + currentThreadCpuTime + ", currentThreadUserTime=" + currentThreadUserTime + '}';
    }
}
