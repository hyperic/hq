/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 *
 * **********************************************************************
 * 29 April 2012
 * Maya Anderson
 * *********************************************************************/
package org.hyperic.hq.api.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/** 
 * Approved resource.
 * 
 * @since   4.5.0
 * @version 1.0 29 April 2012
 * @author Maya Anderson
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "resource", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ResourceTypeType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class ResourceModel extends Notification {

	@XmlAttribute
    private String id;
	@XmlAttribute
    private String name;
	@XmlAttribute 
    private ResourceTypeModel resourceType;
	@XmlAttribute
    private ResourceStatusType resourceStatusType ;

	@XmlElement
    private String naturalID ;
	@XmlElement
    private ResourcePrototype resourcePrototype;
	
	@XmlElementWrapper(name="subResources", namespace=RestApiConstants.SCHEMA_NAMESPACE)
	@XmlElement(name = "resource", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private List<ResourceModel> subResources;
	
	@XmlElement(name = "resourceConfig", namespace=RestApiConstants.SCHEMA_NAMESPACE)
	private ResourceConfig resourceConfig;

	// Set with non valid chars
	private final Character[] NON_VALID_VALUES = new Character[] { 0x000a, 0x0018, 0x000b, 0x003f };
	private final Set<Character> NON_VALID_CHAR_SET = new HashSet<Character>(Arrays.asList(NON_VALID_VALUES));
	
	
	//  @XmlElement(name = "ResourceProperty", required = true)
//	private List<ResourceProperty> resourceProperty;

	public ResourceModel(){}//EOM 
	
	public ResourceModel(final String id) { 
		this.id = id ; 
	}//EOM 
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
    	// Clear any illegal character in name 
		this.name = clearNonValidCharacters(name);
	}

    public ResourcePrototype getResourcePrototype() {
        return resourcePrototype;
    }
    public void setResourcePrototype(ResourcePrototype resourcePrototype) {
        this.resourcePrototype = resourcePrototype;
    }
    
    public void setResourcePrototypeFromString(final String name) {
        this.resourcePrototype = new ResourcePrototype(name) ; 
    }//EOM 
    
    public List<ResourceModel> getSubResources() {
        return subResources;
    }
    public void setSubResources(List<ResourceModel> subResources) {
        this.subResources = subResources;
    }
    
    public final void addSubResource(final ResourceModel subResource) { 
    	if(this.subResources == null) this.subResources = new ArrayList<ResourceModel>() ; 
    	this.subResources.add(subResource) ;
    }//EOM 
    
    public ResourceTypeModel getResourceType() {
        return resourceType;
    }
    public void setResourceType(ResourceTypeModel resourceType) {
        this.resourceType = resourceType;
    }    
    
    public final void setNaturalID(final String naturalID) { 
    	this.naturalID = naturalID ;  
    }//EOM 
    
    public final String getNaturalID() { 
    	return this.naturalID ; 
    }//EOM 
    
    public final void setResourceConfig(final ResourceConfig resourceConfig) { 
    	this.resourceConfig = resourceConfig ; 
    }//EOM 
    
    public final ResourceConfig getResourceConfig() { 
    	return this.resourceConfig ; 
    }//EOM
    
    public final void setResourceStatusType(final ResourceStatusType resourceStatusType) { 
    	this.resourceStatusType = resourceStatusType; 
    }//EOM 
    
    public final ResourceStatusType getResourceStatusType() { 
    	return this.resourceStatusType ; 
    }//EOM 
    
    @Override
   	public String toString() {
    	final StringBuilder builder = new StringBuilder(500) ; 
    	return this.toString(builder, "").toString() ; 
   	}
    
    public StringBuilder toString(final StringBuilder builder, final String indentation) {
    	
    	builder.append(indentation).append("Resource [id=").append(id).append(", name=" ).append(name).append(", uuid=").append(naturalID).append(", resourcePrototype=").
			append(resourcePrototype).append(", resourceType=").append(resourceType).append(", resourceStatusType=").append(resourceStatusType).append("]").
			append("\n").append(indentation).append(",resourceConfig=["); 
    		(resourceConfig == null ? builder : resourceConfig.toString(builder.append("\n") , indentation)).append("]").
    		append("\n").append(indentation).append(",subResources=[") ; 

		if(this.subResources != null) { 
			
			for(ResourceModel child : this.subResources) {
				child.toString(builder.append("\n"), indentation+"\t").append("\n") ; 
			}//EO while there are more resources 
		}//EO if subresources != null 
			
		builder.append("]") ;
		return builder ; 
    }//EOM 

    /**
     * This method was written for Rest API <b>getResources</b> call that returns resources data.
     * Any illegal character fails the API call. 
     * The method validates that the resource name will not contain any illegal character before save.
     * 
     * @param name the resource name
     * @return name without any illegal character
     */
    private String clearNonValidCharacters(String name){
    	if (name != null && !name.matches("\\A\\p{ASCII}*\\z")){
			StringBuilder cleanName = new StringBuilder(); 

			for (int i = 0; i < name.length(); i++) {
				if (!NON_VALID_CHAR_SET.contains(name.charAt(i))) {
					cleanName.append(name.charAt(i));
				}
			}
			
			return cleanName.toString();
    	}
    	return name;
    }
}
