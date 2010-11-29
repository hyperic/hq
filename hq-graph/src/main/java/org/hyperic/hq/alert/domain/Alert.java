package org.hyperic.hq.alert.domain;

import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.reference.RelationshipTypes;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.api.Direction;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

@RooJavaBean
@RooToString
@RooEntity
@NodeEntity(partial = true)
public class Alert {

    //TODO Boolean values not persisting to MySQL.  Need to check dialect
    //private Boolean fixed;

    @ManyToOne
    @Transient
    @RelatedTo(type = "ALERTING_ELEMENT", direction = Direction.OUTGOING, elementClass = org.hyperic.hq.inventory.domain.Resource.class)
    private Resource resource;

    private String comment;

    @Transactional
    public void setResource(Resource resource) {
        relateTo(resource, DynamicRelationshipType.withName("ALERTING_ELEMENT"));
    }

    @Transactional
    public void persist() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.persist(this);
        //TODO this call appears to be necessary to get Alert populated with its underlying node
        getId();
    }
}
