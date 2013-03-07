package org.hyperic.hq.api.services.impl;

import org.hyperic.hq.api.services.AdminService;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.springframework.beans.factory.annotation.Autowired;

public class AdminServiceImpl implements AdminService {

    @Autowired
    private ServerConfigManager serverConfigManager;

    public String getHqServerGUID() throws SessionNotFoundException, SessionTimeoutException {
        return serverConfigManager.getGUID();
    }
}
