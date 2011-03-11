package org.hyperic.hq.measurement.data;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.AvailabilityDataRLE;
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

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/measurement/data/jpa-integration-test-context.xml" })
public class AvailabilityDataRepositoryIntegrationTest {

    @Autowired
    private AvailabilityDataRepository availabilityDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Measurement measurement;

    private Measurement measurement2;

    @Before
    public void setUp() {
        MonitorableType type = new MonitorableType("Tomcat Server", "tomcat");
        entityManager.persist(type);
        Category category = new Category("Availability");
        entityManager.persist(category);
        MeasurementTemplate template = new MeasurementTemplate("Queue Size", "Availability",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category, "tomcat");
        entityManager.persist(template);
        MeasurementTemplate template2 = new MeasurementTemplate("Queue Errors", "Availability",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category, "tomcat");
        entityManager.persist(template2);
        Resource resource = new Resource();
        resource.setName("Resource1");
        entityManager.persist(resource);
        measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        entityManager.persist(measurement);
        measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        entityManager.persist(measurement2);

    }

    @Test
    public void testFindByMeasurements() {
        long starttime = System.currentTimeMillis();
        AvailabilityDataRLE avail1 = new AvailabilityDataRLE(measurement, starttime, 1.0);
        availabilityDataRepository.save(avail1);
        AvailabilityDataRLE avail2 = new AvailabilityDataRLE(measurement, starttime, 1.0);
        availabilityDataRepository.save(avail2);
        AvailabilityDataRLE avail3 = new AvailabilityDataRLE(measurement2, starttime, 1.0);
        availabilityDataRepository.save(avail3);
        Set<AvailabilityDataRLE> expected = new HashSet<AvailabilityDataRLE>();
        expected.add(avail1);
        expected.add(avail2);
        expected.add(avail3);
        assertEquals(
            expected,
            new HashSet<AvailabilityDataRLE>(availabilityDataRepository.findByMeasurements(Arrays
                .asList(new Integer[] { measurement.getId(), measurement2.getId() }))));
    }
}
