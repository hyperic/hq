package org.hyperic.hq.galert.data;

import org.hyperic.hq.galerts.server.session.GtriggerTypeInfo;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/galert/data/jpa-integration-test-context.xml" })
public class GtriggerTypeInfoRepositoryIntegrationTest {

    @Autowired
    private GtriggerTypeInfoRepository gtriggerTypeInfoRepository;

    @Test
    public void testFindByType() {
        GtriggerTypeInfo typeInfo1 = new GtriggerTypeInfo(String.class);
        gtriggerTypeInfoRepository.save(typeInfo1);
        GtriggerTypeInfo typeInfo2 = new GtriggerTypeInfo(Long.class);
        gtriggerTypeInfoRepository.save(typeInfo2);
        assertEquals(typeInfo1, gtriggerTypeInfoRepository.findByType(String.class));
    }

}
