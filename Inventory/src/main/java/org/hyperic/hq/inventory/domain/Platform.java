package org.hyperic.hq.inventory.domain;

import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.roo.addon.entity.RooEntity;
import javax.validation.constraints.NotNull;

@RooJavaBean
@RooToString
@RooEntity(finders = { "findPlatformsByFqdnEquals", "findPlatformsByFqdnLike" })
public class Platform {

    @NotNull
    private String fqdn;
}
