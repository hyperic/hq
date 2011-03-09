package org.hyperic.hq.measurement.data;

import java.util.List;

import javax.persistence.QueryHint;

import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface MeasurementTemplateRepository extends JpaRepository<MeasurementTemplate, Integer> {

    @Query("select t from MeasurementTemplate t where t.id in (:ids)")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "MeasurementTemplate.findTemplates") })
    List<MeasurementTemplate> findTemplates(@Param("ids") List<Integer> ids);
    
    
    @Query("select t from MeasurementTemplate t " + "join fetch t.monitorableType mt "
                     + "where mt.name=:typeName")
    List<MeasurementTemplate> findTemplatesByMonitorableType(@Param("typeName")String type, Pageable pageable);
}
