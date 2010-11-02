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
@RooEntity(finders = { "findPlatformsByFqdnEquals", "findPlatformsByFqdnLike" })
public class Platform {

    @NotNull
    private String fqdn;

    @NotNull
    private String name;

    @OneToOne
    private Config config;
    
    @ManyToOne
    //@JoinColumn(name="AGENT_ID", nullable=false)
    private Agent agent;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy="platform")
    private Set<Ip> ips = new HashSet<Ip>();
}
