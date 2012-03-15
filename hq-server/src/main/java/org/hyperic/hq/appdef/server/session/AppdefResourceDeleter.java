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

package org.hyperic.hq.appdef.server.session;

import javax.annotation.PreDestroy;

import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.authz.server.session.ResourceDeleteRequestedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
/**
 * Notifies managers of a pending resource delete
 * @author jhickey
 *
 */
@Component
public class AppdefResourceDeleter implements ApplicationListener<ResourceDeleteRequestedEvent> {

    private PlatformManager platformManager;
    private ServerManager serverManager;
    private ServiceManager serviceManager;
    private ApplicationManager applicationManager;

    @Autowired
    public AppdefResourceDeleter(PlatformManager platformManager, ServerManager serverManager,
                                 ServiceManager serviceManager,
                                 ApplicationManager applicationManager) {
        this.platformManager = platformManager;
        this.serverManager = serverManager;
        this.serviceManager = serviceManager;
        this.applicationManager = applicationManager;
    }

    public void onApplicationEvent(ResourceDeleteRequestedEvent event) {
        // Go ahead and let every appdef type handle a resource
        // delete
        platformManager.handleResourceDelete(event.getResource());
        serverManager.handleResourceDelete(event.getResource());
        serviceManager.handleResourceDelete(event.getResource());
        applicationManager.handleResourceDelete(event.getResource());
    }
    
    @PreDestroy
    public final void destroy() { 
        this.platformManager = null ; 
        this.serverManager = null ; 
        this.serviceManager = null ; 
        this.applicationManager = null ;
    }//EOM 

}
