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

package org.hyperic.hq.livedata.agent.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.livedata.agent.client.LiveDataCommandsClient;
import org.hyperic.hq.livedata.agent.commands.LiveData_args;
import org.hyperic.hq.livedata.agent.commands.LiveData_result;
import org.hyperic.hq.livedata.shared.LiveDataResult;
import org.hyperic.hq.product.LiveDataPluginManager;
import org.hyperic.util.config.ConfigResponse;

/**
 * The Live Data Commands service.
 */
public class LiveDataCommandsService implements LiveDataCommandsClient {
    
    private static final Log _log = LogFactory.getLog(LiveDataCommandsService.class);
    
    private final LiveDataPluginManager _manager;
    
    
    public LiveDataCommandsService(LiveDataPluginManager manager) {
        _manager = manager;
    }

    /**
     * @see org.hyperic.hq.livedata.agent.client.LiveDataCommandsClient#getData(org.hyperic.hq.appdef.shared.AppdefEntityID, java.lang.String, java.lang.String, org.hyperic.util.config.ConfigResponse)
     */
    public LiveDataResult getData(AppdefEntityID id, 
                                  String type,
                                  String command, 
                                  ConfigResponse config) {        
        try {
            String xml = _manager.getData(type, command, config);
            return new LiveDataResult(id, xml);  
        } catch (Exception e) {
            return new LiveDataResult(id, e, e.getMessage());
        }
    }
    
    LiveData_result getData(LiveData_args args) throws AgentRemoteException {
        _log.info("Asked to invoke getData for " + args.getType());

        try {
            String xml = _manager.getData(args.getType(), 
                                          args.getCommand(), 
                                          args.getConfig());
            LiveData_result res = new LiveData_result();
            res.setResult(xml);
            return res;
        } catch (Exception e) {
            throw new AgentRemoteException(e.getMessage(), e);
        }
    }

}
