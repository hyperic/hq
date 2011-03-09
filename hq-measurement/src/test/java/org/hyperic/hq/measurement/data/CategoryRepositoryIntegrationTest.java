package org.hyperic.hq.measurement.data;

import org.hyperic.hq.measurement.server.session.Category;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/measurement/data/jpa-integration-test-context.xml" })
public class CategoryRepositoryIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    public void testFindByName() {
        Category category = new Category("Availability");
        categoryRepository.save(category);
        Category category2 = new Category("Performance");
        categoryRepository.save(category2);
        assertEquals(category2, categoryRepository.findByName("Performance"));
    }

    @Test
    public void testFindByNameNotFound() {
        assertNull(categoryRepository.findByName("Performance"));
    }
}
