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

package org.hyperic.hq.events.ext;

import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.server.session.AlertConditionEvaluator;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.config.ConfigSchema;

/**
 * Mock trigger for testing RegisteredTriggers. Uses a static variable to
 * confirm that it has been initialized and/or enabled (since instantiation
 * happens via reflection), so can't be used more than once in a VM
 * @author jhickey
 *
 */
public class MockTrigger implements RegisterableTriggerInterface {
    /** Indicates that init has been called **/
    public static boolean initialized = false;
    /** Mock return value for isEnabled or indicator that setEnabled was called **/
    public static boolean enabled = true;

    public ConfigSchema getConfigSchema() {
        return null;
    }

    public Class<?>[] getInterestedEventTypes() {
        return new Class[] { MockEvent.class };
    }

    public Integer[] getInterestedInstanceIDs(Class<?> c) {
        return new Integer[] { 123, 456 };
    }

    public Integer getId() {
        return 3;
    }

    public void init(RegisteredTriggerValue trigger, AlertConditionEvaluator alertConditionEvaluator) throws InvalidTriggerDataException
    {
        MockTrigger.initialized = true;
    }

    public boolean isEnabled() {
        return MockTrigger.enabled;
    }

    public void setEnabled(boolean enabled) {
        MockTrigger.enabled = enabled;
    }

}
