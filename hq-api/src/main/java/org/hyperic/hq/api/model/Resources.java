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
 */
package org.hyperic.hq.api.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "resources", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ResourcesType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class Resources implements Serializable{

	private static final long serialVersionUID = -7751427064022499930L;
 
	@XmlElement(name="resource", namespace=RestApiConstants.SCHEMA_NAMESPACE) 
	private List<ResourceModel> resources ; 
	
	public Resources(){}//EOM  
	
	public Resources(final List<ResourceModel> resources) { 
		this.resources = resources ;  
	}//EOM 
	
	public final void setResources(final List<ResourceModel> resources) { 
		this.resources = resources ; 
	}//EOM 
	
	public final List<ResourceModel> getResources() { 
		return this.resources ;  
	}//EOM
	
	public final void addResource(final ResourceModel resource) { 
		if(this.resources == null) this.resources = new ArrayList<ResourceModel>() ; 
		this.resources.add(resource) ;
	}//EOM 
	
	@Override
	public final String toString() {
		return "resources:[" + this.resources + "]" ; 
	}//EOM 
	
	
}//EOC 
