package org.hyperic.hq.measurement;

public interface DataPurgeJob extends Runnable {

    void truncateMeasurementData(long truncateBefore);
    
    void purgeMeasurements(long dataInterval, long purgeAfter);
    
    void purgeMetricProblems(long purgeAfter);
    
    long compressData(long toInterval, long now);
}
