package org.hyperic.hq.measurement.data;

import java.util.List;

import javax.persistence.QueryHint;

import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface MeasurementTemplateRepository extends JpaRepository<MeasurementTemplate, Integer>,
    MeasurementTemplateCustom {

    @Query("select t from MeasurementTemplate t where t.id in (:ids)")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "MeasurementTemplate.findTemplates") })
    List<MeasurementTemplate> findByIds(@Param("ids") List<Integer> ids);

    @Query("select t from MeasurementTemplate t " + "join fetch t.monitorableType mt "
           + "where mt.name=:type order by t.name")
    List<MeasurementTemplate> findByMonitorableTypeOrderByName(@Param("type") String type);

    @Query("select t from MeasurementTemplate t " + "where t.monitorableType.name=:type "
           + "and t.category.name=:category " + "order by t.name")
    List<MeasurementTemplate> findByMonitorableTypeAndCategoryOrderByName(@Param("type") String type,
                                                                          @Param("category") String category);

    @Query("select t from MeasurementTemplate t " + "join fetch t.monitorableType mt "
           + "where mt.name=:type " + "and t.defaultOn = true")
    List<MeasurementTemplate> findByMonitorableTypeDefaultOn(@Param("type") String type);

    List<MeasurementTemplate> findByMonitorableType(MonitorableType monitorableType);
}
