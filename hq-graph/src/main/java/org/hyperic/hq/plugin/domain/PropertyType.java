package org.hyperic.hq.plugin.domain;

import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.hyperic.hq.reference.RelationshipTypes;
import org.springframework.datastore.graph.annotation.GraphProperty;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.api.Direction;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.transaction.annotation.Transactional;

@NodeEntity(partial=true)
@RooToString
@RooJavaBean
@RooEntity
public class PropertyType {

    @ManyToOne
    @NotNull
    @Transient
    @RelatedTo(type = RelationshipTypes.CONTAINS, direction = Direction.INCOMING, elementClass = ResourceType.class)
    private ResourceType resourceType;

    @NotNull
    @GraphProperty
    @Transient
    private String name;

    @NotNull
    @GraphProperty
    @Transient
    private String description;

    @GraphProperty
    @Transient
    private Boolean optional;

    @GraphProperty
    @Transient
    private Boolean secret;

    @GraphProperty
    @Transient
    private String defaultValue;

    @Transactional
    public void persist() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.persist(this);
        //TODO this call appears to be necessary to get PropertyType populated with its underlying node
        getId();
    }
}
