package org.hyperic.hq.plugin.domain;

import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.hyperic.hq.reference.RelationshipTypes;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.api.Direction;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

@NodeEntity
@RooToString
@RooJavaBean
@RooEntity
public class PropertyType {

    @ManyToOne
    @NotNull
    @RelatedTo(type = RelationshipTypes.CONTAINS, direction = Direction.INCOMING, elementClass = ResourceType.class)
    private ResourceType resourceType;

    @NotNull
    private String name;

    @NotNull
    private String description;

    private Boolean optional;

    private Boolean secret;

    private String defaultValue;
}
