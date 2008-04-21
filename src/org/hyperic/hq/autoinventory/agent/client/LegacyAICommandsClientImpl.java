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

package org.hyperic.hq.autoinventory.agent.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.autoinventory.agent.AICommandsAPI;
import org.hyperic.hq.bizapp.agent.client.SecureAgentConnection;
import org.hyperic.util.config.ConfigResponse;

/**
 * The AI Commands client that uses the legacy transport.
 */
public class LegacyAICommandsClientImpl implements AICommandsClient {

    protected Log log = LogFactory.getLog(LegacyAICommandsClientImpl.class);

    private SecureAgentConnection agentConn;
    private AICommandsAPI verAPI;

    /**
     * Creates a new AICommandsClient object which should communicate
     * through the passed connection object.
     *
     * @param agentConn Connection this object should use when sending 
     *                  commands.
     */
    public LegacyAICommandsClientImpl ( SecureAgentConnection agentConn ) {
        this.agentConn = agentConn;
        this.verAPI    = new AICommandsAPI();
    }

    /**
     * @see org.hyperic.hq.autoinventory.agent.client.AICommandsClient#startScan(org.hyperic.hq.autoinventory.ScanConfigurationCore)
     */
    public void startScan (ScanConfigurationCore scanConfig) 
        throws AgentRemoteException, 
               AgentConnectionException, 
               AutoinventoryException {

        AgentRemoteValue configARV = new AgentRemoteValue();
        scanConfig.toAgentRemoteValue(AICommandsAPI.PROP_SCANCONFIG, configARV);

        log.info("AICommandsClient.startScan for " + agentConn);
        agentConn.sendCommand(AICommandsAPI.command_startScan,
                              verAPI.getVersion(),
                              configARV);
    }

    /**
     * @see org.hyperic.hq.autoinventory.agent.client.AICommandsClient#stopScan()
     */
    public void stopScan () 
        throws AgentRemoteException, AgentConnectionException {

        AgentRemoteValue emptyParam = new AgentRemoteValue();
        log.info("CommandsClient.stopScan");
        agentConn.sendCommand(AICommandsAPI.command_stopScan,
                              verAPI.getVersion(),
                              emptyParam);
    }

    /**
     * @see org.hyperic.hq.autoinventory.agent.client.AICommandsClient#getScanStatus()
     */
    public ScanStateCore getScanStatus () 
        throws AgentRemoteException, 
               AgentConnectionException, 
               AutoinventoryException {

        AgentRemoteValue rval;
        AgentRemoteValue emptyParam = new AgentRemoteValue();
        log.info("CommandsClient.getScanStatus");
        rval = agentConn.sendCommand(AICommandsAPI.command_getScanStatus,
                                     verAPI.getVersion(),
                                     emptyParam);

        ScanStateCore core
            = ScanStateCore.fromAgentRemoteValue("scanState", rval);

        if (log.isDebugEnabled())
            log.debug("scan state: "+ core.getPlatform().getId() + " done:" + core.getIsDone());
            
        return core;
    }

    /**
     * @see org.hyperic.hq.autoinventory.agent.client.AICommandsClient#pushRuntimeDiscoveryConfig(int, int, java.lang.String, java.lang.String, org.hyperic.util.config.ConfigResponse)
     */
    public void pushRuntimeDiscoveryConfig ( int type, int id, 
                                             String typeName,
                                             String name,
                                             ConfigResponse response ) {
        AgentRemoteValue arv = 
            AICommandsUtils.createArgForRuntimeDiscoveryConfig(type, 
                                                               id, 
                                                               typeName, 
                                                               name, 
                                                               response);

        log.debug("AICommandsClient.pushRuntimeDiscoveryConfig");
        try {
            agentConn.sendCommand(AICommandsAPI.command_pushRuntimeDiscoveryConfig,
                                  verAPI.getVersion(), arv); 
        } catch (AgentConnectionException ace) {
            log.error("Error connecting to agent to push runtime discovery "
                      + "config: " + ace.getMessage());
        } catch (AgentRemoteException are) {
            log.error("Error sending runtime discover configuration to agent: "
                      + are.getMessage());
        }
    }

}
