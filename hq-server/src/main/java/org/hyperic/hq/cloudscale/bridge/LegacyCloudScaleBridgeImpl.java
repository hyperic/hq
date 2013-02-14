package org.hyperic.hq.cloudscale.bridge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LegacyCloudScaleBridgeImpl implements LegacycloudScaleBridge, InitializingBean {

    private static List<MediaType> ACCEPTABLE_MEDIA_TYPES;
    private static HttpHeaders HEADERS_TEMLPATE;
    private static final String CREATE_MEASUREMENTS_URI = "measurements/";
    private static final String DELETE_MEASUREMENTS_URI = "measurements/delete";
    private static final String DELETE_MEASUREMENTS_FOR_RESOURCE_URI = "measurements/delete/resource";
    private static String CREATE_MEASUREMENTS_REST_URL;
    private static String DELETE_MEASUREMENTS_REST_URL;
    private static String DELETE_MEASUREMENTS_FOR_RESOURCE_URL;

    @Autowired
    AgentManager agentManager ; 
    
    private String baseUrl;

    @Autowired
    public LegacyCloudScaleBridgeImpl(@Value("${cloudscale.base.http.url}") String cloudscaleBaseHttpUrl) {
        this.baseUrl = cloudscaleBaseHttpUrl;
    }// EOM

    public final void createMeasurements(final int agentId, final int resourceId, final List<Measurement> measurements) throws Throwable {
        if(measurements == null || measurements.isEmpty()) return ;
        final CloudScaleMeasurementRequest request = this.addMeasurementsToRequest(""+agentId, resourceId, measurements, true/*extractAlias*/,null/*request*/) ;
        
        this.sendMeasurementRequest(request, CREATE_MEASUREMENTS_REST_URL, HttpMethod.POST) ; 
    }//EOM
    
    public final void deleteMeasurements(final int agentId, final Integer resourceId, final List<Measurement> measurements) throws Throwable {
       final CloudScaleMeasurementRequest request = this.addMeasurementsToRequest(""+agentId, resourceId, measurements, false/*extractAlias*/,null/*request*/) ;  
        
        this.sendMeasurementRequest(request, DELETE_MEASUREMENTS_REST_URL, HttpMethod.POST) ; 
    }// EOM
    
    public final void deleteMeasurements(final Map<Integer, List<Resource>> agentResources) throws Throwable {
        if(agentResources == null || agentResources.isEmpty()) return ; 
        
        final CloudScaleMeasurementDeleteRequest deleteRequest = new CloudScaleMeasurementDeleteRequest() ; 
        
        String resourceIdPrefix = null ; 
        
        for(Map.Entry<Integer,List<Resource>> entry : agentResources.entrySet()) { 
            resourceIdPrefix = entry.getKey() + ":" ;
            for(Resource resource : entry.getValue()) { 
                deleteRequest.resourceIds.add(resourceIdPrefix + resource.getId()) ;
            }//EO while there are more resourceids 
        }//EO while there are more resources 
        
        this.sendMeasurementRequest(deleteRequest, DELETE_MEASUREMENTS_FOR_RESOURCE_URL, HttpMethod.POST) ;
    }//EOM 
    
    private final CloudScaleMeasurementRequest addMeasurementsToRequest(final String agentId, final Integer resourceId, final List<Measurement> entities,
            final boolean extractAlias, CloudScaleMeasurementRequest request) { 
        
        if(request == null) request = new CloudScaleMeasurementRequest(agentId) ;
        
        final String resourceIdStr = agentId + ":" + resourceId ; 
        
        for(Object entity : entities) { 
            if(entity instanceof Measurement) request.addCloudscaleMeasurement((Measurement)entity, resourceIdStr, extractAlias) ; 
        }//EO while there are more measurements 
        
        return request;  
    }//EOM 
    
    
    private final <T> void sendMeasurementRequest(final T request, final String url, final HttpMethod httpMethod) { 
        final RestTemplate template = new RestTemplate();
        
        final HttpEntity<T> entity = new HttpEntity<T>(request, HEADERS_TEMLPATE);
        template.exchange(url, httpMethod, entity, null/* responseType */, new HashMap<String, Object>());
    }//EOM 

    public final void afterPropertiesSet() throws Exception {
        ACCEPTABLE_MEDIA_TYPES = Arrays.asList(new MediaType[] { MediaType.APPLICATION_XML });
        HEADERS_TEMLPATE = new HttpHeaders();
        HEADERS_TEMLPATE.setAccept(ACCEPTABLE_MEDIA_TYPES);
        HEADERS_TEMLPATE.setContentType(MediaType.APPLICATION_XML);

        CREATE_MEASUREMENTS_REST_URL = this.baseUrl + CREATE_MEASUREMENTS_URI;
        DELETE_MEASUREMENTS_REST_URL = this.baseUrl + DELETE_MEASUREMENTS_URI;
        DELETE_MEASUREMENTS_FOR_RESOURCE_URL = this.baseUrl + DELETE_MEASUREMENTS_FOR_RESOURCE_URI;
    }// EOM
    
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "measurementDeleteRequest", namespace = SCHEMA_NAMESPACE)
    public static final class CloudScaleMeasurementDeleteRequest implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        @XmlElementWrapper(name = "resourceIds", namespace = SCHEMA_NAMESPACE)
        @XmlElement(name = "resId", namespace = SCHEMA_NAMESPACE)
        private List<String> resourceIds;
        
        public CloudScaleMeasurementDeleteRequest() { this.resourceIds = new ArrayList<String>() ; }//EOM 
        
    }//EO inner class CloudScaleMeasurementDeleteRequest

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "measurementRequest", namespace = SCHEMA_NAMESPACE)
    public static final class CloudScaleMeasurementRequest implements Serializable {

        private static final long serialVersionUID = 1L;

        @XmlElementWrapper(name = "measurments", namespace = SCHEMA_NAMESPACE)
        @XmlElement(name = "meas", namespace = SCHEMA_NAMESPACE)
        private List<CloudScaleMeasurement> measurements;

        @XmlAttribute
        private String agentId;

        public CloudScaleMeasurementRequest() {
        }// EOM

        public CloudScaleMeasurementRequest(final String agentId) {
            this.agentId = agentId;
        }// EOM
        
        public final void addCloudscaleMeasurement(final Measurement measurement, final String resourceId, final boolean extractAlias) {
            this.addCloudscaleMeasurement(measurement.getId(), 
                    resourceId,
                    (extractAlias ? measurement.getTemplate().getAlias() : null)) ;
        }//EOM 
        
        public final void addCloudscaleMeasurement(final int measurementId, final String resourceId) { 
            this.addCloudscaleMeasurement(measurementId, resourceId, null/*measurementAlias*/) ; 
        }//EOM
        
        public final void addCloudscaleMeasurement(final Integer measurementId, final String resourceId, final String measurementAlias) { 
            if(this.measurements == null) this.measurements = new ArrayList<CloudScaleMeasurement>() ; 
            this.measurements.add(new CloudScaleMeasurement(measurementId, resourceId, measurementAlias)) ;
        }//EOM 
        
    }// EO inner class CloudScaleMeasurementRequest

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(namespace = SCHEMA_NAMESPACE)
    private static final class CloudScaleMeasurement implements Serializable {

        @XmlAttribute(name = "id")
        public Integer measurementId;

        @XmlAttribute(name = "alias")
        public String measurementAlias;

        @XmlAttribute(name = "resId")
        public String resourceId;

        public CloudScaleMeasurement() {
        }// EOM

        public CloudScaleMeasurement(final Integer measurementId, final String resourceId, final String measurementAlias) {
            this.measurementId = measurementId;
            this.measurementAlias = measurementAlias;
            this.resourceId = resourceId;
        }// EOM

    }// EO inner class CloudScaleMeasurement
}// EOC
