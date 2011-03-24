package org.hyperic.hq.alert.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertInfo;
import org.hyperic.hq.events.server.session.ClassicEscalatable;
import org.hyperic.hq.events.server.session.ResourceAlertDefinition;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/alert/data/jpa-integration-test-context.xml" })
public class AlertRepositoryIntegrationTest {

    private Alert alert;

    private Alert alert2;

    private ResourceAlertDefinition alertdef2;

    @Autowired
    private AlertRepository alertRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Resource resource2;

    @Before
    public void setUp() {
        resource2 = new Resource();
        resource2.setName("Resource 2");
        entityManager.persist(resource2);
        alertdef2 = new ResourceAlertDefinition();
        alertdef2.setName("High Heap");
        alertdef2.setResource(resource2);
        alertdef2.setPriority(3);
        entityManager.persist(alertdef2);
        alert = new Alert();
        alert.setAlertDefinition(alertdef2);
        alertRepository.save(alert);
        alert2 = new Alert();
        alert2.setAlertDefinition(alertdef2);
        alertRepository.save(alert2);
    }

    @Test
    public void testCountByCreateTimeAndPriority() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        Alert alert3 = new Alert();
        alert3.setAlertDefinition(alertdef2);
        alert3.setCtime(timestamp + 2000);
        alertRepository.save(alert3);
        assertEquals(2l, alertRepository.countByCreateTimeAndPriority(timestamp - 6000,
            timestamp + 1000, 3, false, false, null, null));
    }

    @Test
    public void testCountByCreateTimeAndPriorityAlertDef() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        Resource resource1 = new Resource();
        resource1.setName("Resource 1");
        entityManager.persist(resource1);
        ResourceAlertDefinition alertdef = new ResourceAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResource(resource1);
        entityManager.persist(alertdef);
        Alert alert3 = new Alert();
        alert3.setAlertDefinition(alertdef);
        alert3.setCtime(timestamp + 2000);
        alertRepository.save(alert3);
        assertEquals(2l, alertRepository.countByCreateTimeAndPriority(timestamp - 6000,
            timestamp + 3000, 3, false, false, null, alertdef2.getId()));
    }

    @Test
    public void testCountByCreateTimeAndPriorityFilterNotFixed() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        Alert alert3 = new Alert();
        alert3.setAlertDefinition(alertdef2);
        alert3.setCtime(timestamp + 2000);
        alert3.setFixed(true);
        alertRepository.save(alert3);
        assertEquals(2l, alertRepository.countByCreateTimeAndPriority(timestamp - 6000,
            timestamp + 3000, 3, false, true, null, null));
    }

    @Test
    public void testCountByCreateTimeAndPriorityGroup() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group 1");
        group.addMember(resource2);
        entityManager.persist(group);
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        Resource resource1 = new Resource();
        resource1.setName("Resource 1");
        entityManager.persist(resource1);
        ResourceAlertDefinition alertdef = new ResourceAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResource(resource1);
        entityManager.persist(alertdef);
        Alert alert3 = new Alert();
        alert3.setAlertDefinition(alertdef);
        alert3.setCtime(timestamp + 2000);
        alertRepository.save(alert3);
        assertEquals(2l, alertRepository.countByCreateTimeAndPriority(timestamp - 6000,
            timestamp + 3000, 3, false, false, group.getId(), null));
    }

    @Test
    public void testCountByCreateTimeAndPriorityInEscalation() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        Alert alert3 = new Alert();
        alert3.setAlertDefinition(alertdef2);
        alert3.setCtime(timestamp + 2000);
        alert3.setFixed(true);
        alertRepository.save(alert3);
        Escalation escalation2 = new Escalation("Escalation2", "Important", true, 1l, true, true);
        entityManager.persist(escalation2);
        alertdef2.setEscalation(escalation2);
        EscalationState state = new EscalationState(new ClassicEscalatable(alert, "short", "long"));
        entityManager.persist(state);
        assertEquals(1l, alertRepository.countByCreateTimeAndPriority(timestamp - 6000,
            timestamp + 3000, 3, true, false, null, null));
    }

    @Test
    public void testCountByResource() {
        Resource resource1 = new Resource();
        resource1.setName("Resource 1");
        entityManager.persist(resource1);
        ResourceAlertDefinition alertdef = new ResourceAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResource(resource1);
        entityManager.persist(alertdef);
        Alert alert3 = new Alert();
        alert3.setAlertDefinition(alertdef);
        alertRepository.save(alert3);
        assertEquals(2l, alertRepository.countByResource(resource2));
    }

    @Test
    public void testDeleteByAlertDefinition() {
        Resource resource1 = new Resource();
        resource1.setName("Resource 1");
        entityManager.persist(resource1);
        ResourceAlertDefinition alertdef = new ResourceAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResource(resource1);
        entityManager.persist(alertdef);
        Alert alert3 = new Alert();
        alert3.setAlertDefinition(alertdef);
        alertRepository.save(alert3);
        alertRepository.deleteByAlertDefinition(alertdef2);
        entityManager.flush();
        entityManager.clear();
        assertEquals(Long.valueOf(1), alertRepository.count());
    }

    @Test
    public void testDeleteByCreateTime() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        alertRepository.save(alert);
        alertRepository.save(alert2);
        alertRepository.flush();
        alertRepository.deleteByCreateTime(timestamp - 1000, 100);
        assertEquals(Long.valueOf(1), alertRepository.count());
    }

    @Test
    public void testDeleteByCreateTimeMaxDeletesZero() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        alertRepository.save(alert);
        alertRepository.save(alert2);
        alertRepository.flush();
        alertRepository.deleteByCreateTime(timestamp - 1000, 0);
        assertEquals(Long.valueOf(2), alertRepository.count());
    }

    @Test
    public void testDeleteByIds() {
        alertRepository.deleteByIds(Arrays.asList(new Integer[] { alert.getId() }));
        assertEquals(Long.valueOf(1), alertRepository.count());
    }

    @Test
    public void testFindByCreateTimeAndPriority() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        Alert alert3 = new Alert();
        alert3.setAlertDefinition(alertdef2);
        alert3.setCtime(timestamp + 2000);
        alertRepository.save(alert3);
        PageRequest request = new PageRequest(0, 5, new Sort("ctime"));
        List<Alert> expected = new ArrayList<Alert>();
        expected.add(alert2);
        expected.add(alert);
        assertEquals(new PageImpl<Alert>(expected, request, 2l),
            alertRepository.findByCreateTimeAndPriority(timestamp - 6000, timestamp + 1000, 3,
                false, false, null, null, request));
    }

    @Test
    public void testFindByCreateTimeAndPriorityAlertDef() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        Resource resource1 = new Resource();
        resource1.setName("Resource 1");
        entityManager.persist(resource1);
        ResourceAlertDefinition alertdef = new ResourceAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResource(resource1);
        entityManager.persist(alertdef);
        Alert alert3 = new Alert();
        alert3.setAlertDefinition(alertdef);
        alert3.setCtime(timestamp + 2000);
        alertRepository.save(alert3);
        PageRequest request = new PageRequest(0, 5, new Sort("ctime"));
        List<Alert> expected = new ArrayList<Alert>();
        expected.add(alert2);
        expected.add(alert);
        assertEquals(new PageImpl<Alert>(expected, request, 2l),
            alertRepository.findByCreateTimeAndPriority(timestamp - 6000, timestamp + 3000, 3,
                false, false, null, alertdef2.getId(), request));
    }

    @Test
    public void testFindByCreateTimeAndPriorityFilterNotFixed() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        Alert alert3 = new Alert();
        alert3.setAlertDefinition(alertdef2);
        alert3.setCtime(timestamp + 2000);
        alert3.setFixed(true);
        alertRepository.save(alert3);
        PageRequest request = new PageRequest(0, 5, new Sort("ctime"));
        List<Alert> expected = new ArrayList<Alert>();
        expected.add(alert2);
        expected.add(alert);
        assertEquals(new PageImpl<Alert>(expected, request, 2l),
            alertRepository.findByCreateTimeAndPriority(timestamp - 6000, timestamp + 3000, 3,
                false, true, null, null, request));
    }

    @Test
    public void testFindByCreateTimeAndPriorityFilterNotSort() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        Alert alert3 = new Alert();
        alert3.setAlertDefinition(alertdef2);
        alert3.setCtime(timestamp + 2000);
        alert3.setFixed(true);
        alertRepository.save(alert3);
        List<Alert> expected = new ArrayList<Alert>();
        expected.add(alert2);
        expected.add(alert);
        assertEquals(expected, alertRepository.findByCreateTimeAndPriority(timestamp - 6000,
            timestamp + 3000, 3, false, true, null, null, new Sort("ctime")));
    }

    @Test
    public void testFindByCreateTimeAndPriorityGroup() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group 1");
        group.addMember(resource2);
        entityManager.persist(group);
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        Resource resource1 = new Resource();
        resource1.setName("Resource 1");
        entityManager.persist(resource1);
        ResourceAlertDefinition alertdef = new ResourceAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResource(resource1);
        entityManager.persist(alertdef);
        Alert alert3 = new Alert();
        alert3.setAlertDefinition(alertdef);
        alert3.setCtime(timestamp + 2000);
        alertRepository.save(alert3);
        PageRequest request = new PageRequest(0, 5, new Sort("ctime"));
        List<Alert> expected = new ArrayList<Alert>();
        expected.add(alert2);
        expected.add(alert);
        assertEquals(new PageImpl<Alert>(expected, request, 2l),
            alertRepository.findByCreateTimeAndPriority(timestamp - 6000, timestamp + 3000, 3,
                false, false, group.getId(), null, request));
    }

    @Test
    public void testFindByCreateTimeAndPriorityInEscalation() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        Alert alert3 = new Alert();
        alert3.setAlertDefinition(alertdef2);
        alert3.setCtime(timestamp + 2000);
        alert3.setFixed(true);
        alertRepository.save(alert3);
        Escalation escalation2 = new Escalation("Escalation2", "Important", true, 1l, true, true);
        entityManager.persist(escalation2);
        alertdef2.setEscalation(escalation2);
        EscalationState state = new EscalationState(new ClassicEscalatable(alert, "short", "long"));
        entityManager.persist(state);
        PageRequest request = new PageRequest(0, 5, new Sort("ctime"));
        List<Alert> expected = new ArrayList<Alert>();
        expected.add(alert);
        assertEquals(new PageImpl<Alert>(expected, request, 1l),
            alertRepository.findByCreateTimeAndPriority(timestamp - 6000, timestamp + 3000, 3,
                true, false, null, null, request));
    }

    @Test
    public void testFindByResourceInRange() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        Resource resource1 = new Resource();
        resource1.setName("Resource 1");
        entityManager.persist(resource1);
        ResourceAlertDefinition alertdef = new ResourceAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResource(resource1);
        entityManager.persist(alertdef);
        ResourceAlertDefinition alertdef3 = new ResourceAlertDefinition();
        alertdef3.setName("High Disk Usage");
        alertdef3.setResource(resource1);
        entityManager.persist(alertdef3);
        Alert alert3 = new Alert();
        alert3.setAlertDefinition(alertdef);
        alert3.setCtime(timestamp + 2000);
        alertRepository.save(alert3);
        Alert alert4 = new Alert();
        alert4.setAlertDefinition(alertdef3);
        alert4.setCtime(timestamp + 1000);
        alertRepository.save(alert4);
        List<Alert> expected = new ArrayList<Alert>();
        expected.add(alert3);
        expected.add(alert4);
        assertEquals(expected, alertRepository.findByResourceInRange(resource1, timestamp - 6000,
            timestamp + 3000, true, true));
    }

    @Test
    public void testFindByResourceInRangeNameSortFalse() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        Resource resource1 = new Resource();
        resource1.setName("Resource 1");
        entityManager.persist(resource1);
        ResourceAlertDefinition alertdef = new ResourceAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResource(resource1);
        entityManager.persist(alertdef);
        ResourceAlertDefinition alertdef3 = new ResourceAlertDefinition();
        alertdef3.setName("High Disk Usage");
        alertdef3.setResource(resource1);
        entityManager.persist(alertdef3);
        Alert alert3 = new Alert();
        alert3.setAlertDefinition(alertdef);
        alert3.setCtime(timestamp + 2000);
        alertRepository.save(alert3);
        Alert alert4 = new Alert();
        alert4.setAlertDefinition(alertdef3);
        alert4.setCtime(timestamp + 1000);
        alertRepository.save(alert4);
        List<Alert> expected = new ArrayList<Alert>();
        expected.add(alert4);
        expected.add(alert3);
        assertEquals(expected, alertRepository.findByResourceInRange(resource1, timestamp - 6000,
            timestamp + 3000, false, true));
    }

    @Test
    public void testFindLastByDefinition() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        assertEquals(alert, alertRepository.findLastByDefinition(alertdef2, false));
    }

    @Test
    public void testFindLastByDefinitionFixed() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        alert2.setFixed(true);
        assertEquals(alert2, alertRepository.findLastByDefinition(alertdef2, true));
    }

    @Test
    public void testGetOldestUnfixedAlertTime() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        Alert alert3 = new Alert();
        alert3.setAlertDefinition(alertdef2);
        alert3.setCtime(timestamp - 10000);
        alert3.setFixed(true);
        alertRepository.save(alert3);
        assertEquals(timestamp - 5000, alertRepository.getOldestUnfixedAlertTime());
    }

    @Test
    public void testGetUnfixedAlertInfoAfter() {
        long timestamp = System.currentTimeMillis();
        alert.setCtime(timestamp);
        alert2.setCtime(timestamp - 5000);
        alert2.setFixed(true);
        alertRepository.flush();
        final Map<Integer, Map<AlertInfo, Integer>> expected = new HashMap<Integer, Map<AlertInfo, Integer>>();
        Map<AlertInfo, Integer> expectedAlerts = new HashMap<AlertInfo, Integer>();
        expectedAlerts.put(new AlertInfo(alertdef2.getId(), timestamp), alert.getId());
        expected.put(alertdef2.getId(), expectedAlerts);
        assertEquals(expected, alertRepository.getUnfixedAlertInfoAfter(timestamp));
    }

    @Test
    public void testIsAckableNoEscStates() {
        assertFalse(alertRepository.isAckable(alert));
    }

    @Test
    public void testIsAckable() {
        Escalation escalation2 = new Escalation("Escalation2", "Important", true, 1l, true, true);
        entityManager.persist(escalation2);
        alertdef2.setEscalation(escalation2);
        EscalationState state = new EscalationState(new ClassicEscalatable(alert, "short", "long"));
        entityManager.persist(state);
        assertTrue(alertRepository.isAckable(alert));
    }

    public void testIsAckableAlreadyAcked() {
        AuthzSubject bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob",
            "Bobbins", "Bob", "123123123", "123123123", false);
        entityManager.persist(bob);
        Escalation escalation2 = new Escalation("Escalation2", "Important", true, 1l, true, true);
        entityManager.persist(escalation2);
        alertdef2.setEscalation(escalation2);
        EscalationState state = new EscalationState(new ClassicEscalatable(alert, "short", "long"));
        state.setAcknowledgedBy(bob);
        entityManager.persist(state);
        assertFalse(alertRepository.isAckable(alert));
    }

}
