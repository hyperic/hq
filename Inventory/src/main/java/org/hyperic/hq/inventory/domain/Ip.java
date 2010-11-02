package org.hyperic.hq.inventory.domain;

import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.roo.addon.entity.RooEntity;

import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

@RooJavaBean
@RooToString
@RooEntity
public class Ip {

    @NotNull
    private String address;

    private String netmask;

    private String macAddress;
    
    @ManyToOne
    //@JoinColumn(name="AGENT_ID", nullable=false)
    private Platform platform;
}
