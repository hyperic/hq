package org.hyperic.hq.alert.data;

import java.util.List;

import org.hyperic.hq.events.server.session.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AlertRepository extends JpaRepository<Alert, Integer>, AlertRepositoryCustom {

    @Transactional(readOnly = true)
    @Query("select COUNT(a) from Alert a where a.alertDefinition.resource = :res")
    long countByResource(@Param("res") Integer resource);

    @Transactional
    @Modifying
    @Query("delete Alert a where a.id in (:ids)")
    void deleteByIds(@Param("ids") List<Integer> ids);

    List<Alert> findByFixedOrderByCtimeAsc(boolean fixed);

    @Transactional(readOnly = true)
    @Query("select a from Alert a where a.fixed = :fixed and "
           + "a.alertDefinition.resource in (:resources) order by a.ctime")
    List<Alert> findByResourcesAndFixedOrderByCtimeAsc(@Param("resources") List<Integer> resources,
                                                       @Param("fixed") boolean fixed);

}
