package org.hyperic.hq.auth.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.auth.domain.Role;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/auth/data/jpa-integration-test-context.xml" })
public class RoleRepositoryIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    public void testFindByName() {
        Role role1 = new Role();
        role1.setName("Role1");
        roleRepository.save(role1);
        Role role2 = new Role();
        role2.setName("Role2");
        roleRepository.save(role2);
        assertEquals(role2, roleRepository.findByName("Role2"));
    }

    @Test
    public void testFindByNameNone() {
        assertNull(roleRepository.findByName("Role2"));
    }

    @Test
    public void testFindBySystem() {
        Role role1 = new Role();
        role1.setName("Role1");
        role1.setSystem(true);
        roleRepository.save(role1);
        Role role2 = new Role();
        role2.setName("Role2");
        roleRepository.save(role2);
        Role role3 = new Role();
        role3.setName("Role3");
        role3.setSystem(true);
        roleRepository.save(role3);
        List<Role> expected = new ArrayList<Role>();
        expected.add(role1);
        expected.add(role3);
        assertEquals(expected, roleRepository.findBySystem(true, new Sort("name")));
    }
}
