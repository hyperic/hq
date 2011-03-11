package org.hyperic.hq.dashboard.data;

import static org.junit.Assert.assertEquals;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import net.sf.ehcache.CacheManager;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.config.domain.Crispo;
import org.hyperic.hq.ui.server.session.UserDashboardConfig;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/dashboard/data/jpa-integration-test-context.xml" })
public class UserDashboardConfigRepositoryIntegrationTest {

    private AuthzSubject bob;

    @PersistenceContext
    private EntityManager entityManager;

    private AuthzSubject sue;

    private UserDashboardConfig userConfig1;

    private UserDashboardConfig userConfig2;

    @Autowired
    private UserDashboardConfigRepository userDashboardConfigRepository;

    @Before
    public void setUp() {
        bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob", "Bobbins", "Bob",
            "123123123", "123123123", false);
        entityManager.persist(bob);
        sue = new AuthzSubject(true, "sue", "dev", "sue@bob.com", true, "Sue", "Bobbins", "Sue",
            "123123123", "123123123", false);
        entityManager.persist(sue);
        Crispo config = new Crispo();
        entityManager.persist(config);
        userConfig1 = new UserDashboardConfig(bob, "Config1", config);
        userDashboardConfigRepository.save(userConfig1);
        userConfig2 = new UserDashboardConfig(sue, "Config2", config);
        userDashboardConfigRepository.save(userConfig2);
    }

    @After
    public void tearDown() {
        CacheManager.getInstance().clearAll();
    }

    @Test
    public void testDeleteByUser() {
        userDashboardConfigRepository.deleteByUser(bob);
        assertEquals(Long.valueOf(1), userDashboardConfigRepository.count());
    }

    @Test
    public void testFindByUser() {
        assertEquals(userConfig1, userDashboardConfigRepository.findByUser(bob));
        verifyQueryCaching("UserDashboardConfig.findDashboard");
    }

    private void verifyQueryCaching(String cacheName) {
        assertEquals(1, CacheManager.getInstance().getCache(cacheName).getSize());
    }
}
