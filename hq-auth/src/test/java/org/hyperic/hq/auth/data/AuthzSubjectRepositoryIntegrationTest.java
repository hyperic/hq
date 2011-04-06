package org.hyperic.hq.auth.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import net.sf.ehcache.CacheManager;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.junit.After;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/auth/data/jpa-integration-test-context.xml" })
public class AuthzSubjectRepositoryIntegrationTest {

    @Autowired
    private AuthzSubjectRepository authzSubjectRepository;

    @After
    public void tearDown() {
        CacheManager.getInstance().clearAll();
    }

    @Test
    public void testFindByName() {
        AuthzSubject bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob",
            "Bobbins", "Bob", "123123123", "123123123", false);
        authzSubjectRepository.save(bob);
        AuthzSubject sue = new AuthzSubject(true, "sue", "dev", "bob@bob.com", true, "Sue",
            "Bobbins", "Sue", "123123123", "123123123", false);
        authzSubjectRepository.save(sue);
        assertEquals(bob, authzSubjectRepository.findByName("Bob"));
        verifyQueryCaching("AuthzSubject.findByName");
    }

    @Test
    public void testFindByNameAndDsn() {
        AuthzSubject bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob",
            "Bobbins", "Bob", "123123123", "123123123", false);
        authzSubjectRepository.save(bob);
        AuthzSubject sue = new AuthzSubject(true, "sue", "dev", "bob@bob.com", true, "Sue",
            "Bobbins", "Bob", "123123123", "123123123", false);
        authzSubjectRepository.save(sue);
        assertEquals(bob, authzSubjectRepository.findByNameAndDsn("Bob", "bob"));
        verifyQueryCaching("AuthzSubject.findByAuth");
    }

    @Test
    public void testFindByNameAndDsnNone() {
        assertNull(authzSubjectRepository.findByNameAndDsn("Bob", "bob"));
    }

    @Test
    public void testFindByNameNone() {
        assertNull(authzSubjectRepository.findByName("Bob"));
    }

    private void verifyQueryCaching(String cacheName) {
        assertEquals(1, CacheManager.getInstance().getCache(cacheName).getSize());
    }
}
