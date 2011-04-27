package org.hyperic.hq.plugin.mgmt.data;

import org.hyperic.hq.plugin.mgmt.domain.Plugin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PluginRepository extends JpaRepository<Plugin, Integer> {

    Plugin findByName(String name);

}
