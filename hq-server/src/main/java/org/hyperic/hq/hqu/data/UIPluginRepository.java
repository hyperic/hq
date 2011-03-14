package org.hyperic.hq.hqu.data;

import org.hyperic.hq.hqu.server.session.UIPlugin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UIPluginRepository extends JpaRepository<UIPlugin, Integer> {
    
    UIPlugin findByName(String name);
}
