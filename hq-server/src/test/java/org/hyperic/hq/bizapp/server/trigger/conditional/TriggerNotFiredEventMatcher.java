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

package org.hyperic.hq.bizapp.server.trigger.conditional;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.hyperic.hq.events.TriggerNotFiredEvent;

/**
 * Implementation of {@link IArgumentMatcher} that compares actual and expected
 * {@link TriggerNotFiredEvent} by identity, instance ID, and timestamp
 * @author jhickey
 *
 */
public class TriggerNotFiredEventMatcher implements IArgumentMatcher {
    /**
     *
     * @param in The expected event
     * @return null
     */
    public static TriggerNotFiredEvent eqTriggerNotFiredEvent(TriggerNotFiredEvent in) {
        EasyMock.reportMatcher(new TriggerNotFiredEventMatcher(in));
        return null;
    }

    private TriggerNotFiredEvent expected;

    /**
     *
     * @param expected The expected {@link TriggerNotFiredEvent}
     */
    public TriggerNotFiredEventMatcher(TriggerNotFiredEvent expected) {
        this.expected = expected;
    }

    public void appendTo(StringBuffer buffer) {
        buffer.append("eqTriggerNotFiredEvent(");
        buffer.append(expected.getClass().getName());
        buffer.append(" with instance id \"");
        buffer.append(expected.getInstanceId());
        buffer.append(" with timestamp \"");
        buffer.append(expected.getTimestamp());
        buffer.append("\")");

    }

    public boolean matches(Object actual) {
        if (!(expected.equals(actual))) {
            return false;
        }
        TriggerNotFiredEvent actualEvent = (TriggerNotFiredEvent) actual;
        if (!(expected.getInstanceId().equals(actualEvent.getInstanceId()))) {
            return false;
        }
        return expected.getTimestamp() == actualEvent.getTimestamp();
    }

}
