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

package org.hyperic.hq.appdef.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.authz.shared.TypeCounts;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.util.Reference;


/**
 * This class is meant to represent the resource inventory summary
 * for a given user. It is used primarily by the dashboard resource
 * summary portlet. 
 */
@SuppressWarnings("serial")
public class AppdefInventorySummary implements java.io.Serializable {

    public static int COUNT_UNKNOWN = 0;

    private AuthzSubject _user     = null;
    private int appCount           = COUNT_UNKNOWN;
    private int platformCount      = COUNT_UNKNOWN;
    private int serverCount        = COUNT_UNKNOWN;
    private int serviceCount       = COUNT_UNKNOWN;
    private int clusterCount       = COUNT_UNKNOWN;
    private int groupCntAdhocGroup = COUNT_UNKNOWN;
    private int groupCntAdhocPSS   = COUNT_UNKNOWN;
    private int groupCntAdhocApp   = COUNT_UNKNOWN;
    private int compatGroupCount   = COUNT_UNKNOWN;
    private int dynamicGroupCnt  = COUNT_UNKNOWN;
    private Map<Integer, Reference<Integer>> platformTypeMap    = null;
    private Map<Integer, Reference<Integer>> serverTypeMap      = null;
    private Map<Integer, Reference<Integer>> serviceTypeMap     = null;
    private Map<Integer, Reference<Integer>> appTypeMap         = null;
    private PermissionManager permissionManager;
  

    public AppdefInventorySummary(AuthzSubject user, boolean countTypes, PermissionManager permissionManager) {
        _user = user;
        this.permissionManager = permissionManager;
        init(countTypes);
    }

    /**
     * Populate the summary
     */
    private void init(boolean countTypes) {
        final ResourceManager resourceManager = Bootstrap.getBean(ResourceManager.class);
        final Collection<ResourceType> types = new ArrayList<ResourceType>();
        types.add(resourceManager.findResourceTypeById(AuthzConstants.authzPlatform));
        types.add(resourceManager.findResourceTypeById(AuthzConstants.authzServer));
        types.add(resourceManager.findResourceTypeById(AuthzConstants.authzService));
        types.add(resourceManager.findResourceTypeById(AuthzConstants.authzApplication));
        types.add(resourceManager.findResourceTypeById(AuthzConstants.authzGroup));
        final TypeCounts typeCounts = permissionManager.findViewableInstanceCounts(_user, types);
        platformTypeMap = typeCounts.getProtoTypeCounts(AppdefEntityConstants.APPDEF_TYPE_PLATFORM);
        platformCount = getCount(typeCounts, AppdefEntityConstants.APPDEF_TYPE_PLATFORM);
        serverTypeMap = typeCounts.getProtoTypeCounts(AppdefEntityConstants.APPDEF_TYPE_SERVER);
        serverCount = getCount(typeCounts, AppdefEntityConstants.APPDEF_TYPE_SERVER);
        serviceTypeMap = typeCounts.getProtoTypeCounts(AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        serviceCount = getCount(typeCounts, AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        appTypeMap = typeCounts.getProtoTypeCounts(AppdefEntityConstants.APPDEF_TYPE_APPLICATION);
        appCount = getCount(typeCounts, AppdefEntityConstants.APPDEF_TYPE_APPLICATION);
        setGroupSummary(typeCounts);
    }

    private int getCount(TypeCounts typeCounts, int appdefType) {
        final Reference<Integer> count = typeCounts.getAppdefTypeCounts().get(appdefType);
        return (count == null) ? 0 : count.get();
    }

    /**
     * @return the total number of viewable platforms
     */
    public int getPlatformCount() {
        return platformCount;
    }
    
    /**
     * @return the total compat group count
     */
    public int getCompatGroupCount() {
        return compatGroupCount;
    }
    
    /**
     * @return the total number of viewable servers
     */
    public int getServerCount() {
        return serverCount;
    }
    
    /**
     * @return the total number of viewable services
     */
    public int getServiceCount() {
        return serviceCount;
    }

    /**
     * @return the total number of viewable applications
     */
    public int getApplicationCount() {
        return appCount;
    }

    /**
     * @return the total number of viewable clusters
     */
    public int getClusterCount() {
        return clusterCount;
    }

    /**
     * @return the total number of adhoc groups of group
     */
    public int getGroupCountAdhocGroup() {
        return groupCntAdhocGroup;
    }

    /**
     * @return the total number of adhoc groups of PSS
     */
    public int getGroupCountAdhocPSS() {
        return groupCntAdhocPSS;
    }

    /**
     * @return the total number of adhoc groups of App
     */
    public int getGroupCountAdhocApp() {
        return groupCntAdhocApp;
    }

    /**
     * @return the total number of dynamic groups
     */
    public int getDynamicGroupCount() {
        return dynamicGroupCnt;
    }


    /**
     * @return a map whose keys are the type names, values are
     * count of viewable instances of that type
     */
    public Map<Integer, Reference<Integer>> getPlatformTypeMap() {
        return platformTypeMap;
    }

    /**
     * @return a map whose keys are the type names, values are
     * count of viewable instances of that type
     */
    public Map<Integer, Reference<Integer>> getServerTypeMap() {
        return serverTypeMap;
    }

    /**
     * @return a map whose keys are the type names, values are
     * count of viewable instances of that type
     */
    public Map<Integer, Reference<Integer>> getServiceTypeMap() {
        return serviceTypeMap;
    }

    /**
     * @return a map whose keys are the type names, values are
     * count of viewable instances of that type
     */
    public Map<Integer, Reference<Integer>> getAppTypeMap() {
        return appTypeMap;
    }

    /* With groups, we have multiple types and each type may or may not
       break down to further group types. So we're not returning a "map" per
       se. Rather, we'll encapsulate a map-like structure and provide
       accessors to the group summary count information. This also has the
       added benefit of keeping our types ordered where maps lose this attr. */
    private void setGroupSummary(TypeCounts typeCounts) {
        groupCntAdhocApp = getCount(typeCounts, AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP);
        groupCntAdhocGroup = getCount(typeCounts, AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP);
        groupCntAdhocPSS = getCount(typeCounts, AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS);
        clusterCount = getCount(typeCounts, AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC);
        dynamicGroupCnt = getCount(typeCounts, AppdefEntityConstants.APPDEF_TYPE_GROUP_DYNAMIC);
        compatGroupCount =
            getCount(typeCounts, AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC) +
            getCount(typeCounts, AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS);
    }

}
