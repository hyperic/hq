/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.ui.action;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts.tiles.actions.TilesAction;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * Base TilesAction class
 * 
 */
public abstract class BaseTilesAction
    extends TilesAction {

    protected void checkModifyPermission(HttpServletRequest request) throws ParameterNotFoundException,
        PermissionException {

        AppdefEntityID aeid = RequestUtils.getEntityId(request);
        String opName = null;

        switch (aeid.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                opName = AuthzConstants.platformOpModifyPlatform;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                opName = AuthzConstants.serverOpModifyServer;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                opName = AuthzConstants.serviceOpModifyService;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                opName = AuthzConstants.groupOpModifyResourceGroup;
                break;
            default:
                throw new InvalidAppdefTypeException("Unknown type: " + aeid.getType());
        }

        checkPermission(request, opName);
    }

    protected void checkPermission(HttpServletRequest request, String opName) throws PermissionException {

        // See if user can access this action
        Map userOpsMap = (Map) request.getSession().getAttribute(Constants.USER_OPERATIONS_ATTR);

        if (userOpsMap == null || !userOpsMap.containsKey(opName)) {
            throw new PermissionException("User does not have permission [" + opName + "] to access this page.");
        }
    }
}
