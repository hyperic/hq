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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.ServiceCluster;
import org.hyperic.hq.appdef.server.session.AppdefSessionEJB;
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.CloningBossLocal;
import org.hyperic.hq.appdef.shared.CloningBossUtil;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.server.session.AppdefBossEJBImpl;
import org.hyperic.hq.bizapp.shared.AIBossLocal;
import org.hyperic.hq.bizapp.shared.AIBossUtil;
import org.hyperic.hq.bizapp.shared.AppdefBossLocal;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.util.config.EncodingException;

/**
 * @ejb:bean name="CloningBoss"
 *      jndi-name="ejb/appdef/OrgCloningBoss" 
 *      local-jndi-name="OrgLocalCloningBoss"
 *      view-type="both"
 *      type="Stateless"
 * 
 * @ejb:transaction type="REQUIRED"
 */
public class CloningBossEJBImpl extends AppdefSessionEJB
    implements SessionBean {
    
    private final Log _log = LogFactory.getLog(CloningBossEJBImpl.class);

    public CloningBossEJBImpl() {
    }
    
    /**
     * @param subj
     * @param pType platform type
     * @param nameRegEx regex which matches either the platform fqdn or the
     * resource sortname
     * @ejb:interface-method
     */
    public List findPlatformsByTypeAndName(AuthzSubject subj, Integer pType,
                                           String nameRegEx) {
        PlatformManagerLocal platformMan = PlatformManagerEJBImpl.getOne();
        return platformMan.findPlatformPojosByTypeAndName(
            subj, pType, nameRegEx);
    }
    
    /**
     * @param subj Method ensures that the master platform has viewable
     * permissions and the clone targets have modifiable permissions.
     * @param platformId master platform id
     * @param cloneTaretIds List<Integer> List of Platform Ids to be cloned
     * @ejb:interface-method
     */
    public void clonePlatform(AuthzSubject subj, Integer platformId,
                              List cloneTargetIds)
        throws SessionNotFoundException, SessionTimeoutException,
               SessionException, PermissionException, PlatformNotFoundException
    {
        PlatformManagerLocal platformMan = PlatformManagerEJBImpl.getOne();
        Platform master = platformMan.findPlatformById(platformId);
        AppdefEntityID entityId =
        master.getAppdefResourceValue().getEntityId();
        checkViewPermission(subj, entityId);
        boolean debug = _log.isDebugEnabled();
        for (Iterator it=cloneTargetIds.iterator(); it.hasNext(); ) {
            try {
                Integer pId = (Integer)it.next();
                if (debug) {
                    _log.debug("Attempting to clone platform, masterId=" +
                        platformId + " cloneId=" + pId);
                }
                if (pId.equals(platformId)) {
                    logError(master, "");
                    _log.warn("Attempt to clone master -> master ignored, " +
                        "continuing.");
                    continue;
                }
                Platform toClone = platformMan.findPlatformById(pId);
                entityId = toClone.getAppdefResourceValue().getEntityId();
                checkModifyPermission(subj, entityId);
                clonePlatform(subj, master, toClone);
            } catch (Exception e) {
                _log.error(e.getMessage(), e);
                logError(master, e.getMessage());
            }
        }
    }
    
    private void logError(Platform platform, String msg) {
    }

    public void removeServers(AuthzSubject subj, Integer[] servers)
        throws ServerNotFoundException, SessionNotFoundException,
               SessionTimeoutException, PermissionException,
               SessionException, VetoException {
        AppdefBossLocal boss = AppdefBossEJBImpl.getOne();
        ServerManagerLocal serverMan = ServerManagerEJBImpl.getOne();
        for (int i=0; i<servers.length; i++) {
            Integer serverId = servers[i];
            Server server = serverMan.findServerById(serverId);
            if (server.isWasAutodiscovered()
                || server.isRuntimeAutodiscovery()) {
                boss.removeServer(subj, server.getId());
            }
        }
    }

    public List cloneServers(AuthzSubject subj, Platform clone,
                              Integer[] servers)
        throws AppdefEntityNotFoundException, ConfigFetchException,
               PermissionException, FinderException,
               AppdefDuplicateNameException, ValidationException,
               CreateException {
        List clones = new ArrayList();
        ServerManagerLocal serverMan = ServerManagerEJBImpl.getOne();
        ConfigManagerLocal configManager = ConfigManagerEJBImpl.getOne();
        for (int i=0; i<servers.length; i++) {
            Integer serverId = servers[i];
            Server server = serverMan.findServerById(serverId);
            if (hasServer(clone, server)) {
                AppdefEntityID entityId =
                    server.getAppdefResourceValue().getEntityId();
                ConfigResponseDB cr = server.getConfigResponse();
                byte[] productResponse = cr.getProductResponse();
                byte[] measResponse = cr.getMeasurementResponse();
                byte[] controlResponse = cr.getControlResponse();
                byte[] rtResponse = cr.getResponseTimeResponse();
                configManager.configureResource(subj, entityId, productResponse,
                    measResponse, controlResponse, rtResponse, null, true, true);
            }
            if (!server.isWasAutodiscovered()
                 && server.isRuntimeAutodiscovery()) {
                Server s = serverMan.cloneServer(subj, clone, server);
                clones.add(s);
            }
        }
        return clones;
    }

    /**
     * @ejb:transaction type="RequiresNew"
     * @ejb:interface-method
     */
    public void clonePlatform(AuthzSubject subj, Platform master,
                              Platform clone)
        throws AppdefEntityNotFoundException, ConfigFetchException,
               PermissionException, FinderException, CreateException,
               NamingException, SessionNotFoundException,
               SessionTimeoutException, SessionException, VetoException,
               AppdefDuplicateNameException, ValidationException,
               GroupNotCompatibleException, UpdateException, EncodingException {
        ServerManagerLocal serverMan = ServerManagerEJBImpl.getOne();
        Integer[] servers = serverMan.getServerIdsByPlatform(subj, clone.getId());
        removeServers(subj, servers);
        servers = serverMan.getServerIdsByPlatform(subj, master.getId());
        List clones = cloneServers(subj, clone, servers);
        runAI(subj, clones);
    }

    private void runAI(AuthzSubject subj, List clones)
        throws CreateException, NamingException, AppdefGroupNotFoundException,
               SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, GroupNotCompatibleException,
               PermissionException, UpdateException, ConfigFetchException,
               EncodingException {
        AIBossLocal aiBoss = AIBossUtil.getLocalHome().create();
        for (Iterator it=clones.iterator(); it.hasNext(); ) {
            Server s = (Server)it.next();
            aiBoss.toggleRuntimeScan(
                subj, s.getAppdefResourceValue().getEntityId(), true);
		}
    }

    private boolean hasServer(Platform platform, Server server) {
        for (Iterator i=platform.getServers().iterator(); i.hasNext(); ) {
            String aiId = ((Server)i.next()).getAutoinventoryIdentifier();
            if (aiId.equals(server.getAutoinventoryIdentifier())) {
                return true;
            }
        }
        return false;
	}
    
    public static CloningBossLocal getOne() {
        try {
            return CloningBossUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }
    
    /** @ejb:create-method */
    public void ejbCreate() throws CreateException {}
    public void ejbActivate() throws EJBException, RemoteException {}
    public void ejbPassivate() throws EJBException, RemoteException {}
    public void ejbRemove() throws EJBException, RemoteException {}
    
}
