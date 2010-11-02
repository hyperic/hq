package org.hyperic.hq.inventory.domain;

import javax.persistence.ManyToOne;




public abstract class Resource {
    
    @ManyToOne
    //@JoinColumn(name="AGENT_ID", nullable=false)
    protected ResourceType resourceType;
    //protected int instanceId;
    //private AuthzSubject _owner;
    //protected long modifiedTime  = System.currentTimeMillis();
    //protected String name;
    //private String sortName;
    //private boolean      _system = false;
    //private Collection   _virtuals = new ArrayList();
    //private Collection   _fromEdges = new ArrayList();
    //private Collection   _toEdges = new ArrayList();

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }
    
    
}
