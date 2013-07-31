package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.authz.server.session.Resource;

public interface AimPlatformBridge {

    /**
     * @param newResource
     *            - the newly created resource
     * @param parentResource
     *            - the parent of the new resource
     */
    void resourceCreated(Resource newResource, Resource parentResource);

}
