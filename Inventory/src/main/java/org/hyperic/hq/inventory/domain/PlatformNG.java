package org.hyperic.hq.inventory.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooEntity
public class PlatformNG extends Resource{
    @NotNull
    private String fqdn;

    @NotNull
    private String name;

    @OneToOne
    private Config config;
    
    @ManyToOne
    //@JoinColumn(name="AGENT_ID", nullable=false)
    private AgentNG agent;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy="platform")
    private Set<IpNG> ips = new HashSet<IpNG>();
    
}
