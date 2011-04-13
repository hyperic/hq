/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
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
 */

package org.hyperic.hq.bizapp.server.operations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.auth.shared.AuthManager;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.operation.Envelope;
import org.hyperic.hq.operation.OperationService;
import org.hyperic.hq.operation.annotation.Operation;
import org.hyperic.hq.operation.rabbit.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
@Service
@Transactional
public class ServerOperationServiceValidator {

    private final Log logger = LogFactory.getLog(this.getClass());

    private OperationService operationService;

    private AuthManager authManager;

    private AuthzSubjectManager authzSubjectManager;

    private PermissionManager permissionManager;

    @Autowired
    public ServerOperationServiceValidator(OperationService operationService, AuthManager authManager,
        AuthzSubjectManager authzSubjectManager, PermissionManager permissionManager) {
        this.operationService = operationService;
        this.authzSubjectManager = authzSubjectManager;
        this.authManager = authManager;
        this.permissionManager = permissionManager;
    }

    void checkUserCanManageAgent(String user, String pword, String operation, String callerIp) throws PermissionException {
        try {
            this.authManager.authenticate(user, pword);
            this.permissionManager.checkCreatePlatformPermission(this.authzSubjectManager.findSubjectByAuth(user, HQConstants.ApplicationName));
            
        } catch (SecurityException exc) {
            logger.warn("Security exception when '" + user + "' tried to " + operation + " an Agent @ " + callerIp, exc);
            throw new PermissionException();
        } catch (PermissionException exc) {
            logger.warn("Permission denied when '" + user + "' tried to " + operation + " an Agent @ " + callerIp);
            throw new PermissionException();
        } catch (ApplicationException exc) {
            logger.warn("Application exception when '" + user + "' tried to " + operation + " an Agent @ " + callerIp, exc);
            throw new PermissionException();
        } catch (SystemException exc) {
            logger.warn("System exception when '" + user + "' tried to " + operation + " an Agent @ " + callerIp, exc);
            throw new PermissionException();
        }
    }

   @Operation(operationName = Constants.OPERATION_NAME_SERVER_TO_AGENT_PING, exchangeName = Constants.TO_AGENT_AUTHENTICATED_EXCHANGE)
   void testAgentConn(String agentIP, int agentPort, String authToken, boolean isNewTransportAgent, boolean unidirectional) throws AgentConnectionException {
       try {
           this.operationService.perform(new Envelope(Constants.OPERATION_NAME_SERVER_TO_AGENT_PING, authToken));
       } catch (RuntimeException e) {
           throw new AgentConnectionException(e.getMessage());
       }
   }
}
