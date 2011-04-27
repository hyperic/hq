package org.hyperic.hq.control.data;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.control.server.session.ControlFrequency;
import org.hyperic.hq.control.server.session.ControlHistory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/control/data/jpa-integration-test-context.xml" })
public class ControlHistoryRepositoryIntegrationTest {

    @Autowired
    private ControlHistoryRepository controlHistoryRepository;

    @Test
    public void testFindByGroupIdAndBatchId() {
        int resource1 = 83723;
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
        int resource1 = 83723;
        int resource2 = 898773;
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
        assertEquals(expected, controlHistoryRepository.findByResource(resource1));
    }

    @Test
    public void testFindByResourceSorted() {
        int resource1 = 83723;
        int resource2 = 898773;
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
            controlHistoryRepository.findByResource(resource1, new Sort("subject")));
    }

    @Test
    public void testFindByStartTimeGreaterThanOrderByStartTimeDesc() {
        long baseTime = System.currentTimeMillis();
        int resource1 = 83723;
        int resource2 = 898773;
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
        int resource1 = 83723;
        int resource2 = 898773;
        controlHistoryRepository.save(new ControlHistory(resource1, 1, 2, "bob", "restart", null,
            false, baseTime + 5000, System.currentTimeMillis() + 10000,
            System.currentTimeMillis() + 20000, "Completed", "Restarted", "All is well"));
        controlHistoryRepository.save(new ControlHistory(resource2, 4, 2, "bob", "restart", null,
            false, baseTime - 1000, System.currentTimeMillis() + 10000,
            System.currentTimeMillis() + 20000, "Completed", "Restarted", "All is well"));
        controlHistoryRepository.save(new ControlHistory(resource1, 1, 2, "aaron", "restart", null,
            false, baseTime + 10000, System.currentTimeMillis() + 10000,
            System.currentTimeMillis() + 20000, "Completed", "Restarted", "All is well"));
        controlHistoryRepository.flush();
        List<ControlFrequency> expected = new ArrayList<ControlFrequency>();
        expected.add(new ControlFrequency(resource1, "restart", 2l));
        expected.add(new ControlFrequency(resource2, "restart", 1l));
        assertEquals(expected, controlHistoryRepository.getControlFrequencies(1000));
    }

}
