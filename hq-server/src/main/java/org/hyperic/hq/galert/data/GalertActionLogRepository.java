package org.hyperic.hq.galert.data;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.galerts.server.session.GalertActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface GalertActionLogRepository extends JpaRepository<GalertActionLog, Integer> {

    @Transactional
    @Modifying
    @Query("update GalertActionLog l set l.subject = null where l.subject = :subject")
    void removeSubject(@Param("subject") AuthzSubject subject);
}
