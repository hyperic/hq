package org.hyperic.hq.hqu.data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.hqu.ViewDescriptor;
import org.hyperic.hq.hqu.server.session.AttachType;
import org.hyperic.hq.hqu.server.session.Attachment;
import org.hyperic.hq.hqu.server.session.UIPlugin;
import org.hyperic.hq.hqu.server.session.ViewAdmin;
import org.hyperic.hq.hqu.server.session.ViewMasthead;
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
public class AttachmentRepositoryIntegrationTest {

    @Autowired
    private AttachmentRepository attachmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void testFindFor() {
        UIPlugin plugin = new UIPlugin("mass", "2.0");
        entityManager.persist(plugin);
        ViewAdmin view = new ViewAdmin(plugin, new ViewDescriptor("/foo", "desciptor",
            AttachType.ADMIN));
        entityManager.persist(view);
        Attachment attachment = new Attachment(view);
        attachmentRepository.save(attachment);

        ViewMasthead viewMasthead = new ViewMasthead(plugin, new ViewDescriptor("/foo/bar",
            "desciptor", AttachType.MASTHEAD));
        entityManager.persist(viewMasthead);
        Attachment attachment2 = new Attachment(viewMasthead);
        attachmentRepository.save(attachment2);

        List<Attachment> expected = new ArrayList<Attachment>();
        expected.add(attachment);

        assertEquals(expected, attachmentRepository.findFor(AttachType.ADMIN.getCode()));
    }

}
