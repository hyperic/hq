package org.hyperic.hq.agent.mgmt.data;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import net.sf.ehcache.CacheManager;

import org.hyperic.hq.agent.mgmt.domain.Agent;
import org.hyperic.hq.agent.mgmt.domain.AgentType;
import org.junit.After;
import org.junit.Before;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/agent/mgmt/data/jpa-integration-test-context.xml" })
public class AgentRepositoryIntegrationTest {

    private Agent agent;

    private Agent agent2;

    @Autowired
    private AgentRepository agentRepository;

    private AgentType agentType;

    @PersistenceContext
    private EntityManager entityManager;

    @Before
    public void setUp() {
        agentType = new AgentType();
        agentType.setName("Unidirectional");
        entityManager.persist(agentType);
        agent = new Agent(agentType, "127.0.0.1", 2144, true, "auth", "token", "5.0");
        agentRepository.save(agent);
        agent2 = new Agent(agentType, "127.0.0.1", 2133, true, "auth", "token2", "5.0");
        agentRepository.save(agent2);
    }

    @After
    public void tearDown() {
        CacheManager.getInstance().clearAll();
    }

    @Test
    public void testFindByAddress() {
        Set<Agent> expected = new HashSet<Agent>();
        expected.add(agent);
        expected.add(agent2);
        assertEquals(expected, new HashSet<Agent>(agentRepository.findByAddress("127.0.0.1")));
        verifyQueryCaching("Agent.findByAddress");
    }

    @Test
    public void testFindByAddressAndPort() {
        assertEquals(agent, agentRepository.findByAddressAndPort("127.0.0.1", 2144));
    }

    @Test
    public void testFindByAgentToken() {
        assertEquals(agent, agentRepository.findByAgentToken("token"));
        verifyQueryCaching("Agent.findByAgentToken");
    }

    @Test
    public void testLoadQueryCaches() throws Exception {
        agentRepository.loadQueryCaches();
        assertEquals(2, CacheManager.getInstance().getCache("Agent.findByAgentToken").getSize());
        assertEquals(1, CacheManager.getInstance().getCache("Agent.findByAddress").getSize());
    }

    private void verifyQueryCaching(String cacheName) {
        assertEquals(1, CacheManager.getInstance().getCache(cacheName).getSize());
    }
}
