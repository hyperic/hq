package org.hyperic.hq.inventory.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooEntity
public class AgentNG {
    
    @NotNull
    private String address;

    @NotNull
    private Integer port;

    @NotNull
    private String authToken;

    @NotNull
    private String agentToken;

    @NotNull
    private String agentVersion;

    @NotNull
    private Boolean unidirectional;

    @OneToMany(cascade = CascadeType.ALL, mappedBy="agent")
    private Set<PlatformNG> platforms = new HashSet<PlatformNG>();
}
