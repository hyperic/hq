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
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
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
    
    private int _groupType;
    private int _entityType;
    private int _resourceType;
    private AuthzSubject _subject;
    private boolean _exclusive;
    private static final int UNDEFINED = -1;
    private int _filterCount;
    private boolean groupSelected = false;

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
        return _groupType;
    }

    public int getEntityType() {
        return _entityType;
    }

    public int getResourceType() {
        return _resourceType;
    }

    public AuthzSubject getSubject() {
        return _subject;
    }

    public int getFilterCount() {
        return _filterCount;
    }

    public boolean isExclusive() {
        return _exclusive;
    }

    public void setGroupSelected(boolean b) {
        this.groupSelected = b;
    }

    public AppdefPagerFilterGroupEntityResource(AuthzSubject subject, int gt,
                                                int et, int rt, boolean negate)
    {
        _subject = subject;
        _groupType = gt;
        _entityType = et;
        _resourceType = rt;
        _exclusive = (!negate);
        _filterCount = 0;
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
        
        if (!entity.isGroup() && _resourceType == -1 && _resourceType == -1) {
            return false; // Short circuit.
        }

        try {
            boolean caught = isCompatible(entity);
            if (_exclusive == caught) {
                _filterCount++;
            }
            return _exclusive == caught;

        } catch (Exception e) {
            // In a paging context, we swallow all exceptions.
            return _exclusive == false;
        }
    }

    private boolean isCompatible(AppdefEntityID entity)
        throws PermissionException, AppdefEntityNotFoundException {
        AppdefEntityValue aev = new AppdefEntityValue(entity, _subject);

        switch (_groupType) {
        case GROUP_ADHOC_APP:
        case GROUP_ADHOC_GRP:
        case GROUP_ADHOC_PSS:
            return isGroupAdhoc(aev);
        case GROUP_COMPAT_PS:
            if (groupSelected)
                return isResourceCompatible(entity);
            else {
                return isGroupResourceCompatible(aev);
            }
        case GROUP_COMPAT_SVC:
            if (groupSelected)
                return isResourceCompatible(entity);
            else {
                return isGroupResourceCompatible(aev);
            }
        case UNDEFINED:
            if (_resourceType == UNDEFINED) {
                return isEntityCompatible(aev);
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
    private boolean isGroupAdhoc(AppdefEntityValue aev)
        throws AppdefGroupNotFoundException, PermissionException {
        AppdefGroupValue vo = aev.getAppdefGroupValue();
        if (!vo.isGroupAdhoc())
            return false;
        
        if (_resourceType != UNDEFINED) {
            return vo.getGroupEntResType() == _resourceType;
        }
        
        return true;
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
                ResourceGroup group = ResourceGroupManagerEJBImpl.getOne()
                    .findResourceGroupById(_subject, id.getId()); 
                return _resourceType == group.getGroupEntResType().intValue();
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
    private boolean isGroupResourceCompatible(AppdefEntityValue aev)
        throws AppdefGroupNotFoundException, PermissionException {
        if (!aev.getID().isGroup())
            return false;
        
        AppdefGroupValue groupVo = aev.getAppdefGroupValue();
        if (groupVo.isGroupCompat()) {
            if (_entityType == GROUP) {
                if (_resourceType != UNDEFINED)
                    return _resourceType == groupVo.getGroupEntResType();
                else
                    return true;
            } else {
                if (_resourceType == UNDEFINED) {
                    return groupVo.isGroupCompat();
                } else {
                    return (_entityType == groupVo.getGroupEntType() &&
                            _resourceType == groupVo.getGroupEntResType());
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
    private boolean isEntityCompatible(AppdefEntityValue vo)
        throws AppdefGroupNotFoundException, PermissionException {
        if (_entityType == GROUP && _resourceType == UNDEFINED &&
            vo.getID().isGroup()) {
            AppdefGroupValue groupVo = vo.getAppdefGroupValue();
            if (groupVo.isGroupAdhoc()) {
                return true;
            }
            return false;
        }
        if (_entityType == vo.getID().getType()) {
            return true;
        }
        return false;
    }
}
