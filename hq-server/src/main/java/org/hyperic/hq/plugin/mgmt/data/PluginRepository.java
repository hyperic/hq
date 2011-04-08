package org.hyperic.hq.plugin.mgmt.data;

import org.hyperic.hq.plugin.mgmt.domain.Plugin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PluginRepository extends JpaRepository<Plugin, Integer> {

    Plugin findByName(String name);

    @Transactional(readOnly = true)
    @Query("select p from Plugin p where :resType in elements(p.resourceTypes)")
    Plugin findByResourceType(@Param("resType") Integer resourceType);
}
