package org.hyperic.hq.measurement.data;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.measurement.server.session.MonitorableType;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/measurement/data/jpa-integration-test-context.xml" })
public class MonitorableTypeRepositoryIntegrationTest {

    @Autowired
    private MonitorableTypeRepository monitorableTypeRepository;

    @Test
    public void testFindByPluginName() {
        MonitorableType type1 = new MonitorableType("Tomcat Server", "tomcat");
        monitorableTypeRepository.save(type1);
        MonitorableType type2 = new MonitorableType("WebLogic Server", "weblogic");
        monitorableTypeRepository.save(type2);
        List<MonitorableType> expected = new ArrayList<MonitorableType>();
        expected.add(type2);
        assertEquals(expected, monitorableTypeRepository.findByPluginName("weblogic"));
    }

}
