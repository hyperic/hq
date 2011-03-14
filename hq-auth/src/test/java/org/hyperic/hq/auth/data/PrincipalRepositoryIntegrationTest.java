package org.hyperic.hq.auth.data;

import org.hyperic.hq.auth.Principal;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/auth/data/jpa-integration-test-context.xml" })
public class PrincipalRepositoryIntegrationTest {

    @Autowired
    private PrincipalRepository principalRepository;

    @Test
    public void testFindByPrincipal() {
        Principal principal = new Principal();
        principal.setPrincipal("bob");
        principal.setPassword("foo");
        principalRepository.save(principal);
        Principal principal2 = new Principal();
        principal2.setPrincipal("sam");
        principal2.setPassword("bar");
        principalRepository.save(principal2);
        assertEquals(principal, principalRepository.findByPrincipal("bob"));
    }
}
