package org.hyperic.hq.domain;

import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.entity.RooEntity;
import javax.validation.constraints.NotNull;

@NodeEntity
@RooToString
@RooJavaBean
@RooEntity
public class Resource {

    @NotNull
    private String name;
}
