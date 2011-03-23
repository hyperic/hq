package org.hyperic.hq.auth.data;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import net.sf.ehcache.CacheManager;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.inventory.domain.Resource;
import org.junit.After;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/auth/data/jpa-integration-test-context.xml" })
public class AuthzSubjectRepositoryIntegrationTest {

    @Autowired
    private AuthzSubjectRepository authzSubjectRepository;

    @PersistenceContext
    private EntityManager entityManager;

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

    @Test
    public void testFindByOwnedResource() {
        Resource resource1 = new Resource();
        resource1.setName("Resource 1");
        entityManager.persist(resource1);
        AuthzSubject bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob",
            "Bobbins", "Bob", "123123123", "123123123", false);
        authzSubjectRepository.save(bob);
        bob.addOwnedResource(resource1);
        assertEquals(bob, authzSubjectRepository.findByOwnedResource(resource1));
    }

    @Test
    public void testFindByOwnedResourceNoOwner() {
        Resource resource1 = new Resource();
        resource1.setName("Resource 1");
        entityManager.persist(resource1);
        AuthzSubject bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob",
            "Bobbins", "Bob", "123123123", "123123123", false);
        authzSubjectRepository.save(bob);
        assertNull(authzSubjectRepository.findByOwnedResource(resource1));
    }

    private void verifyQueryCaching(String cacheName) {
        assertEquals(1, CacheManager.getInstance().getCache(cacheName).getSize());
    }
}
