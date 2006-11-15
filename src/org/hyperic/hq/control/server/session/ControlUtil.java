package org.hyperic.hq.control.server.session;

import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AgentConnectionUtil;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.control.agent.client.ControlCommandsClient;

public abstract class ControlUtil {

    /**
     * Get a control command client based on an appdefentity
     */
    public static ControlCommandsClient getClient(AppdefEntityID aid) 
        throws PermissionException, AgentNotFoundException {
        return new ControlCommandsClient(AgentConnectionUtil.getClient(aid));
    }
}
