package org.hyperic.hq.inventory.domain;

import java.io.Serializable;

public class ResourceTypeRelationshipId implements Serializable {

    private int fromId;
    
    private int toId;
    
    private String name;
   
    public int hashCode() {
      return (int)(fromId + toId) + name.hashCode();
    }
   
    public boolean equals(Object object) {
      if (object instanceof ResourceTypeRelationshipId) {
        ResourceTypeRelationshipId otherId = (ResourceTypeRelationshipId) object;
        return (otherId.toId == this.toId) && (otherId.fromId == this.fromId) && (otherId.name == this.name);
      }
      return false;
    }

}
