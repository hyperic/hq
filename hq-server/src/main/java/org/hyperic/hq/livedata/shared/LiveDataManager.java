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
package org.hyperic.hq.livedata.shared;

import java.util.Set;

import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.livedata.FormatType;
import org.hyperic.hq.livedata.LiveDataFormatter;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigSchema;

/**
 * Local interface for LiveDataManager.
 */
public interface LiveDataManager {
    /**
     * Run the given live data command.
     */
    public LiveDataResult getData(AuthzSubject subject, LiveDataCommand cmd) throws AppdefEntityNotFoundException,
        PermissionException, AgentNotFoundException, LiveDataException;

    /**
     * Run the given live data command. If cached data is found that is not
     * older than the cachedTimeout the cached data will be returned.
     * @param cacheTimeout
     */
    public LiveDataResult getData(AuthzSubject subject, LiveDataCommand cmd, long cacheTimeout)
        throws PermissionException, AgentNotFoundException, AppdefEntityNotFoundException, LiveDataException;

    /**
     * Run a list of live data commands in batch.
     */
    public LiveDataResult[] getData(AuthzSubject subject, LiveDataCommand[] commands)
        throws AppdefEntityNotFoundException, PermissionException, AgentNotFoundException, LiveDataException;

    /**
     * Run a list of live data commands in batch. If cached data is found that
     * is not older than the cacheTimeout the cached data will be returned.
     * @param cacheTimeout The cache timeout given in milliseconds.
     */
    public LiveDataResult[] getData(AuthzSubject subject, LiveDataCommand[] commands, long cacheTimeout)
        throws PermissionException, AppdefEntityNotFoundException, AgentNotFoundException, LiveDataException;

    /**
     * Get the available commands for a given resources.
     */
    public String[] getCommands(AuthzSubject subject, AppdefEntityID id) throws PluginException, PermissionException;

    public void registerFormatter(LiveDataFormatter f);

    public void unregisterFormatter(LiveDataFormatter f);

    /**
     * Gets a set of {@link LiveDataFormatter}s which are able to format the
     * passed command.
     */
    public Set<LiveDataFormatter> findFormatters(LiveDataCommand cmd, FormatType type);

    /**
     * Find a formatter based on its 'id' property.
     */
    public LiveDataFormatter findFormatter(String id);

    /**
     * Get the ConfigSchema for a given resource.
     */
    public ConfigSchema getConfigSchema(AuthzSubject subject, AppdefEntityID id, String command)
        throws PluginException, PermissionException;

}
