package org.hyperic.hq.measurement.data;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import net.sf.ehcache.CacheManager;

import org.apache.commons.lang.builder.EqualsBuilder;
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

    private int resource = 9342;

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
        int resource2 = 43;
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
        Set<Measurement> expected = new HashSet<Measurement>();
        expected.add(measurement);
        expected.add(measurement3);
        assertEquals(expected, new HashSet<Measurement>(measurementRepository.findAvailabilityMeasurementsByResources(Arrays
            .asList(new Integer[] { resource, resource2 }))));
        verifyQueryCaching("Measurement.findAvailMeasurementsByInstances");
    }
    
    @Test
    public void testFindAvailabilityMeasurementsGroupMembers() {
        int resource2 = 43;
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
        Set<Measurement> expected = new HashSet<Measurement>();
        expected.add(measurement);
        expected.add(measurement3);
        Set<Integer> groupMembers = new HashSet<Integer>();
        groupMembers.add(resource);
        groupMembers.add(resource2);
        assertEquals(expected,
            new HashSet<Measurement>(measurementRepository.findAvailabilityMeasurementsByGroupMembers(groupMembers)));
        verifyQueryCaching("Measurement.findAvailMeasurementsForGroup");
    }
    
    @Test
    public void testFindAvailabilityMeasurementsGroupMembersNoMembers() {
        int resource2 = 43;
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
        Set<Measurement> expected = new HashSet<Measurement>();
        Set<Integer> groupMembers = new HashSet<Integer>();
        assertEquals(expected,
            new HashSet<Measurement>(measurementRepository.findAvailabilityMeasurementsByGroupMembers(groupMembers)));
    }

    @Test
    public void testFindByCategory() {
        int resource2 = 43;
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
        int resource2 = 43;
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
        int resource2 = 43;
        Measurement measurement2 = new Measurement(resource2, template, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Set<Measurement> expected = new HashSet<Measurement>();
        expected.add(measurement);
        expected.add(measurement2);
        List<Integer> resources = new ArrayList<Integer>();
        resources.add(resource);
        resources.add(resource2);
        assertEquals(expected, new HashSet<Measurement>(measurementRepository.findByResources(resources)));
    }
    
    @Test
    public void testFindByResourcesEmpty() {
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        int resource2 = 43;
        Measurement measurement2 = new Measurement(resource2, template, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Set<Measurement> expected = new HashSet<Measurement>();
        List<Integer> resources = new ArrayList<Integer>();
        assertEquals(expected, new HashSet<Measurement>(measurementRepository.findByResources(resources)));
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
        int resource2 = 43;
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource2, template, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        assertEquals(measurement2,
            measurementRepository.findByTemplateAndResource(template.getId(), resource2));
        verifyQueryCaching("Measurement.findByTemplateForInstance");
    }

    @Test
    public void testFindByTemplateAndResourceNotFound() {
        assertNull(measurementRepository.findByTemplateAndResource(template.getId(), resource));
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
        int resource2 = 43;
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
        int resource2 = 43;
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
        Set<Measurement> expected = new HashSet<Measurement>();
        expected.add(measurement2);
        expected.add(measurement3);
        assertEquals(expected, new HashSet<Measurement>(measurementRepository.findByTemplatesAndResources(
            new Integer[] { template.getId(), template2.getId() }, new Integer[] { resource2 },
            false)));
        verifyQueryCaching("Measurement.findMeasurements");
    }
    
    @Test
    public void testFindByTemplatesAndResourcesEmptyTemplates() {
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category, "tomcat");
        entityManager.persist(template2);
        int resource2 = 43;
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
        Set<Measurement> expected = new HashSet<Measurement>();
        assertEquals(expected, new HashSet<Measurement>(measurementRepository.findByTemplatesAndResources(
            new Integer[0], new Integer[] { resource2 },
            false)));
    }
    
    @Test
    public void testFindByTemplatesAndResourcesEmptyResources() {
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category, "tomcat");
        entityManager.persist(template2);
        int resource2 = 43;
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
        Set<Measurement> expected = new HashSet<Measurement>();
        assertEquals(expected, new HashSet<Measurement>(measurementRepository.findByTemplatesAndResources(
            new Integer[] { template.getId(), template2.getId() }, new Integer[0],
            false)));
    }

    @Test
    public void testFindByTemplatesAndResourcesOnlyEnabled() {
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category, "tomcat");
        entityManager.persist(template2);
        int resource2 = 43;
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
            new Integer[] { template.getId(), template2.getId() }, new Integer[] { resource2 },
            true));
        verifyQueryCaching("Measurement.findMeasurements");
    }

    @Test
    public void testFindDesignatedByGroupAndCategory() {
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
        assertEquals(expected,
            measurementRepository.findDesignatedByGroupAndCategoryOrderByTemplate(
                new HashSet<Integer>(Collections.singletonList(resource)), category.getName()));
        verifyQueryCaching("Measurement.findDesignatedByCategoryForGroup");
    }
    
    @Test
    public void testFindDesignatedByGroupAndCategoryEmpty() {
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
        assertEquals(expected,
            measurementRepository.findDesignatedByGroupAndCategoryOrderByTemplate(
                new HashSet<Integer>(0), category.getName()));
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
        int resource2 = 43;
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
        Set<Integer> groupMembers = new HashSet<Integer>();
        groupMembers.add(resource);
        groupMembers.add(resource2);
        List<Measurement> expected = new ArrayList<Measurement>();
        expected.add(measurement);
        assertEquals(
            expected,
            measurementRepository.findEnabledByResourceGroupAndTemplate(groupMembers,
                template.getId()));
        verifyQueryCaching("ResourceGroup.getMetricsCollecting");
    }
    
    @Test
    public void testFindEnabledByResourceGroupAndTemplateNoGroupMembers() {
        int resource2 = 43;
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
        Set<Integer> groupMembers = new HashSet<Integer>();
        List<Measurement> expected = new ArrayList<Measurement>();
        assertEquals(
            expected,
            measurementRepository.findEnabledByResourceGroupAndTemplate(groupMembers,
                template.getId()));
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
        int resource2 = 43;
        Measurement measurement2 = new Measurement(resource2, template, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Map<Integer, List<Measurement>> expected = new HashMap<Integer, List<Measurement>>();
        expected.put(resource, Collections.singletonList(measurement));
        expected.put(resource2, Collections.singletonList(measurement2));
        List<Integer> resources = new ArrayList<Integer>();
        resources.add(resource);
        resources.add(resource2);
        assertEquals(expected, measurementRepository.findEnabledByResources(resources));
    }

    @Test
    public void testFindIdsByTemplatesAndResources() {
        int resource2 = 43;
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
                Collections.singletonList(resource2)));
        verifyQueryCaching("Measurement.findIdsByTemplateForInstances");
    }

    @Test
    public void testFindMeasurementResourcesByTemplate() {
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        int resource2 = 43;
        Measurement measurement2 = new Measurement(resource2, template, 1234);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Set<Integer> expected = new HashSet<Integer>();
        expected.add(resource);
        expected.add(resource2);
        assertEquals(expected,
            new HashSet<Integer>(measurementRepository.findMeasurementResourcesByTemplate(template.getId())));
    }

    @Test
    public void testFindMetricCountSummaries() {
        int resource2 = 43;
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
        int resource2 = 43;
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
        int resource2 = 43;
        int resource3 = 99;
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
        expected.put(resource, Arrays.asList(new Measurement[] { measurement2, measurement3 }));
        Map<Integer, List<Integer>> parentToChildIds = new HashMap<Integer, List<Integer>>();
        parentToChildIds.put(resource, Arrays.asList(new Integer[] { resource2, resource3 }));
        assertEquals(expected,
            measurementRepository.findRelatedAvailabilityMeasurements(parentToChildIds));
        verifyQueryCaching("Measurement.findRelatedAvailMeasurements");
    }

    @Test
    public void testGetMinIntervals() {
        Category category2 = new Category("Performance");
        entityManager.persist(category2);
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Rate", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category2, "tomcat");
        entityManager.persist(template2);
        int resource2 = 43;
        Measurement measurement = new Measurement(resource, template, 10);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        Measurement measurement2 = new Measurement(resource2, template, 20);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Measurement measurement3 = new Measurement(resource2, template2, 30);
        measurement3.setDsn("throughput");
        measurementRepository.save(measurement3);
        List<Object[]> actual = measurementRepository.getMinIntervals();
        assertEquals(2, actual.size());
        assertArrayEquals(new Object[] { resource, 10l }, actual.get(0));
        assertArrayEquals(new Object[] { resource2, 20l }, actual.get(1));
    }

    @Test
    public void testGetMinInterval() {
        Category category2 = new Category("Performance");
        entityManager.persist(category2);
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Rate", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category2, "tomcat");
        entityManager.persist(template2);
        int resource2 = 43;
        Measurement measurement2 = new Measurement(resource2, template, 20);
        measurement2.setDsn("queueErrors");
        measurementRepository.save(measurement2);
        Measurement measurement3 = new Measurement(resource2, template2, 30);
        measurement3.setDsn("throughput");
        measurementRepository.save(measurement3);
        assertEquals(Long.valueOf(20), measurementRepository.getMinInterval(resource2));
    }

    private void verifyQueryCaching(String cacheName) {
        assertEquals(1, CacheManager.getInstance().getCache(cacheName).getSize());
    }

}
