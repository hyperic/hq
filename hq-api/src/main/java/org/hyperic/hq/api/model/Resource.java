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

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/** 
 * Approved resource.
 * 
 * @since   4.5.0
 * @version 1.0 29 April 2012
 * @author Maya Anderson
 */
@XmlRootElement(name = "Resource")
public class Resource {
    
    
//    
//    @XmlElement(name = "ResourceConfig", required = true)
//    protected List<ResourceConfig> resourceConfig;
//    @XmlElement(name = "ResourceProperty", required = true)
//    protected List<ResourceProperty> resourceProperty;

    private String id;
    private String name;
    
    private ResourcePrototype resourcePrototype;
    private List<Resource> subResources;
    private ResourceType resourceType;
    
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
        this.name = name;
    }
    public ResourcePrototype getResourcePrototype() {
        return resourcePrototype;
    }
    public void setResourcePrototype(ResourcePrototype resourcePrototype) {
        this.resourcePrototype = resourcePrototype;
    }
    public List<Resource> getSubResources() {
        return subResources;
    }
    public void setSubResources(List<Resource> subResources) {
        this.subResources = subResources;
    }
    
    public ResourceType getResourceType() {
        return resourceType;
    }
    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }    
}
