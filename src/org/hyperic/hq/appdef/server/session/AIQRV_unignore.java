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

package org.hyperic.hq.appdef.server.session;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AIQApprovalException;
import org.hyperic.hq.appdef.shared.CPropManagerLocal;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AIIp;
import org.hyperic.hq.autoinventory.AIPlatform;
import org.hyperic.hq.autoinventory.AIServer;

/**
 * The AIQueueConstants.Q_DECISION_UNIGNORE means to reset the 'ignored'
 * flag on the queued resource.
 */
public class AIQRV_unignore implements AIQResourceVisitor {

    private static Log _log = LogFactory.getLog(AIQRV_unignore.class);

    public void visitPlatform(AIPlatform aiplatform,
                              AuthzSubject subject,
                              PlatformManagerLocal pmLocal,
                              ConfigManagerLocal configMgr,
                              CPropManagerLocal cpropMgr,
                              List createdResources)
        throws AIQApprovalException, PermissionException {
    }

    public void visitIp(AIIp aiip,
                        AuthzSubject subject,
                        PlatformManagerLocal pmLocal)
        throws AIQApprovalException, PermissionException {
    }

    public void visitServer(AIServer aiserver,
                            AuthzSubject subject,
                            PlatformManagerLocal pmLocal,
                            ServerManagerLocal smLocal,
                            ConfigManagerLocal configMgr,
                            CPropManagerLocal cpropMgr,
                            List createdResources)
        throws AIQApprovalException, PermissionException
    {
        Integer pk = aiserver.getId();
        _log.info("Visiting server: " + pk +
                  " AIID=" + aiserver.getAutoinventoryIdentifier());
        aiserver.setIgnored(false);
    }
}
