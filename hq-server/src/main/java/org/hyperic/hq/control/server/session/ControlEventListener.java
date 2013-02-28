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

package org.hyperic.hq.control.server.session;

import java.util.HashSet;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.control.shared.ControlManager;
import org.hyperic.hq.control.shared.ControlScheduleManager;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ControlEventListener implements ZeventListener<ResourceZevent> {

    private final Log _log = LogFactory.getLog(ControlEventListener.class);
    private ControlScheduleManager controlScheduleManager;
    private AuthzSubjectManager authzSubjectManager;
    private ZeventEnqueuer zEventManager;
    private ControlManager controlManager;

    @Autowired
    public ControlEventListener(ControlScheduleManager controlScheduleManager,
                                AuthzSubjectManager authzSubjectManager,
                                ZeventEnqueuer zEventManager, ControlManager controlManager) {
        this.controlScheduleManager = controlScheduleManager;
        this.authzSubjectManager = authzSubjectManager;
        this.zEventManager = zEventManager;
        this.controlManager = controlManager;
    }

    @PostConstruct
    public void subscribe() {
        // Add listener to remove scheduled control actions after resources
        // are deleted.
        HashSet<Class<? extends Zevent>> events = new HashSet<Class<? extends Zevent>>();
        events.add(ResourceDeletedZevent.class);
        zEventManager.addBufferedListener(events, this);
    }

    public void processEvents(List<ResourceZevent> events) {
        for (ResourceZevent z : events) {
            AuthzSubject subject = authzSubjectManager.findSubjectById(z.getAuthzSubjectId());
            AppdefEntityID id = z.getAppdefEntityID();

            _log.info("Removing scheduled jobs and control history for " + id);
            try {
                controlScheduleManager.removeScheduledJobs(subject, id);
                controlManager.removeControlHistory(id);
            } catch (Exception e) {
                _log.error("Unable to remove scheduled jobs or control history for " + id, e);
            }
        }
    }

    public String toString() {
        return "ControlEventListener";
    }
}
