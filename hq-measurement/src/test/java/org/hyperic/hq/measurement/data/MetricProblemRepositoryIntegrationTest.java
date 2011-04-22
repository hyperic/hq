package org.hyperic.hq.measurement.data;

import java.util.HashSet;
import java.util.Set;

import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.MeasurementDataId;
import org.hyperic.hq.measurement.server.session.MetricProblem;
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
public class MetricProblemRepositoryIntegrationTest {

    @Autowired
    private MetricProblemRepository metricProblemRepository;

    @Test
    public void testDeleteByMetricIds() {
        long timestamp = System.currentTimeMillis();
        int measurementId = 123;
        int measurementId2 = 456;
        int measurementId3 = 789;
        MeasurementDataId id = new MeasurementDataId(measurementId, timestamp, 0);
        MetricProblem prob = new MetricProblem(id, MeasurementConstants.PROBLEM_TYPE_ALERT);
        metricProblemRepository.save(prob);
        MeasurementDataId id2 = new MeasurementDataId(measurementId2, timestamp, 0);
        MetricProblem prob2 = new MetricProblem(id2, MeasurementConstants.PROBLEM_TYPE_ALERT);
        metricProblemRepository.save(prob2);
        MeasurementDataId id3 = new MeasurementDataId(measurementId3, timestamp, 0);
        MetricProblem prob3 = new MetricProblem(id3, MeasurementConstants.PROBLEM_TYPE_ALERT);
        metricProblemRepository.save(prob3);
        Set<Integer> metricIds = new HashSet<Integer>();
        metricIds.add(measurementId);
        metricIds.add(measurementId3);
        metricProblemRepository.deleteByMetricIds(metricIds);
        assertEquals(Long.valueOf(1), metricProblemRepository.count());
    }
    
    @Test
    public void testDeleteByMetricIdsEmpty() {
        long timestamp = System.currentTimeMillis();
        int measurementId = 123;
        int measurementId2 = 456;
        int measurementId3 = 789;
        MeasurementDataId id = new MeasurementDataId(measurementId, timestamp, 0);
        MetricProblem prob = new MetricProblem(id, MeasurementConstants.PROBLEM_TYPE_ALERT);
        metricProblemRepository.save(prob);
        MeasurementDataId id2 = new MeasurementDataId(measurementId2, timestamp, 0);
        MetricProblem prob2 = new MetricProblem(id2, MeasurementConstants.PROBLEM_TYPE_ALERT);
        metricProblemRepository.save(prob2);
        MeasurementDataId id3 = new MeasurementDataId(measurementId3, timestamp, 0);
        MetricProblem prob3 = new MetricProblem(id3, MeasurementConstants.PROBLEM_TYPE_ALERT);
        metricProblemRepository.save(prob3);
        Set<Integer> metricIds = new HashSet<Integer>();
        metricProblemRepository.deleteByMetricIds(metricIds);
        assertEquals(Long.valueOf(3), metricProblemRepository.count());
    }
}
