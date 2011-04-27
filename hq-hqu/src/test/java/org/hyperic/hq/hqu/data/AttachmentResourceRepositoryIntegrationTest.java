package org.hyperic.hq.hqu.data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.hqu.ViewDescriptor;
import org.hyperic.hq.hqu.server.session.AttachType;
import org.hyperic.hq.hqu.server.session.Attachment;
import org.hyperic.hq.hqu.server.session.AttachmentResource;
import org.hyperic.hq.hqu.server.session.UIPlugin;
import org.hyperic.hq.hqu.server.session.ViewResource;
import org.hyperic.hq.hqu.server.session.ViewResourceCategory;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/hqu/data/jpa-integration-test-context.xml" })
public class AttachmentResourceRepositoryIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AttachmentResourceRepository attachmentResourceRepository;

    @Test
    public void testFindByResourceAndCategory() {
        UIPlugin plugin = new UIPlugin("mass", "2.0");
        entityManager.persist(plugin);
        int resource = 123;
        ViewResource view = new ViewResource(plugin, new ViewDescriptor("/foo", "desciptor",
            AttachType.RESOURCE));
        entityManager.persist(view);
        AttachmentResource attachment = new AttachmentResource(view, ViewResourceCategory.VIEWS,
            resource);
        attachmentResourceRepository.save(attachment);

        int resource2 = 456;
        ViewResource view2 = new ViewResource(plugin, new ViewDescriptor("/foo/bar", "desciptor",
            AttachType.RESOURCE));
        entityManager.persist(view2);
        AttachmentResource attachment2 = new AttachmentResource(view2, ViewResourceCategory.VIEWS,
            resource2);
        attachmentResourceRepository.save(attachment2);

        List<Attachment> expected = new ArrayList<Attachment>();
        expected.add(attachment);

        assertEquals(
            expected,
            attachmentResourceRepository.findByResourceAndCategory(resource,
                ViewResourceCategory.VIEWS.getDescription()));
    }
}
