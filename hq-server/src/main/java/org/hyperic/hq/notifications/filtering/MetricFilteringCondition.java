package org.hyperic.hq.notifications.filtering;

import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;

public class MetricFilteringCondition extends FilteringCondition<Measurement> {
    protected Boolean isIndicator = null;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((isIndicator == null) ? 0 : isIndicator.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        MetricFilteringCondition other = (MetricFilteringCondition) obj;
        if(isIndicator == null) {
            if(other.isIndicator != null) return false;
        }else if(!isIndicator.equals(other.isIndicator)) return false;
        return true;
    }


    public MetricFilteringCondition(Boolean isIndicator) {
        this.isIndicator = isIndicator;
    }
    
    @Override
    public boolean check(Measurement m) {
        if (isIndicator!=null) {
            MeasurementTemplate t = m.getTemplate();
            return isIndicator.equals(t.isDesignate());
        }
        return true;
    }

    public Boolean getIsIndicator() {
        return isIndicator;
    }

    public void setIsIndicator(Boolean isIndicator) {
        this.isIndicator = isIndicator;
    }

    @Override
    public String toString() {
        return "isIndicator=" + isIndicator;
    }
}
