package org.hyperic.hq.measurement.data;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
        int resource = 765;
        measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        entityManager.persist(measurement);
        measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("queueErrors");
        entityManager.persist(measurement2);

    }

    @Test
    public void testFindAggregateAvailability() {
        long starttime = System.currentTimeMillis();
        AvailabilityDataRLE avail1 = new AvailabilityDataRLE(measurement, starttime + 1000,
            starttime + 5000, 1.0);
        availabilityDataRepository.save(avail1);
        AvailabilityDataRLE avail2 = new AvailabilityDataRLE(measurement, starttime,
            starttime + 10000, 1.0);
        availabilityDataRepository.save(avail2);
        AvailabilityDataRLE avail3 = new AvailabilityDataRLE(measurement2, starttime,
            starttime + 3000, 1.0);
        availabilityDataRepository.save(avail3);
        List<Object[]> actual = availabilityDataRepository.findAggregateAvailability(
            Arrays.asList(new Integer[] { measurement.getId(), measurement2.getId() }), starttime,
            starttime + 10001);
        assertEquals(3, actual.size());
        assertArrayEquals(
            new Object[] { measurement2, 1.0, 1.0, 1.0, (10001l / 1234), 3000d, 3000d },
            actual.get(0));
        assertArrayEquals(
            new Object[] { measurement, 1.0, 1.0, 1.0, (10001l / 1234), 4000d, 4000d },
            actual.get(1));
        assertArrayEquals(new Object[] { measurement,
                                        1.0,
                                        1.0,
                                        1.0,
                                        (10001l / 1234),
                                        10000d,
                                        10000d }, actual.get(2));
    }

    @Test
    public void testFindByMeasurements() {
        long starttime = System.currentTimeMillis();
        AvailabilityDataRLE avail1 = new AvailabilityDataRLE(measurement, starttime, 1.0);
        availabilityDataRepository.save(avail1);
        AvailabilityDataRLE avail2 = new AvailabilityDataRLE(measurement, starttime - 1000, 1.0);
        availabilityDataRepository.save(avail2);
        AvailabilityDataRLE avail3 = new AvailabilityDataRLE(measurement2, starttime, 1.0);
        availabilityDataRepository.save(avail3);
        Set<AvailabilityDataRLE> expected = new HashSet<AvailabilityDataRLE>();
        expected.add(avail1);
        expected.add(avail2);
        expected.add(avail3);
        assertEquals(
            expected,
            new HashSet<AvailabilityDataRLE>(availabilityDataRepository
                .findLastByMeasurements(Arrays.asList(new Integer[] { measurement.getId(),
                                                                     measurement2.getId() }))));
    }

    @Test
    public void testFindLastByMeasurements() {
        long starttime = System.currentTimeMillis();
        AvailabilityDataRLE avail1 = new AvailabilityDataRLE(measurement, starttime + 1000, 1.0);
        availabilityDataRepository.save(avail1);
        AvailabilityDataRLE avail2 = new AvailabilityDataRLE(measurement, starttime,
            starttime + 10000, 1.0);
        availabilityDataRepository.save(avail2);
        AvailabilityDataRLE avail3 = new AvailabilityDataRLE(measurement2, starttime, 1.0);
        availabilityDataRepository.save(avail3);
        Set<AvailabilityDataRLE> expected = new HashSet<AvailabilityDataRLE>();
        expected.add(avail1);
        expected.add(avail3);
        assertEquals(
            expected,
            new HashSet<AvailabilityDataRLE>(availabilityDataRepository
                .findLastByMeasurements(Arrays.asList(new Integer[] { measurement.getId(),
                                                                     measurement2.getId() }))));
    }

    @Test
    public void testGetDownMeasurements() {
        long starttime = System.currentTimeMillis();
        AvailabilityDataRLE avail1 = new AvailabilityDataRLE(measurement, starttime + 1000,
            starttime + 6000, 0.0);
        availabilityDataRepository.save(avail1);
        AvailabilityDataRLE avail2 = new AvailabilityDataRLE(measurement, starttime, 0.0);
        availabilityDataRepository.save(avail2);
        AvailabilityDataRLE avail3 = new AvailabilityDataRLE(measurement2, starttime, 0.0);
        availabilityDataRepository.save(avail3);
        Set<AvailabilityDataRLE> expected = new HashSet<AvailabilityDataRLE>();
        expected.add(avail2);
        expected.add(avail3);
        assertEquals(
            expected,
            new HashSet<AvailabilityDataRLE>(availabilityDataRepository.getDownMeasurements((Arrays
                .asList(new Integer[] { measurement.getId(), measurement2.getId() })))));
    }

    @Test
    public void testGetDownMeasurementsIncludeAll() {
        long starttime = System.currentTimeMillis();
        AvailabilityDataRLE avail1 = new AvailabilityDataRLE(measurement, starttime + 1000,
            starttime + 6000, 0.0);
        availabilityDataRepository.save(avail1);
        AvailabilityDataRLE avail2 = new AvailabilityDataRLE(measurement, starttime, 0.0);
        availabilityDataRepository.save(avail2);
        AvailabilityDataRLE avail3 = new AvailabilityDataRLE(measurement2, starttime, 0.0);
        availabilityDataRepository.save(avail3);
        Set<AvailabilityDataRLE> expected = new HashSet<AvailabilityDataRLE>();
        expected.add(avail2);
        expected.add(avail3);
        assertEquals(expected,
            new HashSet<AvailabilityDataRLE>(availabilityDataRepository.getDownMeasurements(null)));
    }

    @Test
    public void testGetHistoricalAvailMap() {
        long starttime = System.currentTimeMillis();
        AvailabilityDataRLE avail1 = new AvailabilityDataRLE(measurement, starttime + 1000,
            starttime + 5000, 1.0);
        availabilityDataRepository.save(avail1);
        AvailabilityDataRLE avail2 = new AvailabilityDataRLE(measurement, starttime,
            starttime + 7000, 1.0);
        availabilityDataRepository.save(avail2);
        AvailabilityDataRLE avail3 = new AvailabilityDataRLE(measurement, starttime - 1000,
            starttime + 3000, 1.0);
        availabilityDataRepository.save(avail3);
        Set<AvailabilityDataRLE> expected = new TreeSet<AvailabilityDataRLE>(
            new Comparator<AvailabilityDataRLE>() {
                public int compare(AvailabilityDataRLE lhs, AvailabilityDataRLE rhs) {
                    Long lhsStart = new Long(lhs.getStartime());
                    Long rhsStart = new Long(rhs.getStartime());
                    return rhsStart.compareTo(lhsStart);
                }
            });
        expected.add(avail1);
        expected.add(avail2);
        Map<Integer, TreeSet<AvailabilityDataRLE>> actual = availabilityDataRepository
            .getHistoricalAvailMap(Collections.singletonList(measurement.getId()),
                starttime + 4000, true);
        assertEquals(1, actual.size());
        TreeSet<AvailabilityDataRLE> rles = actual.get(measurement.getId());
        assertEquals(expected, rles);
    }

    @Test
    public void testGetHistoricalAvailsByMeasurement() {
        long starttime = System.currentTimeMillis();
        AvailabilityDataRLE avail1 = new AvailabilityDataRLE(measurement, starttime + 1000,
            starttime + 5000, 1.0);
        availabilityDataRepository.save(avail1);
        AvailabilityDataRLE avail2 = new AvailabilityDataRLE(measurement, starttime + 9000,
            starttime + 12000, 1.0);
        availabilityDataRepository.save(avail2);
        AvailabilityDataRLE avail3 = new AvailabilityDataRLE(measurement, starttime - 1000,
            starttime + 2000, 1.0);
        availabilityDataRepository.save(avail3);
        List<AvailabilityDataRLE> expected = new ArrayList<AvailabilityDataRLE>();
        expected.add(avail1);
        assertEquals(expected, availabilityDataRepository.getHistoricalAvails(measurement,
            starttime + 4000, starttime + 8000, true));
    }

    @Test
    public void testGetHistoricalAvailsByMeasurementIds() {
        long starttime = System.currentTimeMillis();
        AvailabilityDataRLE avail1 = new AvailabilityDataRLE(measurement, starttime + 1000,
            starttime + 5000, 1.0);
        availabilityDataRepository.save(avail1);
        AvailabilityDataRLE avail2 = new AvailabilityDataRLE(measurement, starttime + 9000,
            starttime + 12000, 1.0);
        availabilityDataRepository.save(avail2);
        AvailabilityDataRLE avail3 = new AvailabilityDataRLE(measurement, starttime - 2000,
            starttime + 3000, 1.0);
        availabilityDataRepository.save(avail3);
        List<AvailabilityDataRLE> expected = new ArrayList<AvailabilityDataRLE>();
        expected.add(avail1);
        assertEquals(expected, availabilityDataRepository.getHistoricalAvails(
            Collections.singletonList(measurement.getId()), starttime + 4000, starttime + 8000,
            true));
    }

    @Test
    public void testGetHistoricalAvailsByResource() {
        long starttime = System.currentTimeMillis();
        AvailabilityDataRLE avail1 = new AvailabilityDataRLE(measurement, starttime + 1000,
            starttime + 5000, 1.0);
        availabilityDataRepository.save(avail1);
        AvailabilityDataRLE avail2 = new AvailabilityDataRLE(measurement, starttime,
            starttime + 10000, 1.0);
        availabilityDataRepository.save(avail2);
        AvailabilityDataRLE avail3 = new AvailabilityDataRLE(measurement2, starttime,
            starttime + 3000, 1.0);
        availabilityDataRepository.save(avail3);
        List<AvailabilityDataRLE> expected = new ArrayList<AvailabilityDataRLE>();
        expected.add(avail2);
        expected.add(avail1);
        assertEquals(expected, availabilityDataRepository.getHistoricalAvails(
            measurement.getResource(), starttime + 4000, starttime + 8000));
    }

}
