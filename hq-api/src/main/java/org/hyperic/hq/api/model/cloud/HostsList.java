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
package org.hyperic.hq.api.model.cloud;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "hosts", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class HostsList implements Serializable{

    private static final long serialVersionUID = 1L;
 
    @XmlElement(name="hosts", namespace=RestApiConstants.SCHEMA_NAMESPACE) 
    private List<Host> hosts ; 
    
    public HostsList(){}//EOM  
    
    public HostsList(List<Host> hosts) { 
        this.hosts = hosts ;  
    }//EOM 
    
    public final void setHosts(List<Host> hosts) { 
        this.hosts = hosts ; 
    }//EOM 
    
    public final List<Host> getHosts() { 
        return this.hosts ;  
    }//EOM
    
    public final void addHost(Host host) { 
        if(this.hosts == null) this.hosts = new ArrayList<Host>() ; 
        this.hosts.add(host) ;
    }//EOM 
    
    @Override
    public final String toString() {
        return "hosts:[" + this.hosts + "]" ; 
    }//EOM 
    
    
}//EOC 
