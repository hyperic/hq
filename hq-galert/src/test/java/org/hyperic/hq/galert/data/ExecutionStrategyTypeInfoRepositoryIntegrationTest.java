package org.hyperic.hq.galert.data;

import org.hyperic.hq.galerts.server.session.ExecutionStrategyTypeInfo;
import org.hyperic.hq.galerts.strategies.NoneStrategyType;
import org.hyperic.hq.galerts.strategies.SimpleStrategyType;
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
public class ExecutionStrategyTypeInfoRepositoryIntegrationTest {

    @Autowired
    private ExecutionStrategyTypeInfoRepository executionStrategyTypeInfoRepository;

    @Test
    public void testFindByType() {
        ExecutionStrategyTypeInfo strategy1 = new ExecutionStrategyTypeInfo();
        strategy1.setTypeClass(NoneStrategyType.class);
        executionStrategyTypeInfoRepository.save(strategy1);
        ExecutionStrategyTypeInfo strategy2 = new ExecutionStrategyTypeInfo();
        strategy2.setTypeClass(SimpleStrategyType.class);
        executionStrategyTypeInfoRepository.save(strategy2);
        assertEquals(strategy1,
            executionStrategyTypeInfoRepository.findByType(NoneStrategyType.class));

    }

}
