package org.hyperic.hq.measurement.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.Category;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.Assert.assertEquals;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/measurement/data/jpa-integration-test-context.xml" })
public class MeasurementTemplateRepositoryIntegrationTest {

    @Autowired
    private MeasurementTemplateRepository measurementTemplateRepository;

    @PersistenceContext
    private EntityManager entityManager;
    
    private MonitorableType type;
    
    private Category category;
    
    @Before
    public void setUp() {
        type = new MonitorableType("Tomcat Server", "tomcat");
        entityManager.persist(type);
        category = new Category("Availability");
        entityManager.persist(category);
    }

    @Test
    public void testFindTemplates() {
        MeasurementTemplate mt1 = new MeasurementTemplate("Queue Size", "queueSize", "messages",
            MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true, "service:queueSize", type,
            category, "tomcat");
        measurementTemplateRepository.save(mt1);
        MeasurementTemplate mt2 = new MeasurementTemplate("Queue Failures", "queueFailures",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueFailures", type, category, "tomcat");
        measurementTemplateRepository.save(mt2);
        List<MeasurementTemplate> expected = new ArrayList<MeasurementTemplate>();
        expected.add(mt1);
        assertEquals(expected,
            measurementTemplateRepository.findTemplates(Arrays.asList(new Integer[] { mt1.getId() })));
    }
    
    @Test
    public void testFindTemplatesByMonitorableType() {
        //TODO create templates
        PageRequest pageRequest = new PageRequest(2, 1, new Sort("name"));
        measurementTemplateRepository.findTemplatesByMonitorableType("Tomcat Server", pageRequest);
    }
}
