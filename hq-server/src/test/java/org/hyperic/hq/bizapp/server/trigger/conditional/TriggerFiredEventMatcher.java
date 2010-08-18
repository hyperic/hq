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

import java.util.Arrays;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.hyperic.hq.events.TriggerFiredEvent;

/**
 * Implementation of {@link IArgumentMatcher} that compares actual and expected
 * {@link TriggerFiredEvent}s by identity, message, instance id, and underlying
 * events
 * @author jhickey
 *
 */
public class TriggerFiredEventMatcher implements IArgumentMatcher {

    /**
     *
     * @param in The expected event
     * @return null
     */
    public static TriggerFiredEvent eqTriggerFiredEvent(TriggerFiredEvent in) {
        EasyMock.reportMatcher(new TriggerFiredEventMatcher(in));
        return null;
    }

    private TriggerFiredEvent expected;

    /**
     *
     * @param expected The expected {@link TriggerFiredEvent}
     */
    public TriggerFiredEventMatcher(TriggerFiredEvent expected) {
        this.expected = expected;
    }

    public void appendTo(StringBuffer buffer) {
        buffer.append("eqTriggerFiredEvent(");
        buffer.append(expected.getClass().getName());
        buffer.append(" with message \"");
        buffer.append(expected.getMessage());
        buffer.append(" with id \"");
        buffer.append(expected.getId());
        buffer.append(" with instance id \"");
        buffer.append(expected.getInstanceId());
        buffer.append("\")");
    }

    public boolean matches(Object actual) {
        if (!(expected.equals(actual))) {
            return false;
        }
        TriggerFiredEvent actualEvent = (TriggerFiredEvent) actual;
        if (expected.getMessage() == null) {
            if (actualEvent.getMessage() == null) {
                return true;
            }
            return false;
        }
        if (!(expected.getMessage().equals(((TriggerFiredEvent) actual).getMessage()))) {
            return false;
        }
        if (!(expected.getInstanceId().equals(actualEvent.getInstanceId()))) {
            return false;
        }
        return Arrays.equals(expected.getEvents(), actualEvent.getEvents());
    }

}