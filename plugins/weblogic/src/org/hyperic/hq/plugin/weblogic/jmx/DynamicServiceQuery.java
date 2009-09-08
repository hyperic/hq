package org.hyperic.hq.plugin.weblogic.jmx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * Query for dynamically discovered services in WebLogic
 * 
 * @author Jennifer Hickey
 * 
 */
public class DynamicServiceQuery extends ServiceQuery {
	
	private String[] attributeNames;

	private String type;
	
	private Map keyValues = new LinkedHashMap();

	/**
	 * 
	 */
	public DynamicServiceQuery() {

	}

	
	public WeblogicQuery cloneInstance() {
		throw new UnsupportedOperationException();
	}

	public void configure(Properties props) {
		props.put("name", getName());
		for(Iterator iterator = keyValues.entrySet().iterator();iterator.hasNext();) {
			Map.Entry keyEntry = (Map.Entry)iterator.next();
			props.put(keyEntry.getKey(),keyEntry.getValue());
		}
	}

	

	public String[] getAttributeNames() {
		if (this.attributeNames == null) {
			return super.getAttributeNames();
		}
		return attributeNames;
	}

	public boolean getAttributes(MBeanServerConnection mServer,
			ObjectName name, String[] attrNames) {
		if (name == null) {
			return false;
		}
		String alphabeticalkeyProps = name.getCanonicalKeyPropertyListString();
		final String[] keyProps = alphabeticalkeyProps.split(",");
		for(int i=0;i<keyProps.length;i++) {
			final String[] keyProp = keyProps[i].split("=");
			//remove name, type, and subtype.  We are assuming type and subtype were used to create the service type name (setType) and name was set as (setName)
			if(!("type".equals(keyProp[0])) && !("name".equals(keyProp[0])) && !("subtype".equals(keyProp[0]))) {
				keyValues.put(keyProp[0],keyProp[1]);
			}
		}
		boolean attributesObtained = super.getAttributes(mServer, name, attrNames);
		//name will be reset to null by super call if attrNames.length == 0
		setName(name.getKeyProperty("name"));
		return attributesObtained;
	}

	public String[] getCustomPropertiesNames() {
		if (this.attributeNames == null) {
			return super.getCustomPropertiesNames();
		}
		return attributeNames;
	}

	public String getFullName() {
		StringBuffer name = new StringBuffer();
		//ObjectName properties will likely overlap with parent query names somewhat so need to put in Set to make unique.  Parent name order should win
		Set names = new LinkedHashSet();
		List parentNames = new ArrayList();
		WeblogicQuery query = this;
		while ((query = query.getParent()) != null) {
			parentNames.add(query.getQualifiedName());
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



	public String getResourceType() {
		return this.type;
	}

	public String getScope() {
		throw new UnsupportedOperationException();
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
	 * 
	 * @param type
	 *            The name of the service type as defined in metadata, example
	 *            "Spring Bean Factory"
	 */
	public void setType(String type) {
		this.type = type;
	}
}
