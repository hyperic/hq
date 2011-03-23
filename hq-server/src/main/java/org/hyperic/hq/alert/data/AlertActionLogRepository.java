package org.hyperic.hq.alert.data;

import java.util.List;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AlertActionLogRepository extends JpaRepository<AlertActionLog, Integer> {

    @Transactional
    @Modifying
    @Query("delete from AlertActionLog l where l.alert in (:alerts)")
    void deleteByAlerts(@Param("alerts") List<Alert> alerts);

    @Transactional
    @Modifying
    @Query("update AlertActionLog l set " + "l.subject = null " + "where l.subject = :subject")
    void removeSubject(@Param("subject") AuthzSubject subject);
}
