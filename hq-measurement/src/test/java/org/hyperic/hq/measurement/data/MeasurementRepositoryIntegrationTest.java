package org.hyperic.hq.measurement.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import net.sf.ehcache.CacheManager;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.Category;
import org.hyperic.hq.measurement.server.session.CollectionSummary;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/measurement/data/jpa-integration-test-context.xml" })
public class MeasurementRepositoryIntegrationTest {

    private Category category;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private MeasurementRepository measurementRepository;

    private Resource resource;

    private MeasurementTemplate template;

    private MonitorableType type;

    @Before
    public void setUp() {
        type = new MonitorableType("Tomcat Server", "tomcat");
        entityManager.persist(type);
        category = new Category("Availability");
        entityManager.persist(category);
        template = new MeasurementTemplate("Queue Size", "Availability", "messages",
            MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true, "service:queueSize", type,
            category, "tomcat");
        entityManager.persist(template);
        resource = new Resource();
        resource.setName("Resource1");
        entityManager.persist(resource);
    }

    @After
    public void tearDown() {
        CacheManager.getInstance().clearAll();
    }

    @Test
    public void testFindAllEnabledMeasurementsAndTemplates() {
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        List<Object[]> measAndTemplates = measurementRepository
            .findEnabledMeasurementsAndTemplates();
        assertEquals(1, measAndTemplates.size());
        Object[] measAndTemplate = measAndTemplates.get(0);
        assertEquals(measurement, measAndTemplate[0]);
        assertEquals(template, measAndTemplate[1]);
    }

    @Test
    public void testFindAvailabilityMeasurementByResource() {
        Category category2 = new Category("Performance");
        entityManager.persist(category2);
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category2, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        assertEquals(measurement,
            measurementRepository.findAvailabilityMeasurementByResource(resource));
    }

    @Test
    public void testFindAvailabilityMeasurementsByResourceIds() {
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Category category2 = new Category("Performance");
        entityManager.persist(category2);
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category2, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Measurement measurement3 = new Measurement(resource2, template, 1234);
        measurement3.setDsn("throughput");
        measurement3.setEnabled(false);
        measurementRepository.save(measurement3);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement);
        expected.add(measurement3);
        assertEquals(expected, measurementRepository.findAvailabilityMeasurementsByResources(Arrays
            .asList(new Integer[] { resource.getId(), resource2.getId() })));
        verifyQueryCaching("Measurement.findAvailMeasurementsByInstances");
    }

    @Test
    public void testFindAvailabilityMeasurementsByResources() {
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Category category2 = new Category("Performance");
        entityManager.persist(category2);
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category2, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Measurement measurement3 = new Measurement(resource2, template, 1234);
        measurement3.setDsn("throughput");
        measurement3.setEnabled(false);
        measurementRepository.save(measurement3);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement);
        expected.add(measurement3);
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(resource);
        resources.add(resource2);
        assertEquals(expected,
            measurementRepository.findAvailabilityMeasurementsByResources(resources));
    }

    @Test
    public void testFindAvailabilityMeasurementsGroup() {
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        group.addMember(resource);
        group.addMember(resource2);
        entityManager.persist(group);
        Category category2 = new Category("Performance");
        entityManager.persist(category2);
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category2, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Measurement measurement3 = new Measurement(resource2, template, 1234);
        measurement3.setDsn("throughput");
        measurement3.setEnabled(false);
        measurementRepository.save(measurement3);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement);
        expected.add(measurement3);
        assertEquals(expected, measurementRepository.findAvailabilityMeasurementsByGroup(group));
        verifyQueryCaching("Measurement.findAvailMeasurementsForGroup");
    }

    @Test
    public void testFindByCategory() {
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Category category2 = new Category("Performance");
        entityManager.persist(category2);
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category2, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Measurement measurement3 = new Measurement(resource2, template, 1234);
        measurement3.setDsn("throughput");
        measurement3.setEnabled(false);
        measurementRepository.save(measurement3);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement);
        assertEquals(expected, measurementRepository.findByCategory(category.getName()));
        verifyQueryCaching("Measurement.findByCategory");
    }

    @Test
    public void testFindByResource() {
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Measurement measurement2 = new Measurement(resource2, template, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement);
        assertEquals(expected, measurementRepository.findByResource(resource));
        verifyQueryCaching("Measurement.findByResource");
    }

    @Test
    public void testFindByResourceAndCategory() {
        Category category2 = new Category("Performance");
        entityManager.persist(category2);
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category2, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement2);
        assertEquals(expected,
            measurementRepository.findByResourceAndCategoryOrderByTemplate(resource, "Performance"));
    }

    @Test
    public void testFindByResourceNull() {
        Category category2 = new Category("Performance");
        entityManager.persist(category2);
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Rate", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category2, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(null, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        List<Integer> expected = new ArrayList<Integer>();
        expected.add(measurement2.getId());
        assertEquals(expected, measurementRepository.findIdsByResourceNull());
    }

    @Test
    public void testFindByResourceNullNoNull() {
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Category category2 = new Category("Performance");
        entityManager.persist(category2);
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Rate", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category2, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        assertTrue(measurementRepository.findIdsByResourceNull().isEmpty());
    }

    @Test
    public void testFindByResources() {
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Measurement measurement2 = new Measurement(resource2, template, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement);
        expected.add(measurement2);
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(resource);
        resources.add(resource2);
        assertEquals(expected, measurementRepository.findByResources(resources));
    }

    @Test
    public void testFindByTemplate() {
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement);
        assertEquals(expected, measurementRepository.findByTemplate(template));
    }

    @Test
    public void testFindByTemplateAndResource() {
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource2, template, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        assertEquals(measurement2,
            measurementRepository.findByTemplateAndResource(template.getId(), resource2.getId()));
        verifyQueryCaching("Measurement.findByTemplateForInstance");
    }

    @Test
    public void testFindByTemplateAndResourceNotFound() {
        assertNull(measurementRepository.findByTemplateAndResource(template.getId(),
            resource.getId()));
    }

    @Test
    public void testFindByTemplateId() {
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement);
        assertEquals(expected, measurementRepository.findByTemplate(template.getId()));
    }

    @Test
    public void testFindByTemplatesAndResource() {
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource2, template, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement2);
        assertEquals(
            expected,
            measurementRepository.findByTemplatesAndResource(
                Collections.singletonList(template.getId()), resource2));
        verifyQueryCaching("Measurement.findByTemplateForInstance");
    }

    @Test
    public void testFindByTemplatesAndResourcesAll() {
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category, "tomcat");
        entityManager.persist(template2);
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource2, template, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Measurement measurement3 = new Measurement(resource2, template2, 1234);
        measurement3.setDsn("throughput");
        measurement3.setEnabled(false);
        measurementRepository.save(measurement3);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement2);
        expected.add(measurement3);
        assertEquals(expected, measurementRepository.findByTemplatesAndResources(
            new Integer[] { template.getId(), template2.getId() },
            new Integer[] { resource2.getId() }, false));
        verifyQueryCaching("Measurement.findMeasurements");
    }

    @Test
    public void testFindByTemplatesAndResourcesOnlyEnabled() {
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category, "tomcat");
        entityManager.persist(template2);
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource2, template, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Measurement measurement3 = new Measurement(resource2, template2, 1234);
        measurement3.setDsn("throughput");
        measurement3.setEnabled(false);
        measurementRepository.save(measurement3);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement2);
        assertEquals(expected, measurementRepository.findByTemplatesAndResources(
            new Integer[] { template.getId(), template2.getId() },
            new Integer[] { resource2.getId() }, true));
        verifyQueryCaching("Measurement.findMeasurements");
    }

    @Test
    public void testFindDesignatedByGroupAndCategory() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        group.addMember(resource);
        entityManager.persist(group);
        Category category2 = new Category("Performance");
        entityManager.persist(category2);
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category2, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        MeasurementTemplate template3 = new MeasurementTemplate("Throughput", "throughput",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, false,
            "service:queueSize", type, category, "tomcat");
        entityManager.persist(template3);
        Measurement measurement3 = new Measurement(resource, template3, 1234);
        measurement3.setDsn("throughput");
        measurementRepository.save(measurement3);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement);
        assertEquals(
            expected,
            measurementRepository.findDesignatedByGroupAndCategoryOrderByTemplate(group,
                category.getName()));
        verifyQueryCaching("Measurement.findDesignatedByCategoryForGroup");
    }

    @Test
    public void testFindDesignatedByResource() {
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, false,
            "service:queueSize", type, category, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement);
        assertEquals(expected,
            measurementRepository.findDesignatedByResourceOrderByTemplate(resource));
        verifyQueryCaching("Measurement.findDesignatedByResource");
    }

    @Test
    public void testFindDesignatedByResourceAndCategory() {
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, false,
            "service:queueSize", type, category, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement);
        assertEquals(expected,
            measurementRepository.findDesignatedByResourceAndCategory(resource, "Availability"));
    }

    @Test
    public void testFindDesignatedByResourcesAndCategory() {
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, false,
            "service:queueSize", type, category, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement);
        assertEquals(
            expected,
            measurementRepository.findDesignatedByResourcesAndCategory(
                Collections.singletonList(resource), "Availability"));
    }

    @Test
    public void testFindEnabledByResourceGroupAndTemplate() {
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        group.addMember(resource);
        group.addMember(resource2);
        entityManager.persist(group);
        Category category2 = new Category("Performance");
        entityManager.persist(category2);
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category2, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Measurement measurement3 = new Measurement(resource2, template, 1234);
        measurement3.setDsn("throughput");
        measurement3.setEnabled(false);
        measurementRepository.save(measurement3);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement);
        assertEquals(expected,
            measurementRepository.findEnabledByResourceGroupAndTemplate(group, template.getId()));
        verifyQueryCaching("ResourceGroup.getMetricsCollecting");
    }

    @Test
    public void testFindEnabledByResourceOrderByTemplate() {
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurement2.setEnabled(false);
        measurementRepository.save(measurement2);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement);
        assertEquals(expected, measurementRepository.findEnabledByResourceOrderByTemplate(resource));
        verifyQueryCaching("Measurement.findEnabledByResource");
    }

    @Test
    public void testFindEnabledByResources() {
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Measurement measurement2 = new Measurement(resource2, template, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Map<Integer, List<Measurement>> expected = new HashMap<Integer, List<Measurement>>();
        expected.put(resource.getId(), Collections.singletonList(measurement));
        expected.put(resource2.getId(), Collections.singletonList(measurement2));
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(resource);
        resources.add(resource2);
        assertEquals(expected, measurementRepository.findEnabledByResources(resources));
    }

    @Test
    public void testFindIdsByTemplatesAndResources() {
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource2, template, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        List<Integer> expected = new ArrayList<Integer>();
        expected.add(measurement2.getId());
        assertEquals(
            expected,
            measurementRepository.findIdsByTemplateAndResources(template.getId(),
                Collections.singletonList(resource2.getId())));
        verifyQueryCaching("Measurement.findIdsByTemplateForInstances");
    }

    @Test
    public void testFindMeasurementResourcesByTemplate() {
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Measurement measurement2 = new Measurement(resource2, template, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        List<Resource> expected = new ArrayList<Resource>();
        expected.add(resource);
        expected.add(resource2);
        assertEquals(expected,
            measurementRepository.findMeasurementResourcesByTemplate(template.getId()));
    }

    @Test
    public void testFindMetricCountSummaries() {
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Category category2 = new Category("Performance");
        entityManager.persist(category2);
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Rate", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category2, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Measurement measurement3 = new Measurement(resource2, template, 1234);
        measurement3.setDsn("throughput");
        // measurement3.setEnabled(false);
        measurementRepository.save(measurement3);
        measurementRepository.flush();
        List<CollectionSummary> summaries = measurementRepository.findMetricCountSummaries();
        assertEquals(2, summaries.size());
        assertTrue(EqualsBuilder.reflectionEquals(new CollectionSummary(2, 1234 / 60000,
            "Queue Size", "Tomcat Server"), summaries.get(0)));
        assertTrue(EqualsBuilder.reflectionEquals(new CollectionSummary(1, 1234 / 60000,
            "Queue Rate", "Tomcat Server"), summaries.get(1)));
    }

    @Test
    public void testFindMetricCountSummariesSomeDisabled() {
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Category category2 = new Category("Performance");
        entityManager.persist(category2);
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Rate", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category2, "tomcat");
        entityManager.persist(template2);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Measurement measurement3 = new Measurement(resource2, template, 1234);
        measurement3.setDsn("throughput");
        measurement3.setEnabled(false);
        measurementRepository.save(measurement3);
        measurementRepository.flush();
        List<CollectionSummary> summaries = measurementRepository.findMetricCountSummaries();
        assertEquals(2, summaries.size());
        assertTrue(EqualsBuilder.reflectionEquals(new CollectionSummary(1, 1234 / 60000,
            "Queue Size", "Tomcat Server"), summaries.get(0)));
        assertTrue(EqualsBuilder.reflectionEquals(new CollectionSummary(1, 1234 / 60000,
            "Queue Rate", "Tomcat Server"), summaries.get(1)));
    }

    @Test
    public void testFindRelatedAvailabilityMeasurements() {
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        Resource resource3 = new Resource();
        resource3.setName("Resource3");
        entityManager.persist(resource3);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource2, template, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Measurement measurement3 = new Measurement(resource3, template, 1234);
        measurement3.setDsn("throughput");
        measurementRepository.save(measurement3);
        Map<Integer, List<Measurement>> expected = new HashMap<Integer, List<Measurement>>();
        expected.put(resource.getId(),
            Arrays.asList(new Measurement[] { measurement2, measurement3 }));
        Map<Integer, List<Integer>> parentToChildIds = new HashMap<Integer, List<Integer>>();
        parentToChildIds.put(resource.getId(),
            Arrays.asList(new Integer[] { resource2.getId(), resource3.getId() }));
        assertEquals(expected,
            measurementRepository.findRelatedAvailabilityMeasurements(parentToChildIds));
        verifyQueryCaching("Measurement.findRelatedAvailMeasurements");
    }

    private void verifyQueryCaching(String cacheName) {
        assertEquals(1, CacheManager.getInstance().getCache(cacheName).getSize());
    }

}
