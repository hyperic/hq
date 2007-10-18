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

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;

/** Pager Processor filter that filters object instances of
 *   AppdefGroupValue based on entity and resource type. The entity & 
 *   resource types are specified during construction and used during paging.
 *   The filter simply returns a flag indicating whether or not entity was 
 *   caught by the filter.
 *
 *   [input]       [input]        [output]
 *   ENTITY_TYPE   RESOURCE_TYPE  TRUE (INVENTORY RETURNED)
 *   ----------------------------------------------------------------------
 *   PLATFORM      -1             Mixed Groups of PSS & Compat groups of PS
 *   PLATFORM      <restype>      Compat groups of PS and <restype>
 *   SERVER        -1             Mixed Groups of PSS & Compat groups of PS
 *   SERVER        <restype>      Compat groups of PS and <restype>
 *   SERVICE       -1             Mixed Groups of PSS & Compat groups of SVC
 *   SERVICE       <restype>      Compat groups of SVC and <restype>
 *   APPLICATION   -1             Mixed Groups of App
 *
 */
public class AppdefGroupPagerFilterGrpEntRes implements AppdefPagerFilter {

    private int entityType;
    private int resourceType;
    private int filterCount;
    private boolean exclusive;

    // Create some shorter constant references...
    private static final int PLATFORM = 
        AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
    private static final int SERVER   = 
        AppdefEntityConstants.APPDEF_TYPE_SERVER;
    private static final int SERVICE  = 
        AppdefEntityConstants.APPDEF_TYPE_SERVICE;
    private static final int APPLICATION = 
        AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
    private static final int GROUP_ADHOC_APP = 
        AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP;
    private static final int GROUP_ADHOC_PSS = 
        AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS;
    private static final int GROUP_COMPAT_PS = 
        AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS;
    private static final int GROUP_COMPAT_SVC = 
        AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC;

    public int getEntityType ()  { return entityType; }
    public int getResourceType() { return resourceType; }
    public int getFilterCount () { return filterCount; }
    public boolean isExclusive (){ return exclusive; }

    public AppdefGroupPagerFilterGrpEntRes  ( int et, int rt ) {
        this.entityType   = et;
        this.resourceType = rt;
        this.exclusive    = true;
    }

    public AppdefGroupPagerFilterGrpEntRes  ( int et, int rt, 
        boolean negate ) {
        this.entityType   = et;
        this.resourceType = rt;
        this.exclusive    = (! negate);
    }

   /** Evaluate an object against the filter.
    * @param o - object instance of AppdefGroupValue
    * @return flag - true if caught (unless negated)  */
    public boolean isCaught ( Object o ) {

      if (!(o instanceof AppdefGroupValue)) {
          throw new IllegalArgumentException("Expecting instance of "+
                                             "AppdefGroupValue");
      }

      AppdefGroupValue groupVo = (AppdefGroupValue) o;

      //System.out.println ("EntityType: "+entityType);
      //System.out.println ("groupValue:"+groupVo.toString());

      return exclusive == isCompatible (groupVo);
    }

    private boolean isCompatible ( AppdefGroupValue vo ) {
        switch (entityType) {
        case (PLATFORM):
            return isGroupPlatformCompatible(vo);
        case (APPLICATION):
            return isGroupAppCompatible(vo);
        case (SERVER):
            return isGroupServerCompatible(vo);
        case (SERVICE):
            return isGroupServiceCompatible(vo);
        default: 
            return false; // unsupported entity type?
        }
    }

    // return true if group is compatible with platform inventory.
    private boolean isGroupPlatformCompatible (AppdefGroupValue vo) {
        if (vo.getGroupType() == GROUP_ADHOC_PSS) {
            return true;
        }
        if (vo.getGroupType()       == GROUP_COMPAT_PS &&
            vo.getGroupEntType()    == PLATFORM &&
            vo.getGroupEntResType() == resourceType ) { 
            return true;
        }
        return false;
    }

    // return true if group is compatible with application inventory.
    private boolean isGroupAppCompatible(AppdefGroupValue vo) {
        if (vo.getGroupType() == GROUP_ADHOC_APP) {
            return true;
        }
        return false;
    }

    // return true if group is compatible with server inventory.
    private boolean isGroupServerCompatible(AppdefGroupValue vo) {
        if (vo.getGroupType() == GROUP_ADHOC_PSS) {
            return true;
        }
        if (vo.getGroupType()       == GROUP_COMPAT_PS &&
            vo.getGroupEntType()    == SERVER &&
            vo.getGroupEntResType() == resourceType) {
            return true;
        }
        return false;
    }

    // return true if group is compatible with service inventory.
    private boolean isGroupServiceCompatible(AppdefGroupValue vo) {
        if (vo.getGroupType() == GROUP_ADHOC_PSS) {
            return true;
        }
        if (vo.getGroupType()       == GROUP_COMPAT_SVC &&
            vo.getGroupEntType()    == SERVICE &&
            vo.getGroupEntResType() == resourceType) { 
            return true;
        }
        return false;
    }
}
