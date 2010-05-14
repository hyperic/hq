/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.bizapp.server.session;

import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.LiveDataBoss;
import org.hyperic.hq.livedata.shared.LiveDataCommand;
import org.hyperic.hq.livedata.shared.LiveDataException;
import org.hyperic.hq.livedata.shared.LiveDataManager;
import org.hyperic.hq.livedata.shared.LiveDataResult;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * External API into the live data system.
 */

@Service
@Transactional
public class LiveDataBossImpl implements LiveDataBoss {
    private SessionManager sessionManager;
    private LiveDataManager liveDataManager;

    @Autowired
    public LiveDataBossImpl(SessionManager sessionManager, LiveDataManager liveDataManager) {
        this.sessionManager = sessionManager;
        this.liveDataManager = liveDataManager;
    }

    /**
     * Get live data for a given resource
     */
    @Transactional(readOnly=true)
    public LiveDataResult getLiveData(int sessionId, LiveDataCommand command)
        throws PermissionException, AgentNotFoundException,
        AppdefEntityNotFoundException, LiveDataException,
        SessionTimeoutException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return liveDataManager.getData(subject, command);
    }

    /**
     * Get live data for the given commands
     */
    @Transactional(readOnly=true)
    public LiveDataResult[] getLiveData(int sessionId,
                                        LiveDataCommand[] commands)
        throws PermissionException, AgentNotFoundException,
        AppdefEntityNotFoundException, LiveDataException,
        SessionTimeoutException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return liveDataManager.getData(subject, commands);
    }

    /**
     * Get the commands for a given resource.
     */
    @Transactional(readOnly=true)
    public String[] getLiveDataCommands(int sessionId, AppdefEntityID id)
        throws PluginException, PermissionException,
        SessionTimeoutException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return liveDataManager.getCommands(subject, id);
    }

    /**
     * Get the ConfigSchema for this resource
     */
    @Transactional(readOnly=true)
    public ConfigSchema getConfigSchema(int sessionId, AppdefEntityID id,
                                        String command)
        throws PluginException, PermissionException,
        SessionTimeoutException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return liveDataManager.getConfigSchema(subject, id, command);
    }

}
