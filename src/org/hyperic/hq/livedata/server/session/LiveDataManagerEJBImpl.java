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

package org.hyperic.hq.livedata.server.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.LiveDataPluginManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.server.session.ProductManagerEJBImpl;
import org.hyperic.hq.livedata.agent.client.LiveDataClient;
import org.hyperic.hq.appdef.shared.AgentConnectionUtil;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.livedata.shared.LiveDataManagerLocal;
import org.hyperic.hq.livedata.shared.LiveDataManagerUtil;
import org.hyperic.hq.livedata.shared.LiveDataException;

import javax.ejb.SessionContext;
import javax.ejb.SessionBean;
import java.util.Properties;

/**
 * @ejb:bean name="LiveDataManager"
 *      jndi-name="ejb/livedata/LiveDataManager"
 *      local-jndi-name="LocalLiveDataManager"
 *      view-type="local"
 *      type="Stateless"
 */
public class LiveDataManagerEJBImpl implements SessionBean {

    private static Log _log = LogFactory.getLog(LiveDataManagerEJBImpl.class);

    private LiveDataPluginManager _manager;

    /** @ejb:create-method */
    public void ejbCreate() {

        // Get reference to the plugin manager
        try {
            _manager = (LiveDataPluginManager) ProductManagerEJBImpl.
                getOne().getPluginManager(ProductPlugin.TYPE_LIVE_DATA);
        } catch (Exception e) {
            _log.error("Unable to get plugin manager", e);
        }
    }

    public static LiveDataManagerLocal getOne() {
        try {
            return LiveDataManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx) {}

    private String getPlugin(AppdefEntityID id)
        throws AppdefEntityNotFoundException
    {
        ConfigManagerLocal cManager = ConfigManagerEJBImpl.getOne();
        // XXX: Seems this method should not be in the config manager.
        return cManager.getPluginName(id);
    }

    /**
     * Get live data for a given resource.
     *
     * @ejb:interface-method
     */
    public String getData(AppdefEntityID id, String command)
        throws PermissionException, AgentNotFoundException,
        AgentConnectionException, AgentRemoteException,
        AppdefEntityNotFoundException, LiveDataException
    {
        LiveDataClient client =
            new LiveDataClient(AgentConnectionUtil.getClient(id));
        Properties props = new Properties();

        String plugin = getPlugin(id);
        return client.getData(plugin, command, props);
    }

    /**
     * Get the available commands for a given resources.
     *
     * @ejb:interface-method 
     */
    public String[] getCommands(AppdefEntityID id)
        throws PluginException
    {
        try {
            String plugin = getPlugin(id);
            return _manager.getCommands(plugin);
        } catch (AppdefEntityNotFoundException e) {
            throw new PluginNotFoundException("No plugin found for " + id, e);
        }
    }
}
