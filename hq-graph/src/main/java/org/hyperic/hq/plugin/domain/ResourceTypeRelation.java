package org.hyperic.hq.plugin.domain;

import org.springframework.datastore.graph.annotation.EndNode;
import org.springframework.datastore.graph.annotation.RelationshipEntity;
import org.springframework.datastore.graph.annotation.StartNode;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

@RelationshipEntity
@RooToString
@RooJavaBean
@RooEntity
public class ResourceTypeRelation {

    @StartNode
    private ResourceType from;

    @EndNode
    private ResourceType to;
}
