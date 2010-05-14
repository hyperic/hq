package org.hyperic.hq.appdef.server.session;

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

}
