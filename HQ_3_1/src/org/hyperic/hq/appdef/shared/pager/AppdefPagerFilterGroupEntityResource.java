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

package org.hyperic.hq.appdef.shared.pager;

import org.hyperic.hq.appdef.server.session.AppdefGroupManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;

/** Pager Processor filter that filters object instances of
*   AppdefEntityID based on three contextual criteria: group type
*   entity type and resource type. These three values are passed during
*   construction and referenced during pager processing. The filter
*   simply returns a flag indicating whether or not entity was caught
*   by the filter.
*
*   Context IO matrix:
*   [input 1]      [input 2]    [input 3]      [output]
*   GROUP_TYPE     ENTITY_TYPE  RESOURCE_TYPE  TRUE (INVENTORY RETURNED)
*   ----------------------------------------------------------------------
*   ADHOC_APP      -1           -1             Applications
*   ADHOC_APP      APP          -1             Applications
*   ADHOC_APP      GROUP        -1             Applications
*   ADHOC_GRP      GROUP        -1             Mixed Groups of Groups
*   ADHOC_GRP      GROUP        COMPAT_PS      Compatible groups
*   ADHOC_GRP      GROUP        ADHOC_APP      Mixed Groups of Applications
*   ADHOC_GRP      GROUP        ADHOC_PSS      Mixed Groups of PSS
*   ADHOC_PSS      -1           -1             All Platforms,Servers & Services
*   ADHOC_PSS      PLATFORM     -1             All Platforms
*   ADHOC_PSS      PLATFORM     <type>         All Platforms of <type>
*   ADHOC_PSS      SERVER       -1             All Servers
*   ADHOC_PSS      SERVER       <type>         All Servers of <type>
*   ADHOC_PSS      SERVICE      -1             All Services
*   ADHOC_PSS      SERVICE      <type>         All Services of <type>
*   ADHOC_PSS      GROUP        -1             Grps Platform,server,Services
*   COMPAT_PS      <type>       <type>         All <type> of <type>
*   COMPAT_SVC     <type>       <type>         All <type> of <type>
*   COMPAT_PS      GROUP        -1             All compatible groups
*   COMPAT_SVC     GROUP        -1             All compatible groups
*   -1             GROUP        -1             All mixed groups
*   -1             GROUP        COMPAT_SVC     All service clusters
*   -1             <type>       -1             All of entity <type>
*   -1             <type>       <type>         All <type> of <type>
*
*/
public class AppdefPagerFilterGroupEntityResource implements AppdefPagerFilter {
    
    private int groupType;
    private int entityType;
    private int resourceType;
    private AuthzSubjectValue subject;
    private boolean exclusive;
    private static final int UNDEFINED = -1;
    private int filterCount;
    private boolean groupSelected = false;

    // Create some shorter constant references...
    private static final int PLATFORM =
        AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
    private static final int SERVER =
        AppdefEntityConstants.APPDEF_TYPE_SERVER;
    private static final int SERVICE =
        AppdefEntityConstants.APPDEF_TYPE_SERVICE;
    private static final int APPLICATION =
        AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
    private static final int GROUP =
        AppdefEntityConstants.APPDEF_TYPE_GROUP;
    private static final int GROUP_ADHOC_APP =
        AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP;
    private static final int GROUP_ADHOC_GRP =
        AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP;
    private static final int GROUP_ADHOC_PSS =
        AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS;
    private static final int GROUP_COMPAT_PS =
        AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS;
    private static final int GROUP_COMPAT_SVC =
        AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC;

    public int getGroupType() {
        return groupType;
    }

    public int getEntityType() {
        return entityType;
    }

    public int getResourceType() {
        return resourceType;
    }

    public AuthzSubjectValue getSubject() {
        return subject;
    }

    public int getFilterCount() {
        return filterCount;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setGroupSelected(boolean b) {
        this.groupSelected = b;
    }

    public AppdefPagerFilterGroupEntityResource(AuthzSubjectValue subject,
                                                int gt, int et, int rt) {
        this.subject = subject;
        this.groupType = gt;
        this.entityType = et;
        this.resourceType = rt;
        this.exclusive = true;
        filterCount = 0;
    }

    public AppdefPagerFilterGroupEntityResource(AuthzSubjectValue subject,
                                                int gt, int et, int rt,
                                                boolean negate) {
        this.subject = subject;
        this.groupType = gt;
        this.entityType = et;
        this.resourceType = rt;
        this.exclusive = (!negate);
        filterCount = 0;
    }

    /**
     * Evaluate an object against the filter.
     *
     * @param o - object instance of AppdefEntityID
     * @return flag - true if caught (unless negated)
     */
    public boolean isCaught(Object o) {
        AppdefEntityID entity;

        if (!(o instanceof AppdefEntityID)) {
            throw new IllegalArgumentException("Expecting instance of " +
                "AppdefEntityID");
        }
        
        entity = (AppdefEntityID) o;
        
        if (!entity.isGroup() && resourceType == -1 && resourceType == -1) {
            return false; // Short circuit.
        }

        try {
            boolean caught = isCompatible(entity);
            if (exclusive == caught) {
                filterCount++;
            }
            return exclusive == caught;

        } catch (Exception e) {
            // In a paging context, we swallow all exceptions.
            return exclusive == false;
        }
    }

    private boolean isCompatible(AppdefEntityID entity)
        throws PermissionException, AppdefEntityNotFoundException {
        AppdefResourceValue arv;
        AppdefEntityValue aev = new AppdefEntityValue(entity, subject);

        switch (groupType) {
        case (GROUP_ADHOC_APP):
            arv = aev.getLiteResourceValue();
            return isGroupAdhocAppCompatible(arv);
        case (GROUP_ADHOC_GRP):
            arv = aev.getLiteResourceValue();
            return isGroupAdhocGrpCompatible(arv);
        case (GROUP_ADHOC_PSS):
            arv = aev.getLiteResourceValue();
            return isGroupAdhocPSSCompatible(arv);
        case (GROUP_COMPAT_PS):
            if (groupSelected)
                return isResourceCompatible(entity);
            else {
                arv = aev.getLiteResourceValue();
                return isGroupResourceCompatible(arv);
            }
        case (GROUP_COMPAT_SVC):
            if (groupSelected)
                return isResourceCompatible(entity);
            else {
                arv = aev.getLiteResourceValue();
                return isGroupResourceCompatible(arv);
            }
        case (UNDEFINED):
            if (resourceType == UNDEFINED) {
                arv = aev.getLiteResourceValue();
                return isEntityCompatible(arv);
            } else {
                return isResourceCompatible(entity);
            }
        default:
                return false;      // unsupported group type?
        }
    }

    // mixed groups of applications are compatible with:
    // - applications
    // GROUP_TYPE     ENTITY_TYPE  RESOURCE_TYPE  INVENTORY RETURNED
    // ----------------------------------------------------------------------
    // ADHOC_APP      -1           -1             Applications
    // ADHOC_APP      APP          -1             Applications
    // ADHOC_APP      GROUP        -1             Applications
    private boolean isGroupAdhocAppCompatible(AppdefResourceValue vo) {
        if (entityType == UNDEFINED && resourceType == UNDEFINED &&
            vo.getEntityId().getType() == APPLICATION) {
            return true;
        }
        if (entityType == APPLICATION && resourceType == UNDEFINED &&
            vo.getEntityId().getType() == APPLICATION) {
            return true;
        }
        if (entityType == GROUP && resourceType == UNDEFINED &&
            vo.getEntityId().getType() == GROUP) {
            AppdefGroupValue groupVo = (AppdefGroupValue) vo;
            if (groupVo.getGroupType() == GROUP_ADHOC_APP) {
                return true;
            }
        }
        return false;
    }

    // mixed groups of groups are compatible with:
    // - mixed groups of application
    // - mixed groups of platform,server,service
    // - compat group of platform,server
    // - compat group of service
    // GROUP_TYPE     ENTITY_TYPE  RESOURCE_TYPE  INVENTORY RETURNED
    // ----------------------------------------------------------------------
    // ADHOC_GRP      GROUP        -1             Mixed Groups of Groups
    // ADHOC_GRP      GROUP        COMPAT_PS      Compatible groups
    // ADHOC_GRP      GROUP        ADHOC_APP      Mixed Groups of Applications            
    // ADHOC_GRP      GROUP        ADHOC_PSS      Mixed Groups of PSS
    private boolean isGroupAdhocGrpCompatible(AppdefResourceValue vo) {
        // We only ever return groups, so short circuit if not group entity
        if (vo.getEntityId().getType() ==
            AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            AppdefGroupValue groupVo = (AppdefGroupValue) vo;

            if (entityType == GROUP && resourceType == UNDEFINED &&
                groupVo.getGroupType() == GROUP_ADHOC_GRP) {
                return true;
            }
            if (entityType == GROUP && resourceType == GROUP_COMPAT_PS &&
                (groupVo.getGroupType() == GROUP_COMPAT_PS ||
                 groupVo.getGroupType() == GROUP_COMPAT_SVC)) {
                return true;
            }
            if (entityType == GROUP && resourceType == GROUP_ADHOC_APP &&
                groupVo.getGroupType() == GROUP_ADHOC_APP) {
                return true;
            }
            if (entityType == GROUP && resourceType == GROUP_ADHOC_PSS &&
                groupVo.getGroupType() == GROUP_ADHOC_PSS) {
                return true;
            }
        }
        return false;
    }

    // mixed groups of "platform,server&service" are compatible with:
    // - platforms
    // - servers
    // - services
    // GROUP_TYPE     ENTITY_TYPE  RESOURCE_TYPE  INVENTORY RETURNED
    // ----------------------------------------------------------------------
    // ADHOC_PSS      -1           -1             All Platforms,Servers&Services
    // ADHOC_PSS      PLATFORM     -1             All Platforms
    // ADHOC_PSS      PLATFORM     <type>         All Platforms of <type>
    // ADHOC_PSS      SERVER       -1             All Servers
    // ADHOC_PSS      SERVER       <type>         All Servers of <type>
    // ADHOC_PSS      SERVICE      -1             All Services
    // ADHOC_PSS      SERVICE      <type>         All Services of <type>
    // ADHOC_PSS      GROUP        -1             Grps Platform,server,Services
    private boolean isGroupAdhocPSSCompatible(AppdefResourceValue vo) {
        if (entityType == UNDEFINED && resourceType == UNDEFINED &&
            (vo.getEntityId().getType() == PLATFORM ||
             vo.getEntityId().getType() == SERVER ||
             vo.getEntityId().getType() == SERVICE)) {
            return true;
        }
        if (entityType == PLATFORM && resourceType == UNDEFINED &&
            vo.getEntityId().getType() == PLATFORM) {
            return true;
        }
        if (entityType == PLATFORM &&
            vo.getEntityId().getType() == PLATFORM &&
            resourceType ==
                vo.getAppdefResourceTypeValue().getId().intValue()) {
            return true;
        }
        if (entityType == SERVER && resourceType == UNDEFINED &&
            vo.getEntityId().getType() == SERVER) {
            return true;
        }
        if (entityType == SERVER &&
            vo.getEntityId().getType() == SERVER &&
            resourceType ==
                vo.getAppdefResourceTypeValue().getId().intValue()) {
            return true;
        }
        if (entityType == SERVICE && resourceType == UNDEFINED &&
            vo.getEntityId().getType() == SERVICE) {
            return true;
        }
        if (entityType == SERVICE &&
            vo.getEntityId().getType() == SERVICE &&
            resourceType ==
                vo.getAppdefResourceTypeValue().getId().intValue()) {
            return true;
        }
        if (entityType == GROUP && resourceType == UNDEFINED &&
            vo.getEntityId().getType() == GROUP) {
            AppdefGroupValue groupVo = (AppdefGroupValue) vo;
            if (groupVo.getGroupType() == GROUP_ADHOC_PSS) {
                return true;
            }
        }
        return false;
    }

    // Resource compatibility implies both appdef type and resource type
    // compatibility (all compat groups and compatible contexts)
    // 
    // GROUP_TYPE     ENTITY_TYPE  RESOURCE_TYPE  INVENTORY RETURNED
    // ----------------------------------------------------------------------
    // UNDEF          <type>       <type>         ALl <type> of <type>
    private boolean isResourceCompatible(AppdefEntityID id)
        throws AppdefGroupNotFoundException, PermissionException {
        switch(id.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                return true;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                AppdefGroupValue group =
                    AppdefGroupManagerEJBImpl.getOne().findGroup(subject, id);
                return resourceType == group.getGroupEntResType();
            default:
                return false;
        }
    }

    // Resource compatibility implies both appdef type and resource type
    // compatibility (all compat groups and compatible contexts)
    // 
    // GROUP_TYPE     ENTITY_TYPE  RESOURCE_TYPE  INVENTORY RETURNED
    // ----------------------------------------------------------------------
    // COMPAT_PS      <type>       <type>         All <type> of <type>
    // COMPAT_SVC     <type>       <type>         All <type> of <type>
    // COMPAT_PS      GROUP        -1             All compatible (ps) groups
    // COMPAT_SVC     GROUP        -1             All compatible (svc) groups
    private boolean isGroupResourceCompatible(AppdefResourceValue vo) {
        if (vo.getEntityId().getType() == GROUP) {
            AppdefGroupValue groupVo = (AppdefGroupValue) vo;
            if (groupVo.isGroupCompat()) {
                if (entityType == GROUP) {
                    if (resourceType != UNDEFINED)
                        return resourceType == groupVo.getGroupEntResType();
                    else
                        return true;
                } else {
                    if (resourceType == UNDEFINED) {
                        return entityType == groupVo.getGroupEntType();
                    } else {
                        return (entityType == groupVo.getGroupEntType() &&
                            resourceType == groupVo.getGroupEntResType());
                    }
                }
            }
        }
        return false;
    }

    // Entity type compatibility implies only entity type matches. Supports
    // contexts where resource compatibility hasn't yet been indicated.
    //
    // GROUP_TYPE     ENTITY_TYPE  RESOURCE_TYPE  INVENTORY RETURNED
    // ----------------------------------------------------------------------
    // -1             GROUP        -1             All mixed groups
    // -1             GROUP        COMPAT_SVC     All service clusters
    // -1             <type>       -1             All of entity <type>
    private boolean isEntityCompatible(AppdefResourceValue vo) {
        if (entityType == GROUP && resourceType == UNDEFINED &&
            vo.getEntityId().getType() == GROUP) {
            AppdefGroupValue groupVo = (AppdefGroupValue) vo;
            if (groupVo.isGroupAdhoc()) {
                return true;
            }
            return false;
        }
        if (entityType == vo.getEntityId().getType()) {
            return true;
        }
        return false;
    }
}
