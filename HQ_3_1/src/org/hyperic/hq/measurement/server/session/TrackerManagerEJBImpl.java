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

package org.hyperic.hq.measurement.server.session;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AgentConnectionUtil;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The tracker manager handles sending agents add and remove operations
 * for the log and config track plugsin.
 *
 * @ejb:bean name="TrackerManager"
 *      jndi-name="ejb/measurement/TrackerManager"
 *      local-jndi-name="LocalTrackerManager"
 *      view-type="local"
 *      type="Stateless"
 */
public class TrackerManagerEJBImpl 
    extends SessionEJB 
    implements SessionBean 
{
    private final Log log = LogFactory.getLog(TrackerManagerEJBImpl.class);

    /**
     * @ejb:create-method
     */
    public void ejbCreate() throws CreateException {}

    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx) {}

    public MeasurementCommandsClient getClient(AppdefEntityID aid) 
        throws PermissionException, AgentNotFoundException {
        return new MeasurementCommandsClient(AgentConnectionUtil.getClient(aid));
    }

    /** 
     * Check if a log or config plugin needs to be created
     *
     * XXX: permission checks
     * @ejb:interface-method
     */
    public void trackPluginAdd(AuthzSubjectValue subject,
                               AppdefEntityID id,
                               String pluginType,
                               ConfigResponse response)
        throws PermissionException, PluginException
    {
        try {
            MeasurementCommandsClient client = getClient(id);
            String resourceName = PlatformManagerEJBImpl.getOne()
                .getPlatformPluginName(id);

            client.addTrackPlugin(id.getAppdefKey(), pluginType, 
                                  resourceName, response);        
        } catch (AppdefEntityNotFoundException e) {
            throw new PluginException("Entity not found: " +
                                      e.getMessage());
        } catch (AgentNotFoundException e) {
            throw new PluginException("Agent error: " + e.getMessage());
        } catch (AgentConnectionException e) {
            throw new PluginException("Agent error: " + e.getMessage());
        } catch (AgentRemoteException e) {
            throw new PluginException("Agent error: " + e.getMessage());
        }
    }

    /**
     * Check if a log or config plugin needs to be disabled
     *
     * XXX: permission checks
     * @ejb:interface-method
     */
    public void trackPluginRemove(AuthzSubjectValue subject,
                                  AppdefEntityID id,
                                  String pluginType)
        throws PermissionException, PluginException
    {
        try {
            MeasurementCommandsClient client = getClient(id);
            client.removeTrackPlugin(id.getAppdefKey(), pluginType);
        } catch (AgentNotFoundException e) {
            throw new PluginException("Agent error: " + e.getMessage());
        } catch (AgentConnectionException e) {
            throw new PluginException("Agent error: " + e.getMessage());
        } catch (AgentRemoteException e) {
            throw new PluginException("Agent error: " + e.getMessage());
        }
    }
}
