package org.hyperic.hq.dashboard.data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import net.sf.ehcache.CacheManager;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.auth.domain.Role;
import org.hyperic.hq.config.domain.Crispo;
import org.hyperic.hq.ui.server.session.RoleDashboardConfig;
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
public class RoleDashboardConfigRepositoryIntegrationTest {

    private Crispo config;

    private RoleDashboardConfig config1;

    private RoleDashboardConfig config2;

    @PersistenceContext
    private EntityManager entityManager;

    private Role role1;

    @Autowired
    private RoleDashboardConfigRepository roleDashboardConfigRepository;

    @Before
    public void setUp() {
        config = new Crispo();
        entityManager.persist(config);
        role1 = new Role();
        role1.setName("Operator");
        entityManager.persist(role1);
        Role role2 = new Role();
        role2.setName("Developer");
        entityManager.persist(role2);
        config1 = new RoleDashboardConfig(role1, "Config1", config);
        roleDashboardConfigRepository.save(config1);
        config2 = new RoleDashboardConfig(role2, "Config2", config);
        roleDashboardConfigRepository.save(config2);
    }

    @After
    public void tearDown() {
        CacheManager.getInstance().clearAll();
    }

    @Test
    public void testDeleteByRole() {
        roleDashboardConfigRepository.deleteByRole(role1);
        assertEquals(Long.valueOf(1), roleDashboardConfigRepository.count());
    }

    @Test
    public void testFindAllOrderByName() {
        List<RoleDashboardConfig> expected = new ArrayList<RoleDashboardConfig>();
        expected.add(config1);
        expected.add(config2);
        assertEquals(expected, roleDashboardConfigRepository.findAllOrderByName());
        verifyQueryCaching("RoleDashboardConfig.findAllRoleDashboards");
    }

    @Test
    public void testFindByRole() {
        assertEquals(config1, roleDashboardConfigRepository.findByRole(role1));
        verifyQueryCaching("RoleDashboardConfig.findDashboard");
    }

    @Test
    public void testFindByUser() {
        AuthzSubject bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob",
            "Bobbins", "Bob", "123123123", "123123123", false);
        entityManager.persist(bob);
        Role role3 = new Role();
        role3.setName("SysAdmin");
        role3.setSubjects(Arrays.asList(new AuthzSubject[] { bob }));
        entityManager.persist(role3);
        Role role4 = new Role();
        role4.setName("Manager");
        role4.setSubjects(Arrays.asList(new AuthzSubject[] { bob }));
        entityManager.persist(role4);
        RoleDashboardConfig config3 = new RoleDashboardConfig(role3, "Config3", config);
        roleDashboardConfigRepository.save(config3);
        RoleDashboardConfig config4 = new RoleDashboardConfig(role4, "Config4", config);
        roleDashboardConfigRepository.save(config4);
        List<RoleDashboardConfig> expected = new ArrayList<RoleDashboardConfig>();
        expected.add(config3);
        expected.add(config4);
        assertEquals(expected, roleDashboardConfigRepository.findByUser(bob));
    }

    private void verifyQueryCaching(String cacheName) {
        assertEquals(1, CacheManager.getInstance().getCache(cacheName).getSize());
    }
}
