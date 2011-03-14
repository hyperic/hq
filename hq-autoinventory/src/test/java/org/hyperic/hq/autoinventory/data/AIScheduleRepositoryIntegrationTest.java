package org.hyperic.hq.autoinventory.data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.autoinventory.AISchedule;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/autoinventory/data/jpa-integration-test-context.xml" })
public class AIScheduleRepositoryIntegrationTest {

    @Autowired
    private AIScheduleRepository aiScheduleRepository;

    @Test
    public void testFindByScanName() {
        AISchedule schedule1 = createAISchedule(2, 1234, "Schedule1", "trig1", "job1");
        createAISchedule(2, 1234, "Schedule2", "trig2", "job2");
        assertEquals(schedule1, aiScheduleRepository.findByScanName("Schedule1"));
    }

    @Test
    public void testFindByEntityIdAndTypeSort() {
        AISchedule schedule3 = createAISchedule(2, 1234, "Schedule3", "trig3", "job3");
        AISchedule schedule1 = createAISchedule(2, 1234, "Schedule1", "trig1", "job1");
        createAISchedule(3, 1234, "Schedule2", "trig2", "job2");
        List<AISchedule> expected = new ArrayList<AISchedule>();
        expected.add(schedule1);
        expected.add(schedule3);
        assertEquals(expected,
            aiScheduleRepository.findByEntityTypeAndEntityId(2, 1234, new Sort("scanName")));
    }

    private AISchedule createAISchedule(int entityType, int entityId, String scanName,
                                        String triggerName, String jobName) {
        AISchedule s = new AISchedule();
        s.setEntityId(entityId);
        s.setEntityType(entityType);
        s.setSubject("bob");
        s.setNextFireTime(System.currentTimeMillis() + 10000);
        s.setTriggerName(triggerName);
        s.setJobName(jobName);
        s.setScanName(scanName);
        aiScheduleRepository.save(s);
        return s;
    }
}
