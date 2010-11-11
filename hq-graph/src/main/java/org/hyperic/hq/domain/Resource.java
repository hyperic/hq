package org.hyperic.hq.domain;

import javax.validation.constraints.NotNull;

import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

@NodeEntity
@RooToString
@RooJavaBean
@RooEntity
public class Resource {

    @NotNull
    private String name;

    
    public Relation relatedTo(Resource resource, String relationName) {
    	return (Relation)this.relateTo(resource,Relation.class,relationName);
    }
  
}
