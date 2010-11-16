package org.hyperic.hq.inventory.domain;

import java.util.Set;

import javax.persistence.ManyToMany;


import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.api.Direction;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

@NodeEntity
@RooToString
@RooJavaBean
@RooEntity()
public class ResourceGroup extends Resource {

    @RelatedTo(type = "CONTAINS", direction = Direction.OUTGOING, elementClass = Resource.class)
    @ManyToMany(targetEntity = Resource.class)
    private Set<Resource> members;
    
    public void addMember(Resource member) {
        members.add(member);
    }
    
    

}
