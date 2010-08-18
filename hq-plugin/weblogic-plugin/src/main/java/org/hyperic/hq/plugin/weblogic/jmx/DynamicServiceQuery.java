/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.plugin.weblogic.jmx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

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

	public boolean getAttributes(MBeanServer mServer,
			ObjectName name, String[] attrNames) {
	    throw new UnsupportedOperationException("This operation is not supported.  Use getDynamicAttributes with WebLogic version 9.1 or higher");	
	}
	
	
    public boolean getAttributes(MBeanServer mServer, ObjectName name) {
        throw new UnsupportedOperationException("This operation is not supported.  Use getDynamicAttributes with WebLogic version 9.1 or higher");
    }

    public boolean getDynamicAttributes(MBeanServerConnection mServer,
	                                    ObjectName name) {
	   String[] attrNames = getAttributeNames();
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
        boolean attributesObtained = getAttributes(mServer, name, attrNames);
        //name will be reset to null by super call if attrNames.length == 0
        setName(name.getKeyProperty("name"));
        return attributesObtained; 
	}
    
    private boolean getAttributes(MBeanServerConnection mServer,
                                 ObjectName name,
                                 String[] attrNames) {

        if (name == null) {
            return false;
        }
        if (attrNames.length == 0) {
            setName(name.getKeyProperty("Name"));
            return true;
        }

        AttributeList list;

        try {
            list = mServer.getAttributes(name, attrNames);
        } catch (InstanceNotFoundException e) {
            //given that the ObjectName is from queryNames
            //returned by the server this should not happen.
            //however, it is possible when nodes are not properly
            //configured.
            logAttrFailure(name, attrNames,e);
            return false;
        } catch (ReflectionException e) {
            //this should not happen either
            logAttrFailure(name, attrNames,e);
            return false;
        }catch (IOException e) {
            //this should not happen either
            logAttrFailure(name, attrNames,e);
            return false;
        }  

        if (list == null) {
            //only 6.1 seems to behave this way,
            //modern weblogics throw exceptions.
            return false;
        }

        for (int i=0; i<list.size(); i++) {
            Attribute attr = (Attribute)list.get(i);
            Object obj = attr.getValue();
            if (obj != null) {
                this.attrs.put(attr.getName(), obj.toString());
            }
        }

        return true;
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
