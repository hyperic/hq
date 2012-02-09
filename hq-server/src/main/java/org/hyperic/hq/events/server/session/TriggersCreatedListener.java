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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.hyperic.hq.events.shared.RegisteredTriggerManager;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/**
 * Listens for trigger creation events
 * @author jhickey
 *
 */
@Component
public class TriggersCreatedListener implements ZeventListener<TriggersCreatedZevent> {

   
    private RegisteredTriggerManager registeredTriggerManager;
    
    private ZeventEnqueuer zEventManager;
    
    
    @Autowired
    public TriggersCreatedListener(RegisteredTriggerManager registeredTriggerManager,
                                   ZeventEnqueuer zEventManager) {
        this.registeredTriggerManager = registeredTriggerManager;
        this.zEventManager = zEventManager;
    }

    @PostConstruct
    public void subscribe() {
        Set<Class<? extends Zevent>> triggerEvents = new HashSet<Class<? extends Zevent>>();
        triggerEvents.add(TriggersCreatedZevent.class);

        zEventManager.addBufferedListener(triggerEvents, this);
    }
    
    public void processEvents(List<TriggersCreatedZevent> events) {
      registeredTriggerManager.handleTriggerCreatedEvents(events);
    }

}
