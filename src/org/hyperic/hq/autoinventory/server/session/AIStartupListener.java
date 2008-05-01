/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.autoinventory.server.session;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.zevents.ZeventManager;

public class AIStartupListener
    implements StartupListener
{
    public void hqStarted() {

        /**
         * Add the runtime-AI listener to enable resources for runtime
         * autodiscovery as they are created.
         */
        Set events = new HashSet();
        events.add(ResourceCreatedZevent.class);
        events.add(ResourceUpdatedZevent.class);
        ZeventManager.getInstance().
            addBufferedListener(events, new RuntimeAIEnabler());
        
        events = new HashSet();
        events.add(MergeServiceReportZevent.class);
        ZeventManager.getInstance().addBufferedListener(events, 
                                                        new ServiceMerger());
        AutoinventoryManagerEJBImpl.getOne().startup();
    }

    /**
     * Listener class that enables runtime-AI on newly created resources.
     */
    private class RuntimeAIEnabler implements ZeventListener {

        public void processEvents(List events) {
            AutoinventoryManagerEJBImpl.getOne().handleResourceEvents(events);
        }
        
        public String toString() {
            return "RuntimeAIEnabler";
        }
    }
}
