/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.events.ext;

import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.jmock.core.Verifiable;
import org.jmock.expectation.ExpectationCounter;

/**
 * A trigger fire strategy where expectations can be set as to how many times 
 * actions are fired.
 */
public class MockTriggerFireStrategy 
    implements TriggerFireStrategy, Verifiable {
    
    private final ExpectationCounter _expectedNumCalls = 
        new ExpectationCounter("number of times actions fired");
    
    public MockTriggerFireStrategy() {
    }
    
    /**
     * Set the expected number of times we expect that the trigger will fire 
     * the actions.
     * 
     * @param num The expected number of times.
     */
    public void setExpectedNumTimesActionsFired(int num) {
        _expectedNumCalls.setExpected(num);
    }
    
    /**
     * @see org.hyperic.hq.events.ext.TriggerFireStrategy#fireActions(org.hyperic.hq.events.TriggerFiredEvent)
     */
    public void fireActions(TriggerFiredEvent event)
            throws ActionExecuteException, AlertCreateException {
        _expectedNumCalls.inc();
    }

    /**
     * Verify the expectations.
     * 
     * @see org.jmock.core.Verifiable#verify()
     */
    public void verify() {
        _expectedNumCalls.verify();
    }

}
