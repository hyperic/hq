package org.hyperic.hq.measurement.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.Category;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import edu.emory.mathcs.backport.java.util.Collections;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/measurement/data/jpa-integration-test-context.xml" })
public class MeasurementRepositoryIntegrationTest {

    @Autowired
    private MeasurementRepository measurementRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private MeasurementTemplate template;

    private Resource resource;

    private MonitorableType type;

    private Category category;

    @Before
    public void setUp() {
        type = new MonitorableType("Tomcat Server", "tomcat");
        entityManager.persist(type);
        category = new Category("Availability");
        entityManager.persist(category);
        template = new MeasurementTemplate("Queue Size", "queueSize", "messages",
            MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true, "service:queueSize", type,
            category, "tomcat");
        entityManager.persist(template);
        resource = new Resource();
        resource.setName("Resource1");
        entityManager.persist(resource);
    }

    @Test
    public void testFindAllEnabledMeasurementsAndTemplates() {
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        measurementRepository.save(measurement);
        List<Object[]> measAndTemplates = measurementRepository
            .findAllEnabledMeasurementsAndTemplates();
        assertEquals(1, measAndTemplates.size());
        Object[] measAndTemplate = measAndTemplates.get(0);
        assertEquals(measurement, measAndTemplate[0]);
        assertEquals(template, measAndTemplate[1]);
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
    }

    @Test
    public void testFindByTemplateAndResourceNotFound() {
        assertNull(measurementRepository.findByTemplateAndResource(template.getId(),
            resource.getId()));
    }

    @SuppressWarnings("unchecked")
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
    }

    @SuppressWarnings("unchecked")
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
        measurementRepository.findIdsByTemplateAndResources(template.getId(),
            Collections.singletonList(resource2.getId()));
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
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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
    }

}
