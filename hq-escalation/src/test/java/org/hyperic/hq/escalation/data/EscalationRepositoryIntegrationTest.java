package org.hyperic.hq.escalation.data;

import org.hyperic.hq.escalation.server.session.Escalation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.Assert.assertEquals;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/escalation/data/jpa-integration-test-context.xml" })
public class EscalationRepositoryIntegrationTest {

    @Autowired
    private EscalationRepository escalationRepository;
    
    @Test
    public void testFindByName() {
        Escalation escalation = new Escalation("Escalation1", "Important", true, 1l, true, true);
        escalationRepository.save(escalation);
        Escalation escalation2 = new Escalation("Escalation2", "Important", true, 1l, true, true);
        escalationRepository.save(escalation2);
        assertEquals(escalation,escalationRepository.findByName("Escalation1"));
    }
}
