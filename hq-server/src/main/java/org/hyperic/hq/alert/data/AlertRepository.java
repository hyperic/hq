package org.hyperic.hq.alert.data;

import java.util.List;

import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AlertRepository extends JpaRepository<Alert, Integer>, AlertRepositoryCustom {

    @Transactional
    @Modifying
    @Query("delete Alert a where a.id in (:ids)")
    void deleteByIds(@Param("ids") List<Integer> ids);

    @Transactional(readOnly = true)
    @Query("select COUNT(a) from Alert a where a.alertDefinition.resource = :res")
    int countByResource(@Param("res") Resource resource);

    @Transactional(readOnly = true)
    @Query("select min(a.ctime) from Alert a where a.fixed = false")
    long getOldestUnfixedAlertTime();

}
