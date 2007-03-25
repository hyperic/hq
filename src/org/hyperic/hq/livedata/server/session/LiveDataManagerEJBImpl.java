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
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.livedata.shared.LiveDataManagerLocal;
import org.hyperic.hq.livedata.shared.LiveDataManagerUtil;
import org.hyperic.hq.livedata.shared.LiveDataException;
import org.hyperic.hq.livedata.shared.LiveDataResult;
import org.hyperic.hq.livedata.shared.LiveDataCommand;
import org.hyperic.hq.agent.client.AgentConnection;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

import javax.ejb.SessionContext;
import javax.ejb.SessionBean;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

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

    /**
     * Live data subsystem uses measurement configs.
     */
    private ConfigResponse getConfig(AuthzSubjectValue subject,
                                     LiveDataCommand command)
        throws LiveDataException
    {
        ConfigManagerLocal cManager = ConfigManagerEJBImpl.getOne();

        try {
            AppdefEntityID id = command.getAppdefEntityID();
            ConfigResponse config = command.getConfig();

            try {
                ConfigResponse mConfig = cManager.
                    getMergedConfigResponse(subject,
                                            ProductPlugin.TYPE_MEASUREMENT,
                                            id, true);
                config.merge(mConfig, false);
                return config;
            } catch (ConfigFetchException e) {
                // No measurement config?  No problem
                return config;
            }
        } catch (Exception e) {
            throw new LiveDataException(e);
        }
    }

    /**
     * Get the appdef type for a given entity id.
     */
    private String getType(AuthzSubjectValue subject, LiveDataCommand cmd)
        throws AppdefEntityNotFoundException, PermissionException
    {
        AppdefEntityID id = cmd.getAppdefEntityID();
        AppdefEntityValue val = new AppdefEntityValue(id, subject);
        AppdefResourceTypeValue typeVal = val.getResourceTypeValue();
        return typeVal.getName();
    }

    /**
     * Run the given live data command.
     *
     * @ejb:interface-method
     */
    public LiveDataResult getData(AuthzSubjectValue subject,
                                  LiveDataCommand cmd)
        throws PermissionException, AgentNotFoundException,
               AppdefEntityNotFoundException, LiveDataException
    {
        AppdefEntityID id = cmd.getAppdefEntityID();
        AgentConnection conn = AgentConnectionUtil.getClient(id);
        LiveDataClient client = new LiveDataClient(conn);

        ConfigResponse config = getConfig(subject, cmd);
        String type = getType(subject, cmd);

        return client.getData(type, cmd.getCommand(), config);
    }

    /**
     * Run a list of live data commands in batch.
     *
     * @ejb:interface-method 
     */
    public LiveDataResult[] getData(AuthzSubjectValue subject,
                                    LiveDataCommand[] commands)
        throws PermissionException, AppdefEntityNotFoundException,
               AgentNotFoundException, LiveDataException
    {
        HashMap buckets = new HashMap();

        for (int i = 0; i < commands.length; i++) {
            LiveDataCommand cmd = commands[i];
            AppdefEntityID id = cmd.getAppdefEntityID();
            AgentConnection conn = AgentConnectionUtil.getClient(id);

            ConfigResponse config = getConfig(subject, cmd);
            String type = getType(subject, cmd);

            LiveDataExecutorCommand exec =
                new LiveDataExecutorCommand(type, cmd.getCommand(), config);

            List queue = (List)buckets.get(conn);
            if (queue == null) {
                queue = new ArrayList();
                queue.add(exec);
                buckets.put(conn, queue);
            } else {
                queue.add(exec);
            }
        }

        LiveDataExecutor executor = new LiveDataExecutor();
        for (Iterator i = buckets.keySet().iterator(); i.hasNext(); ) {
            AgentConnection conn = (AgentConnection)i.next();
            List cmds = (List)buckets.get(conn);
            executor.getData(new LiveDataClient(conn), cmds);
        }

        executor.shutdown();

        return executor.getResult();
    }

    /**
     * Get the available commands for a given resources.
     *
     * @ejb:interface-method 
     */
    public String[] getCommands(AuthzSubjectValue subject, AppdefEntityID id)
        throws PluginException, PermissionException
    {
        try {
            AppdefEntityValue val = new AppdefEntityValue(id, subject);
            AppdefResourceTypeValue tVal = val.getResourceTypeValue();

            return _manager.getCommands(tVal.getName());
        } catch (AppdefEntityNotFoundException e) {
            throw new PluginNotFoundException("No plugin found for " + id, e);
        }
    }

    /**
     * Get the ConfigSchema for a given resource.
     * 
     * @ejb:interface-method
     */
    public ConfigSchema getConfigSchema(AuthzSubjectValue subject,
                                        AppdefEntityID id)
        throws PluginException, PermissionException
    {
        try {
            AppdefEntityValue val = new AppdefEntityValue(id, subject);
            AppdefResourceTypeValue tVal = val.getResourceTypeValue();

            return _manager.getConfigSchema(tVal.getName());
        } catch (AppdefEntityNotFoundException e) {
            throw new PluginNotFoundException("No plugin found for " + id, e);
        }
    }
}
