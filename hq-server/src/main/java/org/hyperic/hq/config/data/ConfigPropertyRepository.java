package org.hyperic.hq.config.data;

import java.util.List;

import javax.persistence.QueryHint;

import org.hyperic.hq.common.ConfigProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.transaction.annotation.Transactional;

public interface ConfigPropertyRepository extends JpaRepository<ConfigProperty, Integer> {

    @Transactional(readOnly = true)
    @Query("select c from ConfigProperty c")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "org.hyperic.hq.common.ConfigProperty.findAll") })
    List<ConfigProperty> findAllAndCache();

    List<ConfigProperty> findByPrefix(String prefix);
}
