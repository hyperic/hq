package org.hyperic.hq.galert.data;

import org.hyperic.hq.appdef.server.session.ResourceAuxLogPojo;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ResourceAuxLogRepository extends JpaRepository<ResourceAuxLogPojo, Integer> {

    @Transactional
    @Modifying
    @Query("delete from ResourceAuxLogPojo p where p.def = :def")
    void deleteByDef(@Param("def") GalertDef def);

    ResourceAuxLogPojo findByAuxLog(GalertAuxLog galertAuxLog);
}
