package org.hyperic.hq.measurement.shared;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.util.config.ConfigResponse;

public interface TopNManager {

    void scheduleTopNCollection(int resourceId, int intervalInMinutes, int numberOfProcesses);

    void scheduleTopNCollection(AppdefEntityID id, ConfigResponse config);

    void unscheduleTopNCollection(int resourceId, ConfigResponse config);

    void unscheduleTopNCollection(int resourceId);

    byte[] compressData(final byte[] data);

    byte[] uncompressData(final byte[] data);

    void updateGlobalTopNInterval(int intervalInMinutes);

    void unscheduleGlobalTopNCollection();

    void updateGlobalTopNNumberOfProcesses(int numberOfProcesses);

    int getNumberOfProcessesToShowForPlatform(int resourceId);

}
