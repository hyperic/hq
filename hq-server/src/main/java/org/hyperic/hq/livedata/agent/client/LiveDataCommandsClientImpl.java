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

package org.hyperic.hq.livedata.agent.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.client.AbstractCommandsClient;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.livedata.shared.LiveDataResult;
import org.hyperic.hq.transport.AgentProxyFactory;
import org.hyperic.util.config.ConfigResponse;

/**
 * The Live Data Commands client that uses the new transport.
 */
public class LiveDataCommandsClientImpl 
    extends AbstractCommandsClient implements LiveDataCommandsClient {
    
    private static final Log _log = LogFactory.getLog(LiveDataCommandsClientImpl.class);
    

    public LiveDataCommandsClientImpl(Agent agent, AgentProxyFactory factory) {
        super(agent, factory);
    }

    /**
     * @see org.hyperic.hq.livedata.agent.client.LiveDataCommandsClient#getData(org.hyperic.hq.appdef.shared.AppdefEntityID, java.lang.String, java.lang.String, org.hyperic.util.config.ConfigResponse)
     */
    public LiveDataResult getData(AppdefEntityID id, 
                                  String type,
                                  String command, 
                                  ConfigResponse config) {

        LiveDataCommandsClient proxy = null;

        try {
            proxy = (LiveDataCommandsClient)getSynchronousProxy(LiveDataCommandsClient.class);
            LiveDataResult rs = proxy.getData(id, type, command, config);
            
            // Check that the live data can be deserialized from XML
            try {
                rs.getObjectResult();
                return rs;
            } catch (Throwable t) {
                String err = LiveDataCommandsClientFactory.BUNDLE.format("error.serialization");
                _log.warn(err, t);
                return new LiveDataResult(id, t, err);
            }
        } catch (Exception e) {
            return new LiveDataResult(id, e, e.getMessage());
        } finally {
            safeDestroyService(proxy);
        }    
    }

}
