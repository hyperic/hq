package org.hyperic.hq.autoinventory.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.autoinventory.AIPlatform;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/autoinventory/data/jpa-integration-test-context.xml" })
public class AIPlatformRepositoryIntegrationTest {

    @Autowired
    private AIPlatformRepository aiPlatformRepository;

    @Test
    public void testFindAllNotIgnored() {
        AIPlatform platform1 = createAIPlatform("Platform1", System.currentTimeMillis() - 5000,
            false, "agentToken1");
        // This one shouldn't be included
        createAIPlatform("Platfom2", System.currentTimeMillis() + 10000, false, "agentToken1");
        AIPlatform platform3 = createAIPlatform("OtherPlatform",
            System.currentTimeMillis() - 10000, false, "agentToken1");

        // make sure they are sorted by name
        List<AIPlatform> expected = new ArrayList<AIPlatform>();
        expected.add(platform3);
        expected.add(platform1);

        assertEquals(expected, aiPlatformRepository.findAllNotIgnored());
    }

    @Test
    public void testFindByIgnoredOrderByNameAsc() {
        AIPlatform platform1 = createAIPlatform("Platform1", System.currentTimeMillis() - 5000,
            false, "agentToken1");
        AIPlatform platform2 = createAIPlatform("Platform2", System.currentTimeMillis() + 10000,
            false, "agentToken1");
        AIPlatform platform3 = createAIPlatform("OtherPlatform",
            System.currentTimeMillis() - 10000, false, "agentToken1");
        // This one shouldn't be included
        createAIPlatform("Platfom4", System.currentTimeMillis() + 10000, true, "agentToken1");
        // make sure they are sorted by name
        List<AIPlatform> expected = new ArrayList<AIPlatform>();
        expected.add(platform3);
        expected.add(platform1);
        expected.add(platform2);

        assertEquals(expected, aiPlatformRepository.findByIgnoredOrderByNameAsc(false));
    }

    @Test
    public void testFindByFqdn() {
        createAIPlatform("Platform1", System.currentTimeMillis() - 5000, false, "agentToken1");
        AIPlatform platform2 = createAIPlatform("Platform2", System.currentTimeMillis() + 10000,
            false, "agentToken1");
        assertEquals(platform2, aiPlatformRepository.findByFqdn("Platform2"));
    }

    @Test
    public void testFindByFqdnNonExistent() {
        assertNull(aiPlatformRepository.findByFqdn("Platform2"));
    }

    @Test
    public void testFindByCertdn() {
        createAIPlatform("Platform1", System.currentTimeMillis() - 5000, false, "agentToken1");
        AIPlatform platform2 = createAIPlatform("Platform2", System.currentTimeMillis() + 10000,
            false, "agentToken1");
        assertEquals(platform2, aiPlatformRepository.findByCertdn("Platform2"));
    }

    @Test
    public void testFindByCertdnNonExistent() {
        assertNull(aiPlatformRepository.findByCertdn("Platform2"));
    }

    @Test
    public void testFindByAgentToken() {
        createAIPlatform("Platform1", System.currentTimeMillis() - 5000, false, "agentToken1");
        AIPlatform platform2 = createAIPlatform("Platform2", System.currentTimeMillis() + 10000,
            false, "agentToken2");
        assertEquals(platform2, aiPlatformRepository.findByAgentToken("agentToken2"));
    }

    @Test(expected = IncorrectResultSizeDataAccessException.class)
    public void testFindByAgentTokenMultiple() {
        createAIPlatform("Platform1", System.currentTimeMillis() - 5000, false, "agentToken1");
        createAIPlatform("Platform2", System.currentTimeMillis() + 10000, false, "agentToken1");
        aiPlatformRepository.findByAgentToken("agentToken1");
    }

    @Test
    public void testFindByAgentTokenNonExistent() {
        assertNull(aiPlatformRepository.findByAgentToken("agentToken"));
    }

    private AIPlatform createAIPlatform(String name, long lastApproved, boolean ignored,
                                        String agentToken) {
        AIPlatform aiPlatform = new AIPlatform();
        aiPlatform.setFqdn(name);
        aiPlatform.setName(name);
        aiPlatform.setCertdn(name);
        aiPlatform.setAgentToken(agentToken);
        aiPlatform.setPlatformTypeName("JenOS");
        aiPlatform.setModifiedTime(System.currentTimeMillis());
        aiPlatform.setLastApproved(lastApproved);
        aiPlatform.setIgnored(ignored);
        aiPlatformRepository.save(aiPlatform);
        return aiPlatform;
    }

}
