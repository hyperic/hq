package org.hyperic.hq.autoinventory.data;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.autoinventory.AIHistory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.Assert.assertEquals;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/autoinventory/data/jpa-integration-test-context.xml" })
public class AIHistoryRepositoryIntegrationTest {

    @Autowired
    private AIHistoryRepository aiHistoryRepository;

    @Test
    public void testFindByEntityTypeAndEntityId() {
        AIHistory history1 = createAIHistory(2, 123, System.currentTimeMillis());
        createAIHistory(3, 123, System.currentTimeMillis());
        AIHistory history3 = createAIHistory(2, 123, System.currentTimeMillis());
        List<AIHistory> expected = new ArrayList<AIHistory>();
        expected.add(history1);
        expected.add(history3);

        assertEquals(expected, aiHistoryRepository.findByEntityTypeAndEntityId(2, 123));
    }

    @Test
    public void testFindByEntityTypeAndEntityIdSortedAsc() {
        AIHistory history1 = createAIHistory(2, 123, System.currentTimeMillis() + 10000);
        createAIHistory(3, 123, System.currentTimeMillis() + 5000);
        AIHistory history3 = createAIHistory(2, 123, System.currentTimeMillis());
        List<AIHistory> expected = new ArrayList<AIHistory>();
        expected.add(history3);
        expected.add(history1);
        assertEquals(expected,
            aiHistoryRepository.findByEntityTypeAndEntityId(2, 123, new Sort("startTime")));
    }

    @Test
    public void testFindByEntityTypeAndEntityIdSortedDesc() {
        AIHistory history1 = createAIHistory(2, 123, System.currentTimeMillis() + 10000);
        createAIHistory(3, 123, System.currentTimeMillis() + 5000);
        AIHistory history3 = createAIHistory(2, 123, System.currentTimeMillis());
        List<AIHistory> expected = new ArrayList<AIHistory>();
        expected.add(history1);
        expected.add(history3);
        assertEquals(expected, aiHistoryRepository.findByEntityTypeAndEntityId(2, 123, new Sort(
            Direction.DESC, "startTime")));
    }

    private AIHistory createAIHistory(int entityType, int entityId, long startTime) {
        AIHistory aiHistory = new AIHistory();
        aiHistory.setEntityId(entityId);
        aiHistory.setEntityType(entityType);
        aiHistory.setSubject("jen");
        aiHistory.setDateScheduled(startTime - 30000);
        aiHistory.setStartTime(startTime);
        aiHistory.setEndTime(startTime + 1000);
        aiHistory.setDuration(1000);
        aiHistory.setStatus("Completed");
        aiHistory.setConfig(new byte[0]);
        aiHistoryRepository.save(aiHistory);
        return aiHistory;
    }
}
