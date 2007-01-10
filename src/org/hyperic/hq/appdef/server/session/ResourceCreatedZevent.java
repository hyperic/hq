package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;

public class ResourceCreatedZevent extends ResourceZevent {

     static {
        ZeventManager.getInstance().
            registerEventClass(ResourceCreatedZevent.class);
    }

    public ResourceCreatedZevent(AuthzSubjectValue subject, AppdefEntityID id) {
        super(subject, id);
    }
}
