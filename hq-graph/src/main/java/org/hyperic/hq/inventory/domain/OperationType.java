package org.hyperic.hq.inventory.domain;

import org.hyperic.hq.reference.RelationshipTypes;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.api.Direction;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.entity.RooEntity;

import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

@NodeEntity(partial = true)
@RooToString
@RooJavaBean
@RooEntity
public class OperationType {

    @NotNull
    @Transient
    private String name;
    
    @ManyToOne
    @NotNull
    @Transient
    @RelatedTo(type = "HAS_OPERATIONS", direction = Direction.INCOMING, elementClass = ResourceType.class)
    private ResourceType resourceType;
    
    
}
