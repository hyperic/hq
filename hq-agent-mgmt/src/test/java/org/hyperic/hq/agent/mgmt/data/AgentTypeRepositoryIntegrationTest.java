package org.hyperic.hq.agent.mgmt.data;

import org.hyperic.hq.agent.mgmt.domain.AgentType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/agent/mgmt/data/jpa-integration-test-context.xml" })
public class AgentTypeRepositoryIntegrationTest {

    @Autowired
    private AgentTypeRepository agentTypeRepository;

    @Test
    public void testFindByName() {
        AgentType agentType = new AgentType();
        agentType.setName("Agent1");
        agentTypeRepository.save(agentType);

        AgentType agentType2 = new AgentType();
        agentType2.setName("Agent2");
        agentTypeRepository.save(agentType2);

        assertEquals(agentType, agentTypeRepository.findByName("Agent1"));
    }

    @Test
    public void testFindByNameNotExists() {
        assertNull(agentTypeRepository.findByName("Agent1"));
    }
}
