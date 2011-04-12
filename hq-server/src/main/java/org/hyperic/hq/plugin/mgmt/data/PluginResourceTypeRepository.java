package org.hyperic.hq.plugin.mgmt.data;

import javax.persistence.QueryHint;

import org.hyperic.hq.plugin.mgmt.domain.PluginResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PluginResourceTypeRepository extends JpaRepository<PluginResourceType, Integer> {

    @Transactional(readOnly = true)
    @Query("select p.pluginName from PluginResourceType p where p.resourceTypeId=:resourceType")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "Plugin.findByResourceType") })
    String findNameByResourceType(@Param("resourceType") Integer resourceType);
}
