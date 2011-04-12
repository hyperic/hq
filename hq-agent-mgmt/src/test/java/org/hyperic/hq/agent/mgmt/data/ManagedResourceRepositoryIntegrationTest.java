package org.hyperic.hq.agent.mgmt.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import net.sf.ehcache.CacheManager;

import org.hyperic.hq.agent.mgmt.domain.Agent;
import org.hyperic.hq.agent.mgmt.domain.AgentType;
import org.hyperic.hq.agent.mgmt.domain.ManagedResource;
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
public class ManagedResourceRepositoryIntegrationTest {

    @Autowired
    private ManagedResourceRepository managedResourceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Agent agent;

    @Before
    public void setUp() {
        AgentType agentType = new AgentType();
        agentType.setName("Unidirectional");
        entityManager.persist(agentType);
        this.agent = new Agent(agentType, "127.0.0.1", 2144, true, "auth", "token", "5.0");
        entityManager.persist(agent);
    }

    @After
    public void tearDown() {
        CacheManager.getInstance().clearAll();
    }

    @Test
    public void testCountUsedAgents() {
        ManagedResource managedResource = new ManagedResource(123, agent);
        managedResourceRepository.save(managedResource);
        ManagedResource managedResource2 = new ManagedResource(456, agent);
        managedResourceRepository.save(managedResource2);
        assertEquals(1l, managedResourceRepository.countUsedAgents());
    }

    @Test
    public void testFindByManagedResource() {
        int resourceId = 1234;
        ManagedResource managedResource = new ManagedResource(resourceId, agent);
        managedResourceRepository.save(managedResource);
        assertEquals(agent, managedResourceRepository.findAgentByResource(resourceId));
        verifyQueryCaching("Agent.findByManagedResource");
    }

    @Test
    public void testFindByManagedResourceNone() {
        assertNull(managedResourceRepository.findAgentByResource(1234));
    }

    private void verifyQueryCaching(String cacheName) {
        assertEquals(1, CacheManager.getInstance().getCache(cacheName).getSize());
    }

}
