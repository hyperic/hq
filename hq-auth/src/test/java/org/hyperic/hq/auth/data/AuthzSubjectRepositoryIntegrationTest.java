package org.hyperic.hq.auth.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.Set;

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
    public void testFindOwner() {
        Resource resource1 = new Resource();
        resource1.setName("Resource 1");
        entityManager.persist(resource1);
        AuthzSubject bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob",
            "Bobbins", "Bob", "123123123", "123123123", false);
        authzSubjectRepository.save(bob);
        authzSubjectRepository.setOwner(bob, resource1);
        assertEquals(bob, authzSubjectRepository.findOwner(resource1));
    }

    @Test
    public void testFindByOwnerNoOwner() {
        Resource resource1 = new Resource();
        resource1.setName("Resource 1");
        entityManager.persist(resource1);
        AuthzSubject bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob",
            "Bobbins", "Bob", "123123123", "123123123", false);
        authzSubjectRepository.save(bob);
        assertNull(authzSubjectRepository.findOwner(resource1));
    }
    
    @Test
    public void testRemoveOwner() {
        Resource resource1 = new Resource();
        resource1.setName("Resource 1");
        entityManager.persist(resource1);
        AuthzSubject bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob",
            "Bobbins", "Bob", "123123123", "123123123", false);
        authzSubjectRepository.save(bob);
        authzSubjectRepository.setOwner(bob, resource1);
        entityManager.flush();
        entityManager.clear();
        authzSubjectRepository.removeOwner(bob, resource1);
        entityManager.flush();
        entityManager.clear();
        assertNull(authzSubjectRepository.findOwner(resource1));
    }
    
    @Test
    public void testGetOwnedResources() {
        Resource resource1 = new Resource();
        resource1.setName("Resource 1");
        entityManager.persist(resource1);
        Resource resource2 = new Resource();
        resource2.setName("Resource 2");
        entityManager.persist(resource2);
        AuthzSubject bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob",
            "Bobbins", "Bob", "123123123", "123123123", false);
        authzSubjectRepository.save(bob);
        authzSubjectRepository.setOwner(bob, resource1);
        authzSubjectRepository.setOwner(bob, resource2);
        Set<Resource> expected = new HashSet<Resource>();
        expected.add(resource1);
        expected.add(resource2);
        assertEquals(expected,authzSubjectRepository.getOwnedResources(bob));
    }

    private void verifyQueryCaching(String cacheName) {
        assertEquals(1, CacheManager.getInstance().getCache(cacheName).getSize());
    }
}
