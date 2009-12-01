package org.hyperic.hq.plugin.websphere.jmx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.ObjectName;

/**
 * An auto-discovery query for a particular dynamic service deployed within
 * WebSphere
 * 
 * @author Jennifer Hickey
 * 
 */
public class DynamicServiceQuery extends WebSphereQuery {
	private String[] attributeNames;

	private Map keyValues = new LinkedHashMap();

	private String type;

	/**
	 * 
	 */
	public DynamicServiceQuery() {

	}

	public WebSphereQuery cloneInstance() {
		throw new UnsupportedOperationException();
	}

	public String[] getAttributeNames() {
		if (this.attributeNames == null) {
			return super.getAttributeNames();
		}
		return attributeNames;
	}
	
	public String getFullName() {
		StringBuffer name = new StringBuffer();
		//ObjectName properties will likely overlap with parent query names somewhat so need to put in Set to make unique.  Parent name order should win
		Set names = new LinkedHashSet();
		List parentNames = new ArrayList();
		WebSphereQuery query = this;
		while ((query = query.getParent()) != null) {
			parentNames.add(query.getName());
		}
		for (int i = parentNames.size() - 1; i >= 0; i--) {
			names.add(parentNames.get(i));
		}
		
		for(Iterator iterator = keyValues.values().iterator();iterator.hasNext();) {
			names.add(iterator.next());
		}
		names.add(getName());
		for(Iterator iterator = names.iterator();iterator.hasNext();) {
			name.append((String)iterator.next()).append(" ");
		}
		name.deleteCharAt(name.length() - 1);
		return name.toString();
	}

	public Properties getProperties() {
		Properties props = new Properties();
		props.put("name", getName());
		for(Iterator iterator = keyValues.entrySet().iterator();iterator.hasNext();) {
			Map.Entry keyEntry = (Map.Entry)iterator.next();
			props.put(keyEntry.getKey(),keyEntry.getValue());
		}
		return props;
	}

	public String getResourceType() {
		return this.type;
	}

	public String getScope() {
		throw new UnsupportedOperationException();
	}

	public boolean hasControl() {
        return true;
    }

	/**
	 * 
	 * @param attributeNames
	 *            The names of attributes to retreive from the MBean. Should
	 *            match any attribute defined as custom properties of the
	 *            service
	 */
	public void setAttributeNames(String[] attributeNames) {
		this.attributeNames = attributeNames;
	}

	/**
	 * Overrides the setObjectName method of parent in order to set the key
	 * properties of this service
	 */
	public void setObjectName(ObjectName name) {
		super.setObjectName(name);
		String alphabeticalkeyProps = name.getCanonicalKeyPropertyListString();
		final String[] keyProps = alphabeticalkeyProps.split(",");
		for(int i=0;i<keyProps.length;i++) {
			final String[] keyProp = keyProps[i].split("=");
			//remove name, type, and subtype.  We are assuming type and subtype were used to create the service type name (setType) and name was set as (setName)
			if(!("type".equals(keyProp[0])) && !("name".equals(keyProp[0])) && !("subtype".equals(keyProp[0]))) {
				keyValues.put(keyProp[0],keyProp[1]);
			}
		}
	}

	/**
	 * 
	 * @param type
	 *            The name of the service type as defined in metadata, example
	 *            "Spring Bean Factory"
	 */
	public void setType(String type) {
		this.type = type;
	}
}
