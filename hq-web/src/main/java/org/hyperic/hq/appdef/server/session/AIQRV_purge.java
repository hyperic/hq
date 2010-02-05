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

package org.hyperic.hq.appdef.server.session;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AIQApprovalException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AIIp;
import org.hyperic.hq.autoinventory.AIPlatform;
import org.hyperic.hq.autoinventory.AIServer;
import org.hyperic.hq.dao.AIIpDAO;
import org.hyperic.hq.dao.AIPlatformDAO;
import org.hyperic.hq.dao.AIServerDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The AIQueueConstants.Q_DECISION_PURGE means to remove the resource from the
 * ai queue.
 */
@Component
public class AIQRV_purge implements AIQResourceVisitor {

    private final Log log = LogFactory.getLog(AIQRV_purge.class);
    private AIPlatformDAO aiPlatformDAO;
    private AIIpDAO aiIpDAO;
    private AIServerDAO aiServerDAO;

    @Autowired
    public AIQRV_purge(AIPlatformDAO aiPlatformDAO, AIIpDAO aiIpDAO, AIServerDAO aiServerDAO) {
        this.aiPlatformDAO = aiPlatformDAO;
        this.aiIpDAO = aiIpDAO;
        this.aiServerDAO = aiServerDAO;
    }

    public void visitPlatform(AIPlatform aiplatform, AuthzSubject subject, List createdResources)
        throws AIQApprovalException, PermissionException {
        log.info("Visiting platform: " + aiplatform.getId() + " fqdn=" + aiplatform.getFqdn());
        aiPlatformDAO.remove(aiplatform);
    }

    public void visitIp(AIIp aiip, AuthzSubject subject) throws AIQApprovalException,
        PermissionException {
        log.info("Visiting ip: " + aiip.getId() + " addr=" + aiip.getAddress());
        aiIpDAO.remove(aiip);
    }

    public void visitServer(AIServer aiserver, AuthzSubject subject, List createdResources)
        throws AIQApprovalException, PermissionException {
        log.info("Visiting server: " + aiserver.getId() + " AIID=" +
                 aiserver.getAutoinventoryIdentifier());
        aiServerDAO.remove(aiserver);
    }
}
