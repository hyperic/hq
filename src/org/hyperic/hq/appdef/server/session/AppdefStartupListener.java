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

package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.galerts.ResourceAuxLogProvider;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceDeleteCallback;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.zevents.ZeventManager;

public class AppdefStartupListener
    implements StartupListener
{
    private static final Object LOCK = new Object();
    
    private static ClusterDeleteCallback _clusterDelCallback;
    private static AgentCreateCallback   _agentCreateCallback;
    
    public void hqStarted() {
        // Make sure we have the aux-log provider loaded
        ResourceAuxLogProvider.class.toString();

        HQApp app = HQApp.getInstance();

        synchronized (LOCK) {
            _clusterDelCallback = (ClusterDeleteCallback)
                app.registerCallbackCaller(ClusterDeleteCallback.class);
            _agentCreateCallback = (AgentCreateCallback)
                app.registerCallbackCaller(AgentCreateCallback.class);
            app.registerCallbackListener(ResourceDeleteCallback.class,
                                         new ResourceDeleteCallback() {

                public void preResourceDelete(Resource r)
                    throws VetoException {
                    switch (r.getResourceType().getAppdefType()) {
                    case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                        PlatformManagerEJBImpl.getOne().handleResourceDelete(r);
                        break;
                    case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                        ServerManagerEJBImpl.getOne().handleResourceDelete(r);
                        break;
                    case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                        ServiceManagerEJBImpl.getOne().handleResourceDelete(r);
                        break;
                    case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                        ApplicationManagerEJBImpl.getOne()
                            .handleResourceDelete(r);
                        break;
                    }
                }
            });
        }
        ApplicationManagerEJBImpl.getOne().startup();
        
        registerTransferAgentBundleZeventListener();
    }
    
    static ClusterDeleteCallback getClusterDeleteCallback() {
        synchronized (LOCK) {
            return _clusterDelCallback;
        }
    }
    
    static AgentCreateCallback getAgentCreateCallback() {
        synchronized (LOCK) {
            return _agentCreateCallback;
        }
    }
    
    private void registerTransferAgentBundleZeventListener() {
        ZeventManager.getInstance()
        .addBufferedListener(TransferAgentBundleZevent.class,
                             new TransferAgentBundleZeventListener());
    }

}
