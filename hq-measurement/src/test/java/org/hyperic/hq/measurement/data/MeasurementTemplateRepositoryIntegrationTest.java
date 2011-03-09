package org.hyperic.hq.measurement.data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.Category;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MonitorableMeasurementInfo;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.hyperic.hq.product.MeasurementInfo;
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
    public void testFindByIds() {
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
            measurementTemplateRepository.findByIds(Arrays.asList(new Integer[] { mt1.getId() })));
    }

    @Test
    public void testFindByMonitorableTypeOrderByName() {
        MeasurementTemplate mt1 = new MeasurementTemplate("Queue Size", "queueSize", "messages",
            MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true, "service:queueSize", type,
            category, "tomcat");
        measurementTemplateRepository.save(mt1);
        MeasurementTemplate mt2 = new MeasurementTemplate("Queue Failures", "queueFailures",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueFailures", type, category, "tomcat");
        measurementTemplateRepository.save(mt2);
        // PageRequest pageRequest = new PageRequest(1, 1, new Sort("name"));
        List<MeasurementTemplate> expected = new ArrayList<MeasurementTemplate>();
        expected.add(mt2);
        expected.add(mt1);
        assertEquals(expected,
            measurementTemplateRepository.findByMonitorableTypeOrderByName("Tomcat Server"));
    }

    @Test
    public void testFindByMonitorableTypeAndCategory() {
        MeasurementTemplate mt1 = new MeasurementTemplate("Queue Size", "queueSize", "messages",
            MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true, "service:queueSize", type,
            category, "tomcat");
        measurementTemplateRepository.save(mt1);
        Category performance = new Category("Performance");
        entityManager.persist(performance);
        MeasurementTemplate mt2 = new MeasurementTemplate("Queue Failures", "queueFailures",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueFailures", type, performance, "tomcat");
        measurementTemplateRepository.save(mt2);
        MeasurementTemplate mt3 = new MeasurementTemplate("Message Throughput", "msgThroughput",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueFailures", type, performance, "tomcat");
        measurementTemplateRepository.save(mt3);
        List<MeasurementTemplate> expected = new ArrayList<MeasurementTemplate>();
        expected.add(mt3);
        expected.add(mt2);
        assertEquals(expected,
            measurementTemplateRepository.findByMonitorableTypeAndCategoryOrderByName(
                "Tomcat Server", "Performance"));
    }

    @Test
    public void testFindByMonitorableTypeDefaultOn() {
        MeasurementTemplate mt1 = new MeasurementTemplate("Queue Size", "queueSize", "messages",
            MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true, "service:queueSize", type,
            category, "tomcat");
        measurementTemplateRepository.save(mt1);
        MonitorableType type2 = new MonitorableType("JBoss Server", "jboss");
        entityManager.persist(type2);
        MeasurementTemplate mt2 = new MeasurementTemplate("Queue Failures", "queueFailures",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueFailures", type2, category, "tomcat");
        measurementTemplateRepository.save(mt2);
        MeasurementTemplate mt3 = new MeasurementTemplate("Message Throughput", "msgThroughput",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, false, 1234, true,
            "service:queueFailures", type2, category, "tomcat");
        measurementTemplateRepository.save(mt3);
        MeasurementTemplate mt4 = new MeasurementTemplate("Message Size", "messageSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueFailures", type2, category, "tomcat");
        measurementTemplateRepository.save(mt4);
        List<MeasurementTemplate> expected = new ArrayList<MeasurementTemplate>();
        expected.add(mt2);
        expected.add(mt4);
        assertEquals(expected,
            measurementTemplateRepository.findByMonitorableTypeDefaultOn("JBoss Server"));
    }

    @Test
    public void testFindByMonitorableType() {
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
        expected.add(mt2);
        assertEquals(expected, measurementTemplateRepository.findByMonitorableType(type));
    }
    
    @Test
    public void testCreateTemplates() {
        Map<MonitorableType,List<MonitorableMeasurementInfo>> infos = new HashMap<MonitorableType,List<MonitorableMeasurementInfo>>();
        List<MonitorableMeasurementInfo> measInfos = new ArrayList<MonitorableMeasurementInfo>();
        MeasurementInfo measInfo = new MeasurementInfo();
        measInfo.setAlias("queueSize");
        measInfo.setCategory(category.getName());
        measInfo.setCollectionType(MeasurementConstants.COLL_TYPE_DYNAMIC);
        measInfo.setDefaultOn(true);
        measInfo.setInterval(1234);
        measInfo.setName("Queue Size");
        measInfo.setUnits("messages");
        measInfo.setIndicator(true);
        measInfo.setTemplate("service:queueFailures");
        MonitorableMeasurementInfo info = new MonitorableMeasurementInfo(type, measInfo);
        measInfos.add(info);
        infos.put(type, measInfos);
        measurementTemplateRepository.createTemplates("tomcat", infos);
        entityManager.clear();
        List<MeasurementTemplate> templates = measurementTemplateRepository.findAll();
        assertEquals(1,templates.size());
    }
}
