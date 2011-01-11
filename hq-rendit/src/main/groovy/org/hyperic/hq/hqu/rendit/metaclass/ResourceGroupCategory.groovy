/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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
 *
 */

package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.server.session.ResourceGroup
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.appdef.shared.AppdefEntityConstants
import org.hyperic.hq.context.Bootstrap;
import org.springframework.security.provisioning.GroupManager;
import org.hyperic.hq.authz.shared.AuthzConstants
import org.hyperic.hq.events.shared.MaintenanceEventManager
import org.hyperic.hq.authz.shared.PermissionManagerFactory
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.events.MaintenanceEvent

class ResourceGroupCategory {
    private static groupMan = Bootstrap.getBean(ResourceGroupManager.class)
    private static rsrcMan  = Bootstrap.getBean(ResourceManager.class)

    private static MaintenanceEventManager maintMan =
        PermissionManagerFactory.getInstance().getMaintenanceEventManager();

    static String urlFor(ResourceGroup r, String context) {
        "/Resource.do?eid=${AppdefEntityConstants.APPDEF_TYPE_GROUP}:${r.id}" 
    }
    
    static void setResources(ResourceGroup group, AuthzSubject user,
                             Collection resources)
    {
        groupMan.setResources(user, group, resources)
    }

    static Collection getResources(ResourceGroup group) {
        groupMan.getMembers(group)
    }
    
    /**
     * Returns true if the ResourceGroup is compatible (i.e. only contains
     * resources of a homogenous type)
     */
    static boolean isCompatible(ResourceGroup g) {
        g.groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS ||
        g.groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC
    }
    
    /**
     * Gets the Resource prototype that the group is compatible wth.
     */
    static Resource getCompatibleType(ResourceGroup g) {
        if (!isCompatible(g)) {
            throw new IllegalArgumentException("Attempted to get compatible " +
                                               "type on non-compatible group")
        }
        
        def resourceTypeId
        if (g.groupEntType == AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
            resourceTypeId = AuthzConstants.authzPlatformProto
        } else if (g.groupEntType == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
            resourceTypeId = AuthzConstants.authzServerProto
        } else if (g.groupEntType == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
            resourceTypeId = AuthzConstants.authzServiceProto
        } else {
            assert "Unknown group ent type ${g.groupEntType} for compat groups"
        }
            
        rsrcMan.findResourceByInstanceId(resourceTypeId, g.groupEntResType)
    }
    
    static void updateGroupType(ResourceGroup g, AuthzSubject subject,
                                int groupType, int groupEntType,
                                int groupEntResType) {
        groupMan.updateGroupType(subject, g, groupType, groupEntType,
                                 groupEntResType)
    }
    
    static void updateGroup(ResourceGroup g, AuthzSubject subject,
                            String name, String description, String location) {
        groupMan.updateGroup(subject, g, name, description, location)
    }

    static void remove(ResourceGroup g, AuthzSubject subject) {
        groupMan.removeResourceGroup(subject, g)
    }

    static MaintenanceEvent scheduleMaintenance(ResourceGroup g, AuthzSubject subject,
                                                long start, long end) {
        MaintenanceEvent e = new MaintenanceEvent(g.getId());
        e.setStartTime(start)
        e.setEndTime(end)
        maintMan.schedule(subject, e)
    }

    static void unscheduleMaintenance(ResourceGroup g, AuthzSubject subject) {
        MaintenanceEvent e = new MaintenanceEvent(g.getId());
        maintMan.unschedule(subject, e)
    }

    static MaintenanceEvent getMaintenanceEvent(ResourceGroup g,
                                                AuthzSubject subject) {
        maintMan.getMaintenanceEvent(subject, g.getId())
    }
}
