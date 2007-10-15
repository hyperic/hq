package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.zevents.ZeventManager;

/**
 * A ResourceRefresh event indicates that the agent that monitors this resource
 * has been re-initialized.
 */
public class ResourceRefreshZevent extends ResourceZevent {

    static {
        ZeventManager.getInstance().
            registerEventClass(ResourceRefreshZevent.class);
    }

    public ResourceRefreshZevent(AuthzSubjectValue subject, AppdefEntityID id) {
        super(subject, id);
    }
}
