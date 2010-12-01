package org.hyperic.hq.inventory.domain;

import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.roo.addon.entity.RooEntity;
import javax.validation.constraints.NotNull;

@RooJavaBean
@RooToString
@RooEntity(finders = { "findPlatformTypesByNameEquals" })
public class PlatformType {

    @NotNull
    private String name;
}
