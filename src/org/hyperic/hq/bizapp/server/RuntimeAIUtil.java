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

package org.hyperic.hq.bizapp.server;

import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManagerLocal;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.bizapp.server.session.BizappSessionEJB;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

public class RuntimeAIUtil {

    public static void toggleRuntimeScan ( AuthzSubjectValue subject,
                                           AppdefEntityID id,
                                           boolean doEnable,
                                           BizappSessionEJB caller) 
    throws AppdefGroupNotFoundException, GroupNotCompatibleException, 
           PermissionException, AppdefEntityNotFoundException, UpdateException,
           ConfigFetchException, EncodingException {
        AppdefEntityValue aval;
        ServerValue serverValue;
        Integer serverID;
        ConfigResponse metricConfig;

        aval = new AppdefEntityValue(id, subject);
        ServerManagerLocal serverMgr    = caller.getServerManager();
        ConfigManagerLocal cman         = caller.getConfigManager();
        AutoinventoryManagerLocal aiMan = caller.getAutoInventoryManager();

        // Update flag in server(s)
        if ( id.getType() == AppdefEntityConstants.APPDEF_TYPE_GROUP ) {
            AppdefGroupManagerLocal agmLocal = caller.getAppdefGroupManager();
            AppdefGroupValue groupVal;

            groupVal = agmLocal.findGroup(subject, id);
            List ids = groupVal.getAppdefGroupEntries();
            AppdefEntityID elementId;
            for (int i=0; i<ids.size(); i++) {
                elementId = (AppdefEntityID) ids.get(i);
                if ( elementId.getType() 
                     != AppdefEntityConstants.APPDEF_TYPE_SERVER ) {
                    throw new GroupNotCompatibleException("Group must contain "
                                                          + "only servers", id);
                }
                serverID = elementId.getId();
                try {
                    serverValue
                        = serverMgr.findServerById(subject, serverID);
                    serverValue.setRuntimeAutodiscovery(doEnable);
                    serverMgr.updateServer(subject, serverValue);

                    if (doEnable) {
                        metricConfig = cman.getMergedConfigResponse(subject,
                                     ProductPlugin.TYPE_MEASUREMENT,
                                     elementId, true);
                        aiMan.pushRuntimeDiscoveryConfig(subject, 
                                                         elementId,
                                                         metricConfig);
                    } else {
                        // Null metricConfig will tell the agent to
                        // disable runtime autodiscovery.
                        metricConfig = null;
                        aiMan.turnOffRuntimeDiscovery(subject, elementId);
                    }
                } catch (ServerNotFoundException e) {
                    throw new SystemException("Error finding server: " 
                                                 + serverID, e);
                } catch (AppdefDuplicateNameException e) {
                    // We rethrow this as a system exception because
                    // we're not changing the name, so this should never
                    // occur unless something really weird is going on.
                    throw new SystemException("Unexpected duplicat name: " 
                                                 + serverID, e);
                }
            }
        } else {
            if (!(id.isServer() || id.isService())) {
                // Should never happen
                throw new SystemException("Entity was not a server or service: " + id);
            }

            if (id.isServer()) {
                serverID = id.getId();
                try {
                    serverValue = serverMgr.findServerById(subject, 
                                                          serverID);
                    serverValue.setRuntimeAutodiscovery(doEnable);
                    serverMgr.updateServer(subject, serverValue);
                } catch (ServerNotFoundException e) {
                    throw new SystemException("Error finding server: " 
                                              + serverID, e);
                } catch (AppdefDuplicateNameException e) {
                    // We rethrow this as a system exception because
                    // we're not changing the name, so this should never
                    // occur unless something really weird is going on.
                    throw new SystemException("Unexpected duplicat name: " 
                                                 + serverID, e);
                }
            }

            if (doEnable) {
                metricConfig =
                    cman.getMergedConfigResponse(subject,
                                                 ProductPlugin.TYPE_MEASUREMENT,
                                                 id, true);
            } else {
                // Null metricConfig will tell the agent to
                // disable runtime autodiscovery.
                metricConfig = null;
            }
            aiMan.pushRuntimeDiscoveryConfig(subject, 
                                             id,
                                             metricConfig);
        }
    }
}
