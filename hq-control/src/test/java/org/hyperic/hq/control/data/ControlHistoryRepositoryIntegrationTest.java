package org.hyperic.hq.control.data;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.control.server.session.ControlFrequency;
import org.hyperic.hq.control.server.session.ControlHistory;
import org.hyperic.hq.inventory.domain.Resource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.Assert.assertEquals;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/control/data/jpa-integration-test-context.xml" })
public class ControlHistoryRepositoryIntegrationTest {

    @Autowired
    private ControlHistoryRepository controlHistoryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void testFindByGroupIdAndBatchId() {
        Resource resource1 = createResource("Resource1");
        ControlHistory controlHistory1 = new ControlHistory(resource1, 1, 2, "bob", "restart",
            null, false, System.currentTimeMillis(), System.currentTimeMillis() + 10000,
            System.currentTimeMillis() + 20000, "Completed", "Restarted", "All is well");
        controlHistoryRepository.save(controlHistory1);
        controlHistoryRepository.save(new ControlHistory(resource1, 4, 2, "bob", "restart", null,
            false, System.currentTimeMillis(), System.currentTimeMillis() + 10000, System
                .currentTimeMillis() + 20000, "Completed", "Restarted", "All is well"));
        ControlHistory controlHistory2 = new ControlHistory(resource1, 1, 2, "aaron", "restart",
            null, false, System.currentTimeMillis(), System.currentTimeMillis() + 10000,
            System.currentTimeMillis() + 20000, "Completed", "Restarted", "All is well");
        controlHistoryRepository.save(controlHistory2);
        List<ControlHistory> expected = new ArrayList<ControlHistory>();
        expected.add(controlHistory2);
        expected.add(controlHistory1);
        assertEquals(expected,
            controlHistoryRepository.findByGroupIdAndBatchId(1, 2, new Sort("subject")));
    }

    @Test
    public void testFindByResource() {
        Resource resource1 = createResource("Resource1");
        Resource resource2 = createResource("Resource2");
        ControlHistory controlHistory1 = new ControlHistory(resource1, 1, 2, "bob", "restart",
            null, false, System.currentTimeMillis(), System.currentTimeMillis() + 10000,
            System.currentTimeMillis() + 20000, "Completed", "Restarted", "All is well");
        controlHistoryRepository.save(controlHistory1);
        controlHistoryRepository.save(new ControlHistory(resource2, 4, 2, "bob", "restart", null,
            false, System.currentTimeMillis(), System.currentTimeMillis() + 10000, System
                .currentTimeMillis() + 20000, "Completed", "Restarted", "All is well"));
        ControlHistory controlHistory2 = new ControlHistory(resource1, 1, 2, "aaron", "restart",
            null, false, System.currentTimeMillis(), System.currentTimeMillis() + 10000,
            System.currentTimeMillis() + 20000, "Completed", "Restarted", "All is well");
        controlHistoryRepository.save(controlHistory2);
        List<ControlHistory> expected = new ArrayList<ControlHistory>();
        expected.add(controlHistory1);
        expected.add(controlHistory2);
        assertEquals(expected, controlHistoryRepository.findByResource(resource1.getId()));
    }

    @Test
    public void testFindByResourceSorted() {
        Resource resource1 = createResource("Resource1");
        Resource resource2 = createResource("Resource2");
        ControlHistory controlHistory1 = new ControlHistory(resource1, 1, 2, "bob", "restart",
            null, false, System.currentTimeMillis(), System.currentTimeMillis() + 10000,
            System.currentTimeMillis() + 20000, "Completed", "Restarted", "All is well");
        controlHistoryRepository.save(controlHistory1);
        controlHistoryRepository.save(new ControlHistory(resource2, 4, 2, "bob", "restart", null,
            false, System.currentTimeMillis(), System.currentTimeMillis() + 10000, System
                .currentTimeMillis() + 20000, "Completed", "Restarted", "All is well"));
        ControlHistory controlHistory2 = new ControlHistory(resource1, 1, 2, "aaron", "restart",
            null, false, System.currentTimeMillis(), System.currentTimeMillis() + 10000,
            System.currentTimeMillis() + 20000, "Completed", "Restarted", "All is well");
        controlHistoryRepository.save(controlHistory2);
        List<ControlHistory> expected = new ArrayList<ControlHistory>();
        expected.add(controlHistory2);
        expected.add(controlHistory1);
        assertEquals(expected,
            controlHistoryRepository.findByResource(resource1.getId(), new Sort("subject")));
    }

    @Test
    public void testFindByStartTimeGreaterThanOrderByStartTimeDesc() {
        long baseTime = System.currentTimeMillis();
        Resource resource1 = createResource("Resource1");
        Resource resource2 = createResource("Resource2");
        ControlHistory controlHistory1 = new ControlHistory(resource1, 1, 2, "bob", "restart",
            null, false, baseTime + 5000, System.currentTimeMillis() + 10000,
            System.currentTimeMillis() + 20000, "Completed", "Restarted", "All is well");
        controlHistoryRepository.save(controlHistory1);
        controlHistoryRepository.save(new ControlHistory(resource2, 4, 2, "bob", "restart", null,
            false, baseTime - 1000, System.currentTimeMillis() + 10000,
            System.currentTimeMillis() + 20000, "Completed", "Restarted", "All is well"));
        ControlHistory controlHistory2 = new ControlHistory(resource1, 1, 2, "aaron", "restart",
            null, false, baseTime + 10000, System.currentTimeMillis() + 10000,
            System.currentTimeMillis() + 20000, "Completed", "Restarted", "All is well");
        controlHistoryRepository.save(controlHistory2);
        List<ControlHistory> expected = new ArrayList<ControlHistory>();
        expected.add(controlHistory2);
        expected.add(controlHistory1);
        assertEquals(expected,
            controlHistoryRepository.findByStartTimeGreaterThanOrderByStartTimeDesc(baseTime));
    }
    
    @Test
    public void testGetControlFrequencies() throws SQLException {
        long baseTime = System.currentTimeMillis();
        Resource resource1 = createResource("Resource1");
        Resource resource2 = createResource("Resource2");
        controlHistoryRepository.save(new ControlHistory(resource1, 1, 2, "bob", "restart",
            null, false, baseTime + 5000, System.currentTimeMillis() + 10000,
            System.currentTimeMillis() + 20000, "Completed", "Restarted", "All is well"));
       controlHistoryRepository.save(new ControlHistory(resource2, 4, 2, "bob", "restart", null,
            false, baseTime - 1000, System.currentTimeMillis() + 10000,
            System.currentTimeMillis() + 20000, "Completed", "Restarted", "All is well"));
        controlHistoryRepository.save( new ControlHistory(resource1, 1, 2, "aaron", "restart",
            null, false, baseTime + 10000, System.currentTimeMillis() + 10000,
            System.currentTimeMillis() + 20000, "Completed", "Restarted", "All is well"));
        controlHistoryRepository.flush();
        List<ControlFrequency> expected = new ArrayList<ControlFrequency>();
        expected.add(new ControlFrequency(resource1.getId(), "restart", 2l));
        expected.add(new ControlFrequency(resource2.getId(), "restart", 1l));
        assertEquals(expected,controlHistoryRepository.getControlFrequencies(1000));
    }

    private Resource createResource(String name) {
        Resource resource = new Resource();
        resource.setName(name);
        entityManager.persist(resource);
        return resource;
    }

}
