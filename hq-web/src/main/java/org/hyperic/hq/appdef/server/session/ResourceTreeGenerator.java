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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.resourceTree.ApplicationNode;
import org.hyperic.hq.appdef.shared.resourceTree.PlatformNode;
import org.hyperic.hq.appdef.shared.resourceTree.ResourceTree;
import org.hyperic.hq.appdef.shared.resourceTree.ServerNode;
import org.hyperic.hq.appdef.shared.resourceTree.ServiceNode;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An object which generates ResourceTree objects.  The
 * generator is called with a single appdef entity, and
 * a traversal method.  If the traversal method is TRAVERSE_NORMAL,
 * the object, and all objects which it depends upon are 
 * added to the tree.  If the traversal is TRAVERSE_UP,
 * the tree will contain all resources which it depends upon
 * as well as resources which depend upon it.
 */
public class ResourceTreeGenerator {
    public static final int TRAVERSE_NORMAL
        = AppdefEntityConstants.RESTREE_TRAVERSE_NORMAL;
    public static final int TRAVERSE_UP
        = AppdefEntityConstants.RESTREE_TRAVERSE_UP;

    // Internal information about how traversal should function
    private static final int GO_UP   = 1 << 5;
    private static final int GO_DOWN = 1 << 6;

    private final ServerManager  serverManager;
    private final ServiceManager serviceManager;
    private ApplicationManager applicationManager;
    private AuthzSubject        _subject;

    // Caches so we don't do much work twice
    private HashSet upPlats  = new HashSet();    // Platform IDs which we've traversed up from
    private HashSet upServers = new HashSet();  // Server IDs which we've traversed up from
    private HashSet upServices = new HashSet(); // Service IDs which we've traversed up from
    private HashMap dnPlats = new HashMap();   // Platform IDs which we've traversed down from
    private HashMap dnServers = new HashMap(); // Server IDs which we've traversed down from
    private HashMap dnServices = new HashMap(); // Service IDs which we've traversed down from
    private HashSet dnApps  = new HashSet();    // App IDs which we've traversed down from

    
    @Autowired
    public ResourceTreeGenerator(ServerManager serverMan, ServiceManager serviceMan,
                                 ApplicationManager applicationManager) {
        this.serverManager = serverMan;
        this.serviceManager = serviceMan;
        this.applicationManager = applicationManager;
    }

    public void setSubject(AuthzSubject subject) {
        _subject = subject;
    }
    
    /**
     * Generate a tree which includes the passed IDs, as well as
     * their dependents (or dependencies) as per the traversal method.
     *
     * @param ids       An array of IDs to generate the tree from
     * @param traversal One of TRAVERSE_*
     *
     * @return a new ResourceTree
     */
    ResourceTree generate(AppdefEntityID[] ids, int traversal)
        throws AppdefEntityNotFoundException, PermissionException
    {
        ResourceTree res;
        int direction;

        if(traversal == TRAVERSE_NORMAL){
            direction = GO_DOWN;
        } else if(traversal == TRAVERSE_UP){
            direction = GO_UP | GO_DOWN;
        } else {
            throw new IllegalArgumentException("Unknown traversal method");
        }

        upPlats.clear();
        upServers.clear();
        upServices.clear();
        dnPlats.clear();
        dnServers.clear();
        dnServices.clear();
        dnApps.clear();

        res = new ResourceTree();

        for (int i = 0; i < ids.length; i++) {
            Integer iID = ids[i].getId();

            switch(ids[i].getType()){
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                addFromPlatform(iID, direction, res);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                addFromServer(iID, direction, res);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                addFromService(iID, direction, res);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                addFromApp(iID, direction, res);
                break;
            default:
                throw new SystemException("Unable to generate tree from " +
                                          ids[i]);
            } 
        }
        return res;
    }

    private void addFromPlatform(Integer id, int direction, ResourceTree tree)
        throws PermissionException, AppdefEntityNotFoundException, 
               PlatformNotFoundException
    {
        try {
            AppdefEntityValue aeval = new AppdefEntityValue(
                AppdefEntityID.newPlatformID(id), _subject);
            traversePlatform((Platform) aeval.getResourcePOJO(), direction,
                             tree);
        } catch(PermissionException exc){
            throw new PermissionException("Failed to find platform " + id + 
                                          ": permission denied");
        }

    }

    private void addFromServer(Integer id, int direction, ResourceTree tree)
        throws PermissionException, AppdefEntityNotFoundException
    {
        try {
            AppdefEntityValue aeval = new AppdefEntityValue(
                AppdefEntityID.newServerID(id), _subject);
            traverseServer((Server) aeval.getResourcePOJO(), direction, tree);
        } catch(PermissionException exc){
            throw new PermissionException("Failed to find server " + id + 
                                          ": permission denied");
        }
    }

    private void addFromService(Integer id, int direction, ResourceTree tree)
        throws PermissionException, AppdefEntityNotFoundException
    {
        try {
            AppdefEntityValue aeval = new AppdefEntityValue(
                AppdefEntityID.newServiceID(id), _subject);
           traverseService((Service) aeval.getResourcePOJO(), direction, tree);
        } catch(PermissionException exc){
            throw new PermissionException("Failed to find service " + id + 
                                          ": permission denied");
        }
    }

    private void addFromApp(Integer id, int direction, ResourceTree tree)
        throws PermissionException, AppdefEntityNotFoundException
    {
        AppdefEntityValue aeval = new AppdefEntityValue(
            AppdefEntityID.newAppID(id), _subject);
        traverseApp((ApplicationValue) aeval.getResourceValue(), direction,
                    tree);
    }

    private PlatformNode traversePlatform(Platform platform, int direction,
                                          ResourceTree tree)
        throws PermissionException {
        PlatformNode res;

        Integer platformID = platform.getId();
        if ((res = (PlatformNode) dnPlats.get(platformID)) == null) {
            res = tree.addPlatform(platform);
            dnPlats.put(platformID, res);
        }
        
        if ((direction & GO_UP) != 0 && !upPlats.contains(platformID))
        {
            upPlats.add(platformID);
            Collection servers = serverManager.getViewableServers(_subject,
                                                               platform);

            for (Iterator i = servers.iterator(); i.hasNext();) {
                traverseServer((Server) i.next(), GO_UP, tree);
            }
        }
        return res;
    }
    
    private ServerNode traverseServer(Server server, int direction,
                                      ResourceTree tree)
        throws PermissionException {
        Integer serverID = server.getId();
        ServerNode res;
        if ((res = (ServerNode) dnServers.get(serverID)) == null) {
            PlatformNode platformNode;
            Platform platform = server.getPlatform();

            // Traverse down to the platform
            platformNode = traversePlatform(platform, GO_DOWN, tree); 

            // Add server, now that the platform exists
            res = platformNode.addServer(server);
            dnServers.put(serverID, res);
        }

        
        if ((direction & GO_UP) != 0 && !upServers.contains(serverID)) {
           Collection services;

            upServers.add(serverID);
            try {
                services = serviceManager.getServicesByServer(_subject, server);
            } catch(AppdefEntityNotFoundException exc){
                throw new SystemException("Internal inconsistancy: could not " +
                                          "find services for server '" + 
                                          serverID +  "'");
            } catch(PermissionException exc){
                throw new PermissionException("Failed to get services for " +
                                              "server " + serverID +
                                              ": permission denied");
            }

            for (Iterator i = services.iterator(); i.hasNext();) {
                traverseService((Service) i.next(), GO_UP, tree);
            }
        }
        return res;
    }

    private ServiceNode traverseService(Service service, int direction,
                                        ResourceTree tree)
        throws PermissionException {
        ServiceNode res;
        Integer serviceID;

        serviceID = service.getId();
        if((res = (ServiceNode)dnServices.get(serviceID)) == null){
            ServerNode serverNode;

            try {
                serverNode = traverseServer(service.getServer(), GO_DOWN, tree);
            } catch(PermissionException exc){
                throw new PermissionException("Failed to get server " +
                                              service.getServer().getId() +
                                              " on which service " +  serviceID
                                              + " resides: Permission denied");
            }

            res = serverNode.addService(service);
            dnServices.put(serviceID, res);
        }

        if ((direction & GO_UP) != 0 && !upServices.contains(service.getId())) {
            Collection apps;

            upServices.add(service.getId());
            try {
                AppdefEntityID id =
                    AppdefEntityID.newServiceID(service.getId());
                apps = applicationManager
                    .getApplicationsByResource(_subject, id,
                                               PageControl.PAGE_ALL);
            } catch(ApplicationNotFoundException exc){
                throw new SystemException("Internal inconsistancy: could not " +
                                          "find apps for service '" +
                                          service.getId() + "'");
            }

            for(Iterator i=apps.iterator(); i.hasNext(); ){
                traverseApp((ApplicationValue)i.next(), GO_UP, tree);
            }
        }
        return res;
    }

    private void traverseApp(ApplicationValue app, int direction,
                             ResourceTree tree)
        throws PermissionException {
        if(dnApps.contains(app.getId()))
            return;

        dnApps.add(app.getId());

        ApplicationNode appNode = tree.addApplication(app);
        Collection services;

        if ((direction & GO_DOWN) != 0) {
            try {
                services =
                    serviceManager.getServicesByApplication(_subject, app.getId());
            } catch(AppdefEntityNotFoundException exc){
                throw new SystemException("Internal inconsistancy: could " +
                                          "not get services on which " +
                                          "application " + app.getId() + 
                                          " depends on");
            }

            for(Iterator i=services.iterator(); i.hasNext(); ){
                ServiceNode servNode =
                    traverseService((Service) i.next(), GO_DOWN, tree);
                
                appNode.linkToService(servNode);
            }
        }
    }
}
