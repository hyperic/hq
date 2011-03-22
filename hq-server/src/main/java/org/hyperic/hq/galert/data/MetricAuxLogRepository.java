package org.hyperic.hq.galert.data;

import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.measurement.server.session.MetricAuxLogPojo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MetricAuxLogRepository extends JpaRepository<MetricAuxLogPojo, Integer>,
    MetricAuxLogRepositoryCustom {

    @Transactional
    @Modifying
    @Query("delete from MetricAuxLogPojo p where p.def = :def")
    void deleteByDef(@Param("def") GalertDef def);

    MetricAuxLogPojo findByAuxLog(GalertAuxLog auxLog);
}
