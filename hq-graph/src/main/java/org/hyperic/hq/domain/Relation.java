package org.hyperic.hq.domain;

import org.springframework.datastore.graph.annotation.EndNode;
import org.springframework.datastore.graph.annotation.RelationshipEntity;
import org.springframework.datastore.graph.annotation.StartNode;

@RelationshipEntity
public class Relation {
	@StartNode
	private Resource from;
	
	@EndNode
	private Resource to;
	
}
