/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.events.server.session;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.events.FileAlertConditionEvaluatorStateRepository;

import junit.framework.TestCase;

/**
 * Unit test of the {@link FileAlertConditionEvaluatorStateRepository}
 * @author jhickey
 *
 */
public class FileAlertConditionEvaluatorStateRepositoryTest
    extends TestCase
{

    private FileAlertConditionEvaluatorStateRepository alertConditionEvaluatorStateRepository;

    private File tempDir = new File(System.getProperty("java.io.tmpdir"));

    public void setUp() throws Exception {
        super.setUp();
        this.alertConditionEvaluatorStateRepository = new FileAlertConditionEvaluatorStateRepository(tempDir);
    }

    /**
     * Verifies an empty map is returned if there is no saved alert condition
     * evaluator state to read
     */
    public void testRetrieveAlertConditionEvaluatorStatesNoFile() {
        assertFalse(new File(tempDir, FileAlertConditionEvaluatorStateRepository.EVALUATOR_STATE_FILE_NAME).exists());
        assertTrue(alertConditionEvaluatorStateRepository.getAlertConditionEvaluatorStates().isEmpty());
    }

    /**
     * Verifies an empty map is returned if there is no saved execution strategy
     * state to read
     */
    public void testRetrieveExecutionStratStatesNoFile() {
        assertFalse(new File(tempDir, FileAlertConditionEvaluatorStateRepository.EXECUTION_STRATEGY_FILE_NAME).exists());
        assertTrue(alertConditionEvaluatorStateRepository.getExecutionStrategyStates().isEmpty());
    }

    /**
     * Verifies successful overwrite of state file if still exists for some
     * reason when save is called
     * @throws IOException
     */
    public void testSaveAlertConditionEvaluatorStatesFileExists() throws IOException {
        new File(tempDir, FileAlertConditionEvaluatorStateRepository.EVALUATOR_STATE_FILE_NAME).createNewFile();
        Integer alertDefinitionId = Integer.valueOf(1234);
        Integer alertDefinition2Id = Integer.valueOf(5678);
        Map<Integer, Serializable> alertConditionEvaluatorStates = new HashMap<Integer, Serializable>();
        alertConditionEvaluatorStates.put(alertDefinitionId, "Some State");
        alertConditionEvaluatorStates.put(alertDefinition2Id, Integer.valueOf(778));
        alertConditionEvaluatorStateRepository.saveAlertConditionEvaluatorStates(alertConditionEvaluatorStates);
        assertEquals(alertConditionEvaluatorStates,
                     alertConditionEvaluatorStateRepository.getAlertConditionEvaluatorStates());
        assertFalse(new File(tempDir, FileAlertConditionEvaluatorStateRepository.EVALUATOR_STATE_FILE_NAME).exists());
    }

    /**
     * Verifies successful save and get of AlertConditionEvaluator state and
     * deletion of file after get
     */
    public void testSaveAndRetrieveAlertConditionEvaluatorStates() {
        Integer alertDefinitionId = Integer.valueOf(1234);
        Integer alertDefinition2Id = Integer.valueOf(5678);
        Map<Integer, Serializable> alertConditionEvaluatorStates = new HashMap<Integer, Serializable>();
        alertConditionEvaluatorStates.put(alertDefinitionId, "Some State");
        alertConditionEvaluatorStates.put(alertDefinition2Id, Integer.valueOf(778));
        alertConditionEvaluatorStateRepository.saveAlertConditionEvaluatorStates(alertConditionEvaluatorStates);
        assertEquals(alertConditionEvaluatorStates,
                     alertConditionEvaluatorStateRepository.getAlertConditionEvaluatorStates());
        assertFalse(new File(tempDir, FileAlertConditionEvaluatorStateRepository.EVALUATOR_STATE_FILE_NAME).exists());
    }

    /**
     * Verifies successful save and get of ExecutionStrategy state and deletion
     * of file after get
     */
    public void testSaveAndRetrieveExecutionStrategyStates() {
        Integer alertDefinitionId = Integer.valueOf(1234);
        Integer alertDefinition2Id = Integer.valueOf(5678);
        Map<Integer, Serializable> executionStrategyStates = new HashMap<Integer, Serializable>();
        executionStrategyStates.put(alertDefinitionId, "Some State");
        executionStrategyStates.put(alertDefinition2Id, Integer.valueOf(778));
        alertConditionEvaluatorStateRepository.saveExecutionStrategyStates(executionStrategyStates);
        assertEquals(executionStrategyStates, alertConditionEvaluatorStateRepository.getExecutionStrategyStates());
        assertFalse(new File(tempDir, FileAlertConditionEvaluatorStateRepository.EXECUTION_STRATEGY_FILE_NAME).exists());
    }

    /**
     * Verifies successful overwrite of state file if still exists for some
     * reason when save is called
     * @throws IOException
     */
    public void testSaveExecutionStrategyStatesFileExists() throws IOException {
        new File(tempDir, FileAlertConditionEvaluatorStateRepository.EXECUTION_STRATEGY_FILE_NAME).createNewFile();
        Integer alertDefinitionId = Integer.valueOf(1234);
        Integer alertDefinition2Id = Integer.valueOf(5678);
        Map<Integer, Serializable> executionStrategyStates = new HashMap<Integer, Serializable>();
        executionStrategyStates.put(alertDefinitionId, "Some State");
        executionStrategyStates.put(alertDefinition2Id, Integer.valueOf(778));
        alertConditionEvaluatorStateRepository.saveExecutionStrategyStates(executionStrategyStates);
        assertEquals(executionStrategyStates, alertConditionEvaluatorStateRepository.getExecutionStrategyStates());
        assertFalse(new File(tempDir, FileAlertConditionEvaluatorStateRepository.EXECUTION_STRATEGY_FILE_NAME).exists());
    }

}
