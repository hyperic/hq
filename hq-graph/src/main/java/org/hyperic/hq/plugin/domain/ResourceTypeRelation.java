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
    private transient ResourceType from;

    @EndNode
    private transient ResourceType to;

	public ResourceType getFrom() {
		return this.from;
	}
	
	public ResourceType getTo() {
		return this.to;
	}
}
