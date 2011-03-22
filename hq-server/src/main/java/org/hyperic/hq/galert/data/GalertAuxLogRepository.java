package org.hyperic.hq.galert.data;

import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface GalertAuxLogRepository extends JpaRepository<GalertAuxLog, Integer>,
    GalertAuxLogRepositoryCustom {

    @Transactional
    @Query("delete from GalertAuxLog l where l.def = :def")
    @Modifying
    void deleteByDef(@Param("def") GalertDef def);
}
