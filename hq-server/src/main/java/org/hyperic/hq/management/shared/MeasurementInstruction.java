package org.hyperic.hq.management.shared;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;

@SuppressWarnings("serial")
public class MeasurementInstruction extends PersistedObject {
    
    private long interval;
    private boolean defaultOn;
    private boolean indicator;
    private MeasurementTemplate measurementTemplate;
    private ManagementProfile managementProfile;

    public long getInterval() {
        return interval;
    }
    public void setInterval(long interval) {
        this.interval = interval;
    }
    public boolean isDefaultOn() {
        return defaultOn;
    }
    public void setDefaultOn(boolean defaultOn) {
        this.defaultOn = defaultOn;
    }
    public boolean isIndicator() {
        return indicator;
    }
    public void setIndicator(boolean indicator) {
        this.indicator = indicator;
    }
    public MeasurementTemplate getMeasurementTemplate() {
        return measurementTemplate;
    }
    public void setMeasurementTemplate(MeasurementTemplate measurementTemplate) {
        this.measurementTemplate = measurementTemplate;
    }
    public ManagementProfile getManagementProfile() {
        return managementProfile;
    }
    public void setManagementProfile(ManagementProfile managementProfile) {
        this.managementProfile = managementProfile;
    }
    
    public int hashCode() {
        return super.hashCode();
    }
    
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof MeasurementInstruction) {
            return super.equals(o);
        }
        return false;
    }

}
