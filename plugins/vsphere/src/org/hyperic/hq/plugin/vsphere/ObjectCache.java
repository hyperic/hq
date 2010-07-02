package org.hyperic.hq.plugin.vsphere;

public class ObjectCache<T> {
    private final long timeout;
    private final T entity;
    private final long timestamp;
    ObjectCache(T obj, long timeout) {
        this.entity = obj;
        this.timestamp = now();
        this.timeout = timeout;
    }
    T getEntity() {
        return entity;
    }
    boolean isExpired() {
        return timestamp < (now()-timeout);
    }
    private long now() {
        return System.currentTimeMillis();
    }
}
