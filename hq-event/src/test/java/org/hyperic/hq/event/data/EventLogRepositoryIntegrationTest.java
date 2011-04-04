package org.hyperic.hq.event.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.events.server.session.EventLog;
import org.hyperic.hq.inventory.domain.Resource;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/event/data/jpa-integration-test-context.xml" })
public class EventLogRepositoryIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private EventLogRepository eventLogRepository;

    @Test
    public void testDeleteByResource() {
        Resource resource1 = new Resource();
        resource1.setName("Resource1");
        entityManager.persist(resource1);
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        long timestamp = System.currentTimeMillis();
        EventLog log1 = new EventLog(resource1, "Big Event", "AlertFiredEvent", "Some details",
            timestamp, "OK", resource1.getId());
        eventLogRepository.save(log1);
        EventLog log2 = new EventLog(resource1, "Another Event", "AlertFiredEvent", "More details",
            timestamp - 2000, "OK", resource1.getId());
        eventLogRepository.save(log2);
        EventLog log3 = new EventLog(resource2, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "OK", resource2.getId());
        eventLogRepository.save(log3);
        EventLog log4 = new EventLog(resource1, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "Not OK", resource1.getId());
        eventLogRepository.save(log4);
        EventLog log5 = new EventLog(resource1, "Huge Event", "PropertyChangedEvent",
            "More details", timestamp + 10000, "OK", resource1.getId());
        eventLogRepository.save(log5);
        eventLogRepository.deleteByResource(resource1);
        assertEquals(Long.valueOf(1), eventLogRepository.count());
    }

    @Test
    public void testDeleteLogsInTimeRange() {
        Resource resource1 = new Resource();
        resource1.setName("Resource1");
        entityManager.persist(resource1);
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        long timestamp = System.currentTimeMillis();
        EventLog log1 = new EventLog(resource1, "Big Event", "AlertFiredEvent", "Some details",
            timestamp, "OK", resource1.getId());
        eventLogRepository.save(log1);
        EventLog log2 = new EventLog(resource1, "Another Event", "AlertFiredEvent", "More details",
            timestamp - 2000, "OK", resource1.getId());
        eventLogRepository.save(log2);
        EventLog log3 = new EventLog(resource2, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "OK", resource2.getId());
        eventLogRepository.save(log3);
        EventLog log4 = new EventLog(resource1, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "Not OK", resource1.getId());
        eventLogRepository.save(log4);
        EventLog log5 = new EventLog(resource1, "Huge Event", "PropertyChangedEvent",
            "More details", timestamp + 10000, "OK", resource1.getId());
        eventLogRepository.save(log5);
        eventLogRepository.deleteLogsInTimeRange(timestamp, timestamp + 10000);
        assertEquals(Long.valueOf(1), eventLogRepository.count());
    }

    @Test
    public void testFindByTimestampBetweenAndResource() {
        Resource resource1 = new Resource();
        resource1.setName("Resource1");
        entityManager.persist(resource1);
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        long timestamp = System.currentTimeMillis();
        EventLog log1 = new EventLog(resource1, "Big Event", "AlertFiredEvent", "Some details",
            timestamp, "OK", resource1.getId());
        eventLogRepository.save(log1);
        EventLog log2 = new EventLog(resource1, "Another Event", "AlertFiredEvent", "More details",
            timestamp - 2000, "OK", resource1.getId());
        eventLogRepository.save(log2);
        EventLog log3 = new EventLog(resource2, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "OK", resource2.getId());
        eventLogRepository.save(log3);
        EventLog log4 = new EventLog(resource1, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "Not OK", resource1.getId());
        eventLogRepository.save(log4);
        EventLog log5 = new EventLog(resource1, "Huge Event", "PropertyChangedEvent",
            "More details", timestamp + 10000, "OK", resource1.getId());
        eventLogRepository.save(log5);
        List<EventLog> expected = new ArrayList<EventLog>();
        expected.add(log2);
        expected.add(log1);
        expected.add(log4);
        assertEquals(expected,
            eventLogRepository.findByTimestampBetweenAndResourceOrderByTimestampAsc(
                timestamp - 3000, timestamp + 3000, resource1));
    }

    @Test
    public void testFindByTimestampBetweenAndResourceAndEventTypes() {
        Resource resource1 = new Resource();
        resource1.setName("Resource1");
        entityManager.persist(resource1);
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        long timestamp = System.currentTimeMillis();
        EventLog log1 = new EventLog(resource1, "Big Event", "AlertFiredEvent", "Some details",
            timestamp, "OK", resource1.getId());
        eventLogRepository.save(log1);
        EventLog log2 = new EventLog(resource1, "Another Event", "AlertFiredEvent", "More details",
            timestamp - 2000, "OK", resource1.getId());
        eventLogRepository.save(log2);
        EventLog log3 = new EventLog(resource2, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "OK", resource2.getId());
        eventLogRepository.save(log3);
        EventLog log4 = new EventLog(resource1, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "Not OK", resource1.getId());
        eventLogRepository.save(log4);
        EventLog log5 = new EventLog(resource1, "Huge Event", "PropertyChangedEvent",
            "More details", timestamp + 10000, "OK", resource1.getId());
        eventLogRepository.save(log5);
        List<EventLog> expected = new ArrayList<EventLog>();
        expected.add(log2);
        expected.add(log1);
        assertEquals(expected,
            eventLogRepository.findByTimestampBetweenAndResourceAndEventTypesOrderByTimestamp(
                timestamp - 3000, timestamp + 1000, resource1,
                Arrays.asList(new String[] { "AlertFiredEvent" })));
    }

    @Test
    public void testFindByTimestampBetweenAndResources() {
        Resource resource1 = new Resource();
        resource1.setName("Resource1");
        entityManager.persist(resource1);
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        long timestamp = System.currentTimeMillis();
        EventLog log1 = new EventLog(resource1, "Big Event", "AlertFiredEvent", "Some details",
            timestamp, "OK", resource1.getId());
        eventLogRepository.save(log1);
        EventLog log2 = new EventLog(resource1, "Another Event", "AlertFiredEvent", "More details",
            timestamp - 2000, "OK", resource1.getId());
        eventLogRepository.save(log2);
        EventLog log3 = new EventLog(resource2, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "OK", resource2.getId());
        eventLogRepository.save(log3);
        EventLog log4 = new EventLog(resource1, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "Not OK", resource1.getId());
        eventLogRepository.save(log4);
        EventLog log5 = new EventLog(resource1, "Huge Event", "PropertyChangedEvent",
            "More details", timestamp + 10000, "OK", resource1.getId());
        eventLogRepository.save(log5);
        List<EventLog> expected = new ArrayList<EventLog>();
        expected.add(log2);
        expected.add(log1);
        expected.add(log4);
        Set<Resource> resources = new HashSet<Resource>();
        resources.add(resource1);
        assertEquals(expected,
            eventLogRepository.findByTimestampBetweenAndResourcesOrderByTimestamp(timestamp - 3000,
                timestamp + 3000, resources));
    }

    @Test
    public void testFindByTimestampBetweenAndResourcesAndEventTypes() {
        Resource resource1 = new Resource();
        resource1.setName("Resource1");
        entityManager.persist(resource1);
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        long timestamp = System.currentTimeMillis();
        EventLog log1 = new EventLog(resource1, "Big Event", "AlertFiredEvent", "Some details",
            timestamp, "OK", resource1.getId());
        eventLogRepository.save(log1);
        EventLog log2 = new EventLog(resource1, "Another Event", "AlertFiredEvent", "More details",
            timestamp - 2000, "OK", resource1.getId());
        eventLogRepository.save(log2);
        EventLog log3 = new EventLog(resource2, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "OK", resource2.getId());
        eventLogRepository.save(log3);
        EventLog log4 = new EventLog(resource1, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "Not OK", resource1.getId());
        eventLogRepository.save(log4);
        EventLog log5 = new EventLog(resource1, "Huge Event", "PropertyChangedEvent",
            "More details", timestamp + 10000, "OK", resource1.getId());
        eventLogRepository.save(log5);
        List<EventLog> expected = new ArrayList<EventLog>();
        expected.add(log2);
        expected.add(log1);
        Set<Resource> resources = new HashSet<Resource>();
        resources.add(resource1);
        assertEquals(expected,
            eventLogRepository.findByTimestampBetweenAndResourcesAndEventTypesOrderByTimestamp(
                timestamp - 3000, timestamp + 1000, resources,
                Arrays.asList(new String[] { "AlertFiredEvent" })));
    }

    @Test
    public void testFindByTimestampBetweenAndStatusAndResource() {
        Resource resource1 = new Resource();
        resource1.setName("Resource1");
        entityManager.persist(resource1);
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        long timestamp = System.currentTimeMillis();
        EventLog log1 = new EventLog(resource1, "Big Event", "AlertFiredEvent", "Some details",
            timestamp, "OK", resource1.getId());
        eventLogRepository.save(log1);
        EventLog log2 = new EventLog(resource1, "Another Event", "AlertFiredEvent", "More details",
            timestamp - 2000, "OK", resource1.getId());
        eventLogRepository.save(log2);
        EventLog log3 = new EventLog(resource2, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "OK", resource2.getId());
        eventLogRepository.save(log3);
        EventLog log4 = new EventLog(resource1, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "Not OK", resource1.getId());
        eventLogRepository.save(log4);
        EventLog log5 = new EventLog(resource1, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 10000, "OK", resource1.getId());
        eventLogRepository.save(log5);
        List<EventLog> expected = new ArrayList<EventLog>();
        expected.add(log2);
        expected.add(log1);
        assertEquals(expected,
            eventLogRepository.findByTimestampBetweenAndStatusAndResourceOrderByTimestampAsc(
                timestamp - 3000, timestamp + 1000, "OK", resource1));
    }

    @Test
    public void testFindByTimestampGreaterThanOrEqualToAndType() {
        Resource resource1 = new Resource();
        resource1.setName("Resource1");
        entityManager.persist(resource1);
        long timestamp = System.currentTimeMillis();
        EventLog log1 = new EventLog(resource1, "Big Event", "AlertFiredEvent", "Some details",
            timestamp, "OK", resource1.getId());
        eventLogRepository.save(log1);
        EventLog log2 = new EventLog(resource1, "Another Event", "AlertFiredEvent", "More details",
            timestamp - 2000, "OK", resource1.getId());
        eventLogRepository.save(log2);
        EventLog log3 = new EventLog(resource1, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "OK", resource1.getId());
        eventLogRepository.save(log3);
        List<EventLog> expected = new ArrayList<EventLog>();
        expected.add(log1);
        expected.add(log3);
        assertEquals(expected, eventLogRepository.findByTimestampGreaterThanOrEqualToAndType(
            timestamp, "AlertFiredEvent"));
    }

    @Test
    public void testGetMinimumTimeStamp() {
        Resource resource1 = new Resource();
        resource1.setName("Resource1");
        entityManager.persist(resource1);
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        long timestamp = System.currentTimeMillis();
        EventLog log1 = new EventLog(resource1, "Big Event", "AlertFiredEvent", "Some details",
            timestamp, "OK", resource1.getId());
        eventLogRepository.save(log1);
        EventLog log2 = new EventLog(resource1, "Another Event", "AlertFiredEvent", "More details",
            timestamp - 2000, "OK", resource1.getId());
        eventLogRepository.save(log2);
        EventLog log3 = new EventLog(resource2, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "OK", resource2.getId());
        eventLogRepository.save(log3);
        EventLog log4 = new EventLog(resource1, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "Not OK", resource1.getId());
        eventLogRepository.save(log4);
        EventLog log5 = new EventLog(resource1, "Huge Event", "PropertyChangedEvent",
            "More details", timestamp + 10000, "OK", resource1.getId());
        eventLogRepository.save(log5);
        assertEquals(Long.valueOf(timestamp - 2000), eventLogRepository.getMinimumTimeStamp());
    }
    
    @Test
    public void testGetMinimumTimeStampNoLogs() {
        assertEquals(Long.valueOf(-1l), eventLogRepository.getMinimumTimeStamp());
    }

    @Test
    public void testInsertLogs() {
        Resource resource1 = new Resource();
        resource1.setName("Resource1");
        entityManager.persist(resource1);
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        long timestamp = System.currentTimeMillis();
        EventLog log1 = new EventLog(resource1, "Big Event", "AlertFiredEvent", "Some details",
            timestamp, "OK", resource1.getId());

        EventLog log2 = new EventLog(resource1, "Another Event", "AlertFiredEvent", "More details",
            timestamp - 2000, "OK", resource1.getId());

        EventLog log3 = new EventLog(resource2, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "OK", resource2.getId());

        EventLog log4 = new EventLog(resource1, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2000, "Not OK", resource1.getId());

        EventLog log5 = new EventLog(resource1, "Huge Event", "PropertyChangedEvent",
            "More details", timestamp + 10000, "OK", resource1.getId());

        eventLogRepository.insertLogs(new EventLog[] { log1, log2, log3, log4, log5 });
        assertEquals(Long.valueOf(5), eventLogRepository.count());
    }

    @Test
    public void testLogsExistPerInterval() {
        Resource resource1 = new Resource();
        resource1.setName("Resource1");
        entityManager.persist(resource1);
        long timestamp = 1l;
        EventLog log1 = new EventLog(resource1, "Big Event", "AlertFiredEvent", "Some details",
            timestamp, "OK", resource1.getId());
        eventLogRepository.save(log1);
        EventLog log2 = new EventLog(resource1, "Another Event", "AlertFiredEvent", "More details",
            timestamp - 2, "OK", resource1.getId());
        eventLogRepository.save(log2);
        EventLog log3 = new EventLog(resource1, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2, "OK", resource1.getId());
        eventLogRepository.save(log3);
        EventLog log4 = new EventLog(resource1, "Huge Event", "AlertFiredEvent", "More details",
            timestamp + 2, "Not OK", resource1.getId());
        eventLogRepository.save(log4);
        EventLog log5 = new EventLog(resource1, "Huge Event", "PropertyChangedEvent",
            "More details", timestamp + 10, "OK", resource1.getId());
        eventLogRepository.save(log5);
        eventLogRepository.flush();
        boolean[] actual = eventLogRepository.logsExistPerInterval(resource1, timestamp - 2,
            timestamp + 10, 3);
        assertTrue(actual[0]);
        assertTrue(actual[1]);
        assertFalse(actual[2]);
    }
}
