package org.hyperic.hq.measurement.server.session;

import org.hyperic.hibernate.PersistedObject;

public class AvailabilityDataRLE
    extends PersistedObject {

    private long _startime;
    private long _endtime;
    private double _availVal;
    private Measurement _measurement;
    private AvailabilityDataId id;
    private static final long MAX_ENDTIME = Long.MAX_VALUE;
    
    public AvailabilityDataRLE() {
    }

    public AvailabilityDataRLE(Measurement meas, long startime,
                               long endtime, double availType) {
        init(meas, startime, endtime, availType);
    }

    public AvailabilityDataRLE(Measurement meas, long startime,
                               double availType) {
        init(meas, startime, MAX_ENDTIME, availType);
    }
    
    public static long getLastTimestamp() {
        return MAX_ENDTIME;
    }
    
    private void init(Measurement meas, long startime, long endtime,
            double availVal) {
        _measurement = meas;
        _startime = startime;
        _endtime = endtime;
        _availVal = availVal;
    }

    public Measurement getMeasurement() {
        return _measurement;
    }

    public void setMeasurement(Measurement meas) {
        _measurement = meas;
    }
    
    protected void setAvailabilityDataId(AvailabilityDataId id) {
        _startime = id.getStartime();
        _measurement = id.getMeasurement();
    }

    public AvailabilityDataId getAvailabilityDataId() {
        if (id == null) {
            id = new AvailabilityDataId(_startime, _measurement);
        }
        return id;
    }

    public long getStartime() {
        return _startime;
    }

    public void setStartime(long startime) {
        _startime = startime;
    }

    public long getEndtime() {
        return _endtime;
    }

    public void setEndtime(long endtime) {
        _endtime = endtime;
    }

    public double getAvailVal() {
        return _availVal;
    }

    public void setAvailVal(double val) {
        _availVal = val;
    }
    
    public long getApproxEndtime() {
        long approxEndtime = _endtime;
        
        if (approxEndtime == MAX_ENDTIME) {        
            long interval = _measurement.getInterval();
            // java will round down
            long multiplier = (System.currentTimeMillis() - _startime) / interval;
            
            approxEndtime = _startime + (multiplier * interval);        
        }
        
        return approxEndtime;
    }
    
    public int hashCode() {
        return getAvailabilityDataId().hashCode();
    }

    public boolean equals(Object rhs) {
        if (this == rhs) {
            return true;
        }
        if (!(rhs instanceof AvailabilityDataRLE)) {
            return false;
        }
        AvailabilityDataRLE rle = (AvailabilityDataRLE)rhs;
        AvailabilityDataId id = rle.getAvailabilityDataId();
        return getAvailabilityDataId().equals(id);
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        return buf.append(" measurement -> ").append(_measurement.getId())
               .append(" startime -> ").append(_startime)
               .append(" endtime -> ").append(_endtime)
               .append(" approxEndtime -> ").append(getApproxEndtime())
               .append(" availVal -> ").append(_availVal).toString();
    }
}
