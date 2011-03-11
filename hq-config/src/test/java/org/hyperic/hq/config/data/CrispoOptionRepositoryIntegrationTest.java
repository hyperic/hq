package org.hyperic.hq.config.data;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.config.domain.Crispo;
import org.hyperic.hq.config.domain.CrispoOption;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/config/data/jpa-integration-test-context.xml" })
public class CrispoOptionRepositoryIntegrationTest {

    @Autowired
    private CrispoOptionRepository crispoOptionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void testFindByArrayValue() {
        Crispo crispo = new Crispo();
        entityManager.persist(crispo);
        CrispoOption opt1 = new CrispoOption(crispo, "user.resources", "joe|bob|sam");
        crispoOptionRepository.save(opt1);
        CrispoOption opt2 = new CrispoOption(crispo, "firstname", "bob");
        crispoOptionRepository.save(opt2);
        CrispoOption opt3 = new CrispoOption(crispo, "email", "some@thing.com");
        crispoOptionRepository.save(opt3);
        List<CrispoOption> expected = new ArrayList<CrispoOption>();
        expected.add(opt1);
        expected.add(opt2);
        assertEquals(expected, crispoOptionRepository.findByValue("bob"));
    }

    @Test
    public void testFindByKeyLike() {
        Crispo crispo = new Crispo();
        entityManager.persist(crispo);
        CrispoOption opt1 = new CrispoOption(crispo, "username", "bob");
        crispoOptionRepository.save(opt1);
        CrispoOption opt2 = new CrispoOption(crispo, "userlastname", "smith");
        crispoOptionRepository.save(opt2);
        CrispoOption opt3 = new CrispoOption(crispo, "email", "some@thing.com");
        crispoOptionRepository.save(opt3);
        List<CrispoOption> expected = new ArrayList<CrispoOption>();
        expected.add(opt1);
        expected.add(opt2);
        assertEquals(expected, crispoOptionRepository.findByKeyLike("%user%"));
    }

    @Test
    public void testFindByValue() {
        Crispo crispo = new Crispo();
        entityManager.persist(crispo);
        CrispoOption opt1 = new CrispoOption(crispo, "username", "bob");
        crispoOptionRepository.save(opt1);
        CrispoOption opt2 = new CrispoOption(crispo, "firstname", "bob");
        crispoOptionRepository.save(opt2);
        CrispoOption opt3 = new CrispoOption(crispo, "email", "some@thing.com");
        crispoOptionRepository.save(opt3);
        List<CrispoOption> expected = new ArrayList<CrispoOption>();
        expected.add(opt1);
        expected.add(opt2);
        assertEquals(expected, crispoOptionRepository.findByValue("bob"));
    }
}
