package org.hyperic.hq.plugin.jboss7.objects;

public class ServerMemory {

    private MemoryStats heapMemoryUsage;
    private MemoryStats nonHeapMemoryUsage;
    private String objectPendingFinalizationCount;

    /**
     * @return the heapMemoryUsage
     */
    public MemoryStats getHeapMemoryUsage() {
        return heapMemoryUsage;
    }

    /**
     * @param heapMemoryUsage the heapMemoryUsage to set
     */
    public void setHeapMemoryUsage(MemoryStats heapMemoryUsage) {
        this.heapMemoryUsage = heapMemoryUsage;
    }

    /**
     * @return the nonHeapMemoryUsage
     */
    public MemoryStats getNonHeapMemoryUsage() {
        return nonHeapMemoryUsage;
    }

    /**
     * @param nonHeapMemoryUsage the nonHeapMemoryUsage to set
     */
    public void setNonHeapMemoryUsage(MemoryStats nonHeapMemoryUsage) {
        this.nonHeapMemoryUsage = nonHeapMemoryUsage;
    }

    /**
     * @return the objectPendingFinalizationCount
     */
    public String getObjectPendingFinalizationCount() {
        return objectPendingFinalizationCount;
    }

    /**
     * @param objectPendingFinalizationCount the objectPendingFinalizationCount to set
     */
    public void setObjectPendingFinalizationCount(String objectPendingFinalizationCount) {
        this.objectPendingFinalizationCount = objectPendingFinalizationCount;
    }

    @Override
    public String toString() {
        return "ServerMemory{" + "heapMemoryUsage=" + heapMemoryUsage + ", nonHeapMemoryUsage=" + nonHeapMemoryUsage + ", objectPendingFinalizationCount=" + objectPendingFinalizationCount + '}';
    }
}
