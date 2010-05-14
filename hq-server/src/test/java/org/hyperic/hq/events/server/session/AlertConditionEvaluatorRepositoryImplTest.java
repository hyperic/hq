package org.hyperic.hq.events.server.session;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hq.events.AlertConditionEvaluatorStateRepository;
/**
 * Unit test of the {@link AlertConditionEvaluatorRepositoryImpl}
 * @author jhickey
 *
 */
public class AlertConditionEvaluatorRepositoryImplTest extends TestCase {
    private AlertConditionEvaluatorRepositoryImpl alertConditionEvaluatorRepository;
    private AlertConditionEvaluatorStateRepository alertEvaluatorStateRepository;
    private AlertConditionEvaluator alertConditionEvaluator;
    private ExecutionStrategy executionStrategy;

    private void replay() {
        EasyMock.replay(alertConditionEvaluator, alertEvaluatorStateRepository, executionStrategy);
    }

    public void setUp() throws Exception {
        super.setUp();
        this.alertEvaluatorStateRepository = EasyMock.createMock(AlertConditionEvaluatorStateRepository.class);
        this.alertConditionEvaluator = EasyMock.createMock(AlertConditionEvaluator.class);
        this.executionStrategy = EasyMock.createMock(ExecutionStrategy.class);
        this.alertConditionEvaluatorRepository = new AlertConditionEvaluatorRepositoryImpl(alertEvaluatorStateRepository);
    }

    /**
     * Verifies successful add to the repository
     */
    public void testAddAlertConditionEvaluator() {
        Integer alertDefinitionId = Integer.valueOf(1234);
        EasyMock.expect(alertConditionEvaluator.getAlertDefinitionId()).andReturn(alertDefinitionId);
        replay();
        alertConditionEvaluatorRepository.addAlertConditionEvaluator(alertConditionEvaluator);
        assertEquals(alertConditionEvaluator,alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId));
        verify();
    }

    /**
     * Verifies successful remove from the repository
     */
    public void testRemoveAlertConditionEvaluator() {
        Integer alertDefinitionId = Integer.valueOf(1234);
        EasyMock.expect(alertConditionEvaluator.getAlertDefinitionId()).andReturn(alertDefinitionId);
        replay();
        alertConditionEvaluatorRepository.addAlertConditionEvaluator(alertConditionEvaluator);
        assertEquals(alertConditionEvaluator,alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId));
        alertConditionEvaluatorRepository.removeAlertConditionEvaluator(alertDefinitionId);
        assertNull(alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId));
        verify();
    }

    /**
     * Verifies state of {@link AlertConditionEvaluator}s and {@link ExecutionStrategy}s is persisted on shutdown
     */
    public void testShutdownEvaluatorExecStrategyWithState() {
        Integer alertDefinitionId = Integer.valueOf(1234);
        EasyMock.expect(alertConditionEvaluator.getAlertDefinitionId()).andReturn(alertDefinitionId).times(3);
        EasyMock.expect(alertConditionEvaluator.getState()).andReturn("Some State");
        Map<Integer, Serializable> expectedState = new HashMap<Integer, Serializable>();
        expectedState.put(alertDefinitionId, "Some State");
        EasyMock.expect(alertConditionEvaluator.getExecutionStrategy()).andReturn(executionStrategy);
        EasyMock.expect(executionStrategy.getState()).andReturn("More State");
        Map<Integer, Serializable> expectedExStratState= new HashMap<Integer, Serializable>();
        expectedExStratState.put(alertDefinitionId, "More State");
        alertEvaluatorStateRepository.saveAlertConditionEvaluatorStates(expectedState);
        alertEvaluatorStateRepository.saveExecutionStrategyStates(expectedExStratState);
        replay();
        alertConditionEvaluatorRepository.addAlertConditionEvaluator(alertConditionEvaluator);
        alertConditionEvaluatorRepository.shutdown();
        verify();
    }

    /**
     * Verifies nothing is persisted on shutdown if evaluators and execution strategies have no state
     */
    public void testShutdownNoState() {
        Integer alertDefinitionId = Integer.valueOf(1234);
        EasyMock.expect(alertConditionEvaluator.getAlertDefinitionId()).andReturn(alertDefinitionId);
        EasyMock.expect(alertConditionEvaluator.getState()).andReturn(null);
        EasyMock.expect(alertConditionEvaluator.getExecutionStrategy()).andReturn(executionStrategy);
        EasyMock.expect(executionStrategy.getState()).andReturn(null);
        replay();
        alertConditionEvaluatorRepository.addAlertConditionEvaluator(alertConditionEvaluator);
        alertConditionEvaluatorRepository.shutdown();
        verify();
    }

    private void verify() {
        EasyMock.verify(alertConditionEvaluator, alertEvaluatorStateRepository, executionStrategy);
    }
}
