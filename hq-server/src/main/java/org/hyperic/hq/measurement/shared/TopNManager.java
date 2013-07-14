package org.hyperic.hq.measurement.shared;

public interface TopNManager {

    void scheduleOrUpdateTopNCollection(int resourceId, int intervalInMinutes);

    void unscheduleTopNCollection(int resourceId);

    void updateGlobalTopNInterval(int intervalInMinutes);

}
