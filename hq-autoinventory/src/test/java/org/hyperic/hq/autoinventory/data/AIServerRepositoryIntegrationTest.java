package org.hyperic.hq.autoinventory.data;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.autoinventory.AIPlatform;
import org.hyperic.hq.autoinventory.AIServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/autoinventory/data/jpa-integration-test-context.xml" })
public class AIServerRepositoryIntegrationTest {
    @Autowired
    private AIServerRepository aiServerRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private AIPlatform aiPlatform;

    private AIServer aiServer;

    @Before
    public void setUp() {
        aiPlatform = new AIPlatform();
        aiPlatform.setFqdn("Platform1");
        aiPlatform.setAgentToken("agentToken1");
        aiPlatform.setPlatformTypeName("JenOS");
        entityManager.persist(aiPlatform);

        aiServer = new AIServer();
        aiServer.setServerTypeName("Tomcat");
        aiServer.setName("Server1");
        aiServer.setAIPlatform(aiPlatform);
        aiServerRepository.save(aiServer);
    }

    @Test
    public void testFindByAIPlatform() {
        List<AIServer> expected = new ArrayList<AIServer>();
        expected.add(aiServer);
        assertEquals(expected, aiServerRepository.findByAIPlatform(aiPlatform.getId()));
    }

    @Test
    public void testFindByAIPlatformNotFound() {
        assertTrue(aiServerRepository.findByAIPlatform(76342).isEmpty());
    }

    @Test
    public void testFindByName() {
        assertEquals(aiServer, aiServerRepository.findByName("Server1"));
    }

    @Test
    public void testFindByNameNotFound() {
        assertNull(aiServerRepository.findByName("Foo"));
    }

}
