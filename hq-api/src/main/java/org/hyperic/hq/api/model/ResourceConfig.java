rc/main/java/org/hyperic/hq/api/model/common/MapPropertiesAdapter.java
vim hq-api/src/main/java/org/hyperic/hq/api/model/common/PropertyListMapAdapter.java
vim hq-api/src/main/java/org/hyperic/hq/api/model/common/PropertyListMapElements.java
vim hq-api/src/main/java/org/hyperic/hq/api/model/common/PropertyMapElements.java
vim hq-api/src/main/java/org/hyperic/hq/api/model/measurements/Measurement.java
vim hq-api/src/main/java/org/hyperic/hq/api/model/measurements/MeasurementRequest.java
vim hq-api/src/main/java/org/hyperic/hq/api/model/measurements/MeasurementResponse.java
vim hq-api/src/main/java/org/hyperic/hq/api/model/measurements/MetricResponse.java
vim hq-api/src/main/java/org/hyperic/hq/api/model/measurements/ResourceMeasurementResponse.java
vim hq-api/src/main/java/org/hyperic/hq/api/services/AIResourceService.java
vim hq-api/src/main/java/org/hyperic/hq/api/services/MeasurementService.java
vim hq-api/src/main/java/org/hyperic/hq/api/services/MetricService.java
vim hq-api/src/main/java/org/hyperic/hq/api/services/ResourceService.java
vim hq-api/src/main/java/org/hyperic/hq/api/services/impl/MeasurementServiceImpl.java
vim hq-api/src/main/java/org/hyperic/hq/api/services/impl/MetricServiceImpl.java
vim hq-api/src/main/java/org/hyperic/hq/api/services/impl/ResourceServiceImpl.java
vim hq-api/src/main/java/org/hyperic/hq/api/transfer/MeasurementTransfer.java
vim hq-api/src/main/java/org/hyperic/hq/api/transfer/impl/MeasurementTransferImpl.java
vim hq-api/src/main/java/org/hyperic/hq/api/transfer/mapping/MeasurementMapper.java
vim hq-api/src/main/java/org/hyperic/hq/api/transfer/mapping/ResourceMapper.java
vim hq-api/src/main/resources/META-INF/hqapi-context.xml
vim hq-integration-tests/src/test/java/org/hyperic/hq/api/resources/ResourceServiceTest.java
vim hq-web/src/test/java/org/hyperic/hq/rest/api/AIResourceServiceTest.java
import java.io.Serializable; 
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
 
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hyperic.hq.api.model.common.MapPropertiesAdapter;
import org.hyperic.hq.api.model.common.PropertyListMapAdapter;

@XmlRootElement(namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ResourceConfigType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
	
public class ResourceConfig implements Serializable{
	private static final long serialVersionUID = 8233944180632888593L;
 	
	private String resourceID; 
	private HashMap<String,String> mapProps ; 
    private Map<String,PropertyList> mapListProps;
	
	public ResourceConfig() {}//EOM
	
	public ResourceConfig(final String resourceID, final HashMap<String,String> mapProps, final Map<String,PropertyList> mapListProps) { 
		this.resourceID = resourceID ; 
		this.mapProps = mapProps ; 
		this.mapListProps = mapListProps ;
	}//EOM 
	
	public final void setResourceID(final String resourceID) { 
		this.resourceID = resourceID ; 
	}//EOM 
	
	public final String getResourceID() { 
		return this.resourceID ; 
	}//EOM 
	
	public final void setMapProps(final HashMap<String,String> configValues) { 
		this.mapProps= configValues ; 
	}//EOM 
	
    @XmlJavaTypeAdapter(MapPropertiesAdapter.class)
	public final HashMap<String,String> getMapProps() { 
		return this.mapProps ; 
	}//EOM 

    @XmlJavaTypeAdapter(PropertyListMapAdapter.class)
	public Map<String,PropertyList> getMapListProps() {
        return mapListProps;
    }

    public void setMapListProps(Map<String,PropertyList> mapListProps) {
        this.mapListProps = mapListProps;
    }

    /**
     * Adds properties to the given key if it already exists,
     * otherwise adds the key with the given properties list 
     * @param key
     * @param properties Values to add to the Property list 
     */
    public void putMapListProps(String key, Collection<ConfigurationValue> properties) {
        if (null == this.mapListProps) {
            this.mapListProps = new HashMap<String, PropertyList>();            
        }
        if (mapListProps.containsKey(key)) {
            mapListProps.get(key).addAll(properties);
        } else {
            mapListProps.put(key, new PropertyList(properties));
        }              
    }
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((resourceID == null) ? 0 : resourceID.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourceConfig other = (ResourceConfig) obj;
		if (resourceID == null) {
			if (other.resourceID != null)
				return false;
		} else if (!resourceID.equals(other.resourceID))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder() ; 
		return this.toString(builder, "").toString() ; 
	}//EOM 
	
	public final StringBuilder toString(final StringBuilder builder, final String indentation) { 
		return builder.append(indentation).append("ResourceConfig [resourceID=").append(resourceID).append("\n").append(indentation).append(", mapProps=").
			append(mapProps.toString().replaceAll(",",  "\n"+indentation + " ")).append("]") ; 
	}//EOM 
	
}//EOC 
