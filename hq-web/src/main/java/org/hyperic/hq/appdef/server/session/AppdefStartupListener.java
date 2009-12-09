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

import javax.annotation.PostConstruct;

import org.hyperic.hq.appdef.galerts.ResourceAuxLogProvider;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.authz.server.session.AuthzStartupListener;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceDeleteCallback;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class AppdefStartupListener
    implements StartupListener
{
    private static final Object LOCK = new Object();
    
    private static AgentCreateCallback   _agentCreateCallback;
    
    private HQApp app;
    private PlatformManager platformManager;
    private ServerManager serverManager;
    private ServiceManager serviceManager;
    private ApplicationManager applicationManager;
    private ZeventEnqueuer zEventManager;
    
    
    
    @Autowired
    public AppdefStartupListener(HQApp app, PlatformManager platformManager, ServerManager serverManager,
                                 ServiceManager serviceManager, ApplicationManager applicationManager,
                                 ZeventEnqueuer zEventManager, AuthzStartupListener authzStartupListener) {
        this.app = app;
        this.platformManager = platformManager;
        this.serverManager = serverManager;
        this.serviceManager = serviceManager;
        this.applicationManager = applicationManager;
        this.zEventManager = zEventManager;
        //TODO AuthzStartupListener has to be initialized first to register the ResourceDeleteCallback handler.  Injecting the listener here purely to wait for that
    }

    @PostConstruct
    public void hqStarted() {
        // Make sure we have the aux-log provider loaded
        ResourceAuxLogProvider.class.toString();

       

        synchronized (LOCK) {
            _agentCreateCallback = (AgentCreateCallback)
                app.registerCallbackCaller(AgentCreateCallback.class);
            app.registerCallbackListener(ResourceDeleteCallback.class,
                                         new ResourceDeleteCallback() {

                public void preResourceDelete(Resource r)
                    throws VetoException {
                    // Go ahead and let every appdef type handle a resource
                    // delete
                    platformManager.handleResourceDelete(r);
                    serverManager.handleResourceDelete(r);
                    serviceManager.handleResourceDelete(r);
                    applicationManager.handleResourceDelete(r);
                }
            });
        }
      
        
        registerTransferAgentBundleZeventListener();
        registerTransferAgentPluginZeventListener();
        registerUpgradeAgentZeventListener();
    }
    
    static AgentCreateCallback getAgentCreateCallback() {
        synchronized (LOCK) {
            return _agentCreateCallback;
        }
    }
    
    private void registerTransferAgentBundleZeventListener() {
        zEventManager
        .addBufferedListener(TransferAgentBundleZevent.class,
                             new TransferAgentBundleZeventListener());
    }
    
    private void registerTransferAgentPluginZeventListener() {
        zEventManager
        .addBufferedListener(TransferAgentPluginZevent.class,
                             new TransferAgentPluginZeventListener());
    }
    
    private void registerUpgradeAgentZeventListener() {
        zEventManager
        .addBufferedListener(UpgradeAgentZevent.class,
                             new UpgradeAgentZeventListener());
    }
    
}
