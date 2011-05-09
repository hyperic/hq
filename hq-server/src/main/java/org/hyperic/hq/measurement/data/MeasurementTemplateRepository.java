package org.hyperic.hq.measurement.data;

import java.util.List;

import javax.persistence.QueryHint;

import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MeasurementTemplateRepository extends JpaRepository<MeasurementTemplate, Integer>,
    MeasurementTemplateRepositoryCustom {

    @Transactional(readOnly = true)
    @Query("select t from MeasurementTemplate t where t.id in (:ids)")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "MeasurementTemplate.findTemplates") })
    List<MeasurementTemplate> findByIds(@Param("ids") List<Integer> ids);

    @Transactional(readOnly = true)
    @Query("select t from MeasurementTemplate t " + "join fetch t.monitorableType mt "
           + "where mt.name=:type order by t.name")
    List<MeasurementTemplate> findByMonitorableTypeOrderByName(@Param("type") String type);
    
    @Transactional(readOnly = true)
    @Query("select t from MeasurementTemplate t " + "join fetch t.monitorableType mt "
           + "where mt.name=:type")
    List<MeasurementTemplate> findByMonitorableType(@Param("type") String type, Sort sort);
    
    @Transactional(readOnly = true)
    @Query("select t from MeasurementTemplate t " + "join fetch t.monitorableType mt "
           + "where mt.name=:type")
    Page<MeasurementTemplate> findByMonitorableType(@Param("type") String type, Pageable pageable);

    List<MeasurementTemplate> findByMonitorableTypeNameAndCategoryNameOrderByNameAsc(String type,
                                                                                     String category);

    @Transactional(readOnly = true)
    @Query("select t from MeasurementTemplate t " + "join fetch t.monitorableType mt "
           + "where mt.name=:type " + "and t.defaultOn = :defaultOn")
    List<MeasurementTemplate> findByMonitorableTypeAndDefaultOn(@Param("type") String type,
                                                                @Param("defaultOn") boolean defaultOn);

    @Transactional(readOnly = true)
    @Query("select t from MeasurementTemplate t " + "join fetch t.monitorableType mt "
           + "where mt.name=:type " + "and t.defaultOn = :defaultOn")
    List<MeasurementTemplate> findByMonitorableTypeAndDefaultOn(@Param("type") String type,
                                                                @Param("defaultOn") boolean defaultOn,
                                                                Sort sort);
    
    @Transactional(readOnly = true)
    @Query("select t from MeasurementTemplate t " + "join fetch t.monitorableType mt "
           + "where mt.name=:type " + "and t.defaultOn = :defaultOn")
    Page<MeasurementTemplate> findByMonitorableTypeAndDefaultOn(@Param("type") String type,
                                                                @Param("defaultOn") boolean defaultOn,
                                                                Pageable pageable);

    List<MeasurementTemplate> findByMonitorableType(MonitorableType monitorableType);

    Page<MeasurementTemplate> findByDefaultOn(boolean defaultOn, Pageable pageable);

    List<MeasurementTemplate> findByDefaultOn(boolean defaultOn, Sort sort);
}
