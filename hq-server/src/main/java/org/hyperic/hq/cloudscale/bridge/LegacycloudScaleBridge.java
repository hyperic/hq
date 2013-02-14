package org.hyperic.hq.cloudscale.bridge;

import java.util.List;
import java.util.Map;

import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.measurement.server.session.Measurement;

public interface LegacycloudScaleBridge {

    String SCHEMA_NAMESPACE="http://hyperic.vmware.com/rest";
   
    void createMeasurements(final int agentId, final int resourceId, final List<Measurement> measurements) throws Throwable ; 
    void deleteMeasurements(final Map<Integer, List<Resource>> agentResources) throws Throwable ;
    void deleteMeasurements(final int agentId, final Integer resourceId, final List<Measurement> measurements) throws Throwable ; 
    
}//EOI 
