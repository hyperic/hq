package org.hyperic.hq.autoinventory.data;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.autoinventory.AIIp;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/autoinventory/data/jpa-integration-test-context.xml" })
public class AIIpRepositoryIntegrationTest {
    
    @Autowired
    private AIIpRepository aiIpRepository;
    
    @Test
    public void testFindByAddress() {
        AIIp ip = new AIIp();
        ip.setAddress("127.0.0.1");
        aiIpRepository.save(ip);
        AIIp ip2 = new AIIp();
        ip2.setAddress("127.0.0.3");
        List<AIIp> expected = new ArrayList<AIIp>();
        expected.add(ip);
        assertEquals(expected,aiIpRepository.findByAddress("127.0.0.1"));
    }
    
    @Test
    public void testFindByMacAddress() {
        AIIp ip = new AIIp();
        ip.setMacAddress("12345");
        ip.setAddress("127.0.0.1");
        aiIpRepository.save(ip);
        AIIp ip2 = new AIIp();
        ip2.setMacAddress("789");
        ip2.setAddress("127.0.0.1");
        List<AIIp> expected = new ArrayList<AIIp>();
        expected.add(ip);
        assertEquals(expected,aiIpRepository.findByMacAddress("12345")); 
    }
}
