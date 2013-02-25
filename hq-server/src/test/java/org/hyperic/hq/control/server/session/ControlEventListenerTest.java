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

package org.hyperic.hq.control.server.session;

import java.util.List;
import java.util.Collections;
import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.control.shared.ControlManager;
import org.hyperic.hq.control.shared.ControlScheduleManager;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventManager;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;


public class ControlEventListenerTest
    extends TestCase
{
    private IMocksControl mocksControl;
    private ControlEventListener tested;
    private ControlScheduleManager controlScheduleManager;
    private AuthzSubjectManager authzSubjectManager;
    private ZeventEnqueuer zEventManager;
    private ControlManager controlManager;
    private AuthzSubject authzSubject;
    


    public void setUp() throws Exception {
        super.setUp();
        mocksControl = EasyMock.createControl();
        this.controlScheduleManager = mocksControl.createMock(ControlScheduleManager.class);
        this.authzSubjectManager = mocksControl.createMock(AuthzSubjectManager.class);
        this.zEventManager = mocksControl.createMock(ZeventManager.class);
        this.controlManager = mocksControl.createMock(ControlManager.class);
        this.authzSubject = mocksControl.createMock(AuthzSubject.class);
        this.tested = new ControlEventListener(controlScheduleManager, authzSubjectManager,
                zEventManager, controlManager);
    }

    public void testProcessEvents() throws Exception {
        AppdefEntityID serverEntityId = new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVER, 10026);
        ResourceZevent event = mocksControl.createMock(ResourceZevent.class);
        List<ResourceZevent> events = Collections.singletonList(event);
        
        Integer subjectId = new Integer(1);
        EasyMock.expect(event.getAuthzSubjectId()).andReturn(subjectId);
        expect(authzSubjectManager.findSubjectById(subjectId)).andReturn(authzSubject);
        expect(event.getAppdefEntityID()).andReturn(serverEntityId);
        
        controlScheduleManager.removeScheduledJobs(authzSubject, serverEntityId);
        expectLastCall();
        controlManager.removeControlHistory(serverEntityId);
        expectLastCall();
        
        mocksControl.replay();
        tested.processEvents(events);
        mocksControl.verify();
    }


}
