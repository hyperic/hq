package org.hyperic.hq.galert.data;

import java.util.List;

import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface GalertLogRepository extends JpaRepository<GalertLog, Integer>,
    GalertLogRepositoryCustom {

    @Transactional
    @Query("delete from GalertLog l where l.def = :def")
    @Modifying
    void deleteByDef(@Param("def") GalertDef def);

    @Transactional
    @Query("delete from GalertLog l where l.def in (select d from GalertDef d where d.group=:group)")
    @Modifying
    void deleteByGroup(@Param("group") Integer group);

    List<GalertLog> findByDefGroupOrderByTimestampAsc(@Param("group") Integer group);

    @Transactional(readOnly = true)
    @Query("select l from GalertLog l where l.def.group = :group and l.fixed=false and l.timestamp >= :begin and l.timestamp <= :end")
    List<GalertLog> findUnfixedByGroupAndTimestampBetween(@Param("group") Integer group,
                                                          @Param("begin") long begin,
                                                          @Param("end") long end);

    @Transactional(readOnly = true)
    @Query("select count(l) from GalertLog l where l.def.group = :group")
    Long countByGroup(@Param("group") Integer group);

}
