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

package org.hyperic.hq.bizapp.server.session;

import java.util.Properties;

import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.util.ConfigPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A boss to provide bizapp client access to the configuration settings
 */
@Service
@Transactional
public class ConfigBossImpl implements ConfigBoss {

    private SessionManager sessionManager;

    private ServerConfigManager serverConfigManager;

    private PermissionManager permissionManager;

    @Autowired
    public ConfigBossImpl(SessionManager sessionManager, ServerConfigManager serverConfigManager,
                          PermissionManager permissionManager) {
        this.sessionManager = sessionManager;
        this.serverConfigManager = serverConfigManager;
        this.permissionManager = permissionManager;
    }

    /**
     * Get the top-level configuration properties
     * 
     */
    @Transactional(readOnly = true)
    public Properties getConfig() throws ConfigPropertyException {
        return serverConfigManager.getConfig();
    }

    /**
     * Get the configuration properties for a specified prefix
     * 
     */
    @Transactional(readOnly = true)
    public Properties getConfig(String prefix) throws ConfigPropertyException {
        return serverConfigManager.getConfig(prefix);
    }

    /**
     * Set the top-level configuration properties
     * 
     */
    public void setConfig(int sessId, Properties props) throws ApplicationException,
        ConfigPropertyException {
        AuthzSubject subject = sessionManager.getSubject(sessId);
        serverConfigManager.setConfig(subject, props);
    }

    /**
     * Set the configuration properties for a prefix
     * 
     */
    public void setConfig(int sessId, String prefix, Properties props) throws ApplicationException,
        ConfigPropertyException {
        AuthzSubject subject = sessionManager.getSubject(sessId);
        serverConfigManager.setConfig(subject, prefix, props);
    }
    
    public void setConfig(AuthzSubject subject, Properties props) throws ApplicationException, 
        ConfigPropertyException{
        serverConfigManager.setConfig(subject, props);
    }

}
