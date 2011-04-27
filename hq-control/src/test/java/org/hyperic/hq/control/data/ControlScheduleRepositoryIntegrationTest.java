package org.hyperic.hq.control.data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.control.server.session.ControlSchedule;
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
public class ControlScheduleRepositoryIntegrationTest {

    @Autowired
    private ControlScheduleRepository controlScheduleRepository;

    @Test
    public void testFindByResource() {
        int resource1 = 83723;
        int resource2 = 898773;
        ControlSchedule schedule1 = createControlSchedule(resource1, "Job1", "Trig1",
            System.currentTimeMillis() + 10000);
        createControlSchedule(resource2, "Job2", "Trig2", System.currentTimeMillis() + 10000);
        List<ControlSchedule> expected = new ArrayList<ControlSchedule>();
        expected.add(schedule1);
        assertEquals(expected, controlScheduleRepository.findByResource(resource1));
    }

    @Test
    public void testFindByResourceSorted() {
        int resource1 = 83723;
        int resource2 = 898773;
        createControlSchedule(resource1, "Job1", "Trig1", System.currentTimeMillis() + 10000);
        ControlSchedule job2Schedule = createControlSchedule(resource2, "Job2", "Trig2",
            System.currentTimeMillis() + 10000);
        ControlSchedule aJobSchedule = createControlSchedule(resource2, "AJob", "ATrig",
            System.currentTimeMillis() + 10000);
        List<ControlSchedule> expected = new ArrayList<ControlSchedule>();
        expected.add(aJobSchedule);
        expected.add(job2Schedule);
        assertEquals(expected,
            controlScheduleRepository.findByResource(resource2, new Sort("jobName")));
    }

    private ControlSchedule createControlSchedule(Integer resource, String jobName,
                                                  String triggerName, long nextFireTime) {
        ControlSchedule s = new ControlSchedule();
        s.setResource(resource);
        s.setSubject("bob");
        s.setNextFireTime(nextFireTime);
        s.setTriggerName(triggerName);
        s.setJobName(jobName);
        s.setAction("stop");
        s.setScheduleValueBytes(new byte[0]);
        controlScheduleRepository.save(s);
        return s;
    }

}
