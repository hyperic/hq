package org.hyperic.hq.api.model.measurements;

import java.io.Serializable;

public class MeasurementAlias implements Serializable {
    private static final long serialVersionUID = 4570879955642000149L;
    private String measurementAlias;
    private String resourceId;

    public String getMeasurementAlias() {
        return measurementAlias;
    }
    public void setMeasurementAlias(String measurementAlias) {
        this.measurementAlias = measurementAlias;
    }
    public String getResourceId() {
        return resourceId;
    }
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
}
