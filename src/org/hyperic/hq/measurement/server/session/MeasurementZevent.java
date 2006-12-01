package org.hyperic.hq.measurement.server.session;

import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

public class MeasurementZevent 
    extends Zevent
{
    static {
        // Pretty sure this is fine to do here.  Handy, even!
        ZeventManager.getInstance().registerEventClass(MeasurementZevent.class);
    }
    
    public static class MeasurementZeventSource implements ZeventSourceId {
        private final int _id;
        
        public MeasurementZeventSource(int id) {
            _id = id;
        }
        
        /**
         * Returns the id of the {@link RawMeasurement} that the event is
         * for.
         */
        public int getId() {
            return _id;
        }
        
        public int hashCode() {
            return _id;
        }
        
        public boolean equals(Object o) {
            if (o== this)
                return true;
            
            if (o == null || !(o instanceof MeasurementZeventSource)) 
                return false;

            return ((MeasurementZeventSource)o).getId() == getId();
        }
        
        public String toString() {
            return "MeasID[" + _id + "]";
        }
    }
    
    public static class MeasurementZeventPayload implements ZeventPayload {
        private MetricValue _val;
        
        public MeasurementZeventPayload(MetricValue val) {
            // Clone for now since it's mutable.  Boo! -- XXX
            _val = new MetricValue(val);  
        }
        
        public MetricValue getValue() {
            // Clone for now since it's mutable.  Boo! -- XXX
            return new MetricValue(_val);
        }
        
        public String toString() {
            return _val.toString();
        }
    }

    public MeasurementZevent(int measId, MetricValue val) {
        super(new MeasurementZeventSource(measId), 
              new MeasurementZeventPayload(val));
    }
}
