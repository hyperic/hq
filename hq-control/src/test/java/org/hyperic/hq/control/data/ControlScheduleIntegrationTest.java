package org.hyperic.hq.control.data;

import static org.junit.Assert.assertEquals;


import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.control.server.session.ControlSchedule;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/control/data/jpa-integration-test-context.xml" })
public class ControlScheduleIntegrationTest {

    @Autowired
    private ControlScheduleRepository controlScheduleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void testFindByResource() {
        Resource resource1 = new Resource();
        resource1.setName("Resource1");
        entityManager.persist(resource1);
        ControlSchedule schedule1 = createControlSchedule(resource1, "Job1", "Trig1");
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        createControlSchedule(resource2, "Job2", "Trig2");
        List<ControlSchedule> expected = new ArrayList<ControlSchedule>();
        expected.add(schedule1);
        assertEquals(expected,controlScheduleRepository.findByResource(resource1.getId()));
    }

    private ControlSchedule createControlSchedule(Resource resource, String jobName, String triggerName) {
        ControlSchedule s = new ControlSchedule();
        s.setResource(resource);
        s.setSubject("bob");
        s.setNextFireTime(System.currentTimeMillis() + 10000);
        s.setTriggerName(triggerName);
        s.setJobName(jobName);
        s.setAction("stop");
        s.setScheduleValueBytes(new byte[0]);
        controlScheduleRepository.save(s);
        return s;
    }

}
