package org.hyperic.hq.measurement.data;

import java.util.List;

import javax.persistence.QueryHint;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MeasurementRepository extends JpaRepository<Measurement, Integer>,
    MeasurementRepositoryCustom {

    @Transactional(readOnly = true)
    @Query("select m from Measurement m join m.template t where m.resource is not null and m.resource.id in (:ids) and  upper(t.alias) = '"
           + "AVAILABILITY' ")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "Measurement.findAvailMeasurementsByInstances") })
    List<Measurement> findAvailabilityMeasurementsByResources(@Param("ids") List<Integer> resourceIds);

    @Transactional(readOnly = true)
    @Query("select distinct m from Measurement m " + "join m.template t "
           + "join t.monitorableType mt " + "join t.category c " + "where m.enabled = true "
           + "and c.name = :category")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "Measurement.findByCategory") })
    List<Measurement> findByCategory(@Param("category") String category);

    @Transactional(readOnly = true)
    @Query("select m from Measurement m where m.resource = :resource")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "Measurement.findByResource") })
    List<Measurement> findByResource(@Param("resource") Resource resource);

    @Transactional(readOnly = true)
    @Query("select m from Measurement m " + "join m.template t " + "join t.category c "
           + "where m.resource = :resource and m.enabled = true and c.name = :category "
           + "order by t.name")
    List<Measurement> findByResourceAndCategoryOrderByTemplate(@Param("resource") Resource resource,
                                                               @Param("category") String category);

    List<Measurement> findByTemplate(Integer templateId);

    List<Measurement> findByTemplate(MeasurementTemplate template);

    @Transactional(readOnly = true)
    @Query("select distinct m from Measurement m " + "join m.template t "
           + "where t.id=:template and m.resource.id=:resource")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "Measurement.findByTemplateForInstance") })
    Measurement findByTemplateAndResource(@Param("template") Integer templateId,
                                          @Param("resource") Integer resourceId);

    @Transactional(readOnly = true)
    @Query("select m from Measurement m " + "join m.template t "
           + "where t.id in (:ids) and m.resource = :resource")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "Measurement.findByTemplateForInstance") })
    List<Measurement> findByTemplatesAndResource(@Param("ids") List<Integer> templateIds,
                                                 @Param("resource") Resource resource);

    @Transactional(readOnly = true)
    @Query("select m from Measurement m " + "join m.template t "
           + "where m.resource = :resource and " + "t.designate = true " + "order by t.name")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "Measurement.findDesignatedByResource") })
    List<Measurement> findDesignatedByResourceOrderByTemplate(@Param("resource") Resource resource);

    @Transactional(readOnly = true)
    @Query("select m from Measurement m " + "join m.template t " + "where m.enabled = true and "
           + "m.resource = :resource " + "order by t.name")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "Measurement.findEnabledByResource") })
    List<Measurement> findEnabledByResourceOrderByTemplate(@Param("resource") Resource resource);

    /**
     * Used primarily for preloaded 2nd level cache measurement objects
     * @return List<Object[]> [0] = Measurement [1] = MeasurementTemplate
     */
    @Transactional(readOnly = true)
    @Query("from Measurement m join m.template t left outer join fetch m.baselinesBag b where m.enabled = true")
    List<Object[]> findEnabledMeasurementsAndTemplates();

    @Transactional(readOnly = true)
    @Query("select m.id FROM Measurement m WHERE m.resource IS NULL")
    List<Integer> findIdsByResourceNull();

    @Transactional(readOnly = true)
    @Query("select m.id from Measurement m where m.template.id = :templateId and m.resource.id in (:resourceIds)")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "Measurement.findIdsByTemplateForInstances") })
    List<Integer> findIdsByTemplateAndResources(@Param("templateId") Integer templateId,
                                                @Param("resourceIds") List<Integer> resourceIds);

    @Transactional(readOnly = true)
    @Query("select distinct m.resource from Measurement m where m.template.id = :templateId")
    List<Resource> findMeasurementResourcesByTemplate(@Param("templateId") Integer templateId);

}
