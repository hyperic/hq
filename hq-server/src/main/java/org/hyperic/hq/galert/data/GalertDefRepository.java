package org.hyperic.hq.galert.data;

import java.util.List;

import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface GalertDefRepository extends JpaRepository<GalertDef, Integer> {

    @Transactional(readOnly = true)
    @Query("select d from GalertDef d where d.deleted = false order by d.name")
    List<GalertDef> findAllExcludeDeletedOrderByName();

    List<GalertDef> findByEscalation(Escalation escalation);

    List<GalertDef> findByGroup(Integer group);

    @Transactional(readOnly = true)
    @Query("select d from GalertDef d where d.group= :group and d.deleted = false order by d.name")
    List<GalertDef> findByGroupExcludeDeletedOrderByName(@Param("group") Integer group);
}
