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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.appdef.server.session.AppdefStatManagerEJBImpl;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.common.SystemException;


/**
 * This class is meant to represent the resource inventory summary
 * for a given user. It is used primarily by the dashboard resource
 * summary portlet. 
 */
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
    private Map platformTypeMap    = null;
    private Map serverTypeMap      = null;
    private Map serviceTypeMap     = null;
    private Map appTypeMap         = null;
    private List platformTypes     = null;
    private List serverTypes       = null;
    private List serviceTypes      = null;
    
    public AppdefInventorySummary(AuthzSubject user) {
        this(user, true);
    }

    public AppdefInventorySummary(AuthzSubject user, boolean countTypes) {
        _user = user;
        init(countTypes);
    }

    /**
     * Populate the summary
     */
    private void init(boolean countTypes) {
        getPlatformSummary(countTypes);
        getServerSummary(countTypes);
        getServiceSummary(countTypes);
        getAppSummary(countTypes);
        getGroupSummary();
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

    /** XXX begin unimplemented stuff! **/
    public List getPlatformTypes () {
        return this.platformTypes; 
    }

    public List getServerTypes () { 
        return this.serverTypes;
    }

    public List getServiceTypes () {
        return this.serviceTypes;
    }

    /**
     * @return a map whose keys are the type names, values are
     * count of viewable instances of that type
     */
    public Map getPlatformTypeMap() {
        return platformTypeMap;
    }

    /**
     * @return a map whose keys are the type names, values are
     * count of viewable instances of that type
     */
    public Map getServerTypeMap() {
        return serverTypeMap;
    }

    /**
     * @return a map whose keys are the type names, values are
     * count of viewable instances of that type
     */
    public Map getServiceTypeMap() {
        return serviceTypeMap;
    }

    /**
     * @return a map whose keys are the type names, values are
     * count of viewable instances of that type
     */
    public Map getAppTypeMap() {
        return appTypeMap;
    }
    
    private void getPlatformSummary(boolean countTypes) {
        if (countTypes) {
            platformTypeMap = getAppdefStatManager()
                .getPlatformCountsByTypeMap(_user);
            if (platformTypeMap != null) {
                platformCount = countMapTotals(platformTypeMap);
            }
        } else {
            platformCount = getAppdefStatManager().getPlatformsCount(_user);
        }
    }

    private void getServerSummary(boolean countTypes) {
        if (countTypes) {
            serverTypeMap = getAppdefStatManager()
                .getServerCountsByTypeMap(this._user);
            if (serverTypeMap != null) {
                serverCount = countMapTotals(serverTypeMap);
            }
        } else {
            serverCount = getAppdefStatManager().getServersCount(_user);
        }
    }

    private void getServiceSummary(boolean countTypes) {
        if (countTypes) {
            serviceTypeMap = getAppdefStatManager()
                .getServiceCountsByTypeMap(this._user);
            if (serviceTypeMap != null) {
                serviceCount = countMapTotals(serviceTypeMap);
            }
        } else {
            serviceCount = getAppdefStatManager().getServicesCount(_user);
        }
    }

    private void getAppSummary(boolean countTypes) {
        if (countTypes) {
            appTypeMap = getAppdefStatManager()
                .getApplicationCountsByTypeMap(this._user);
            if (appTypeMap != null) {
                appCount = countMapTotals(appTypeMap);
            }
        } else {
            appCount = getAppdefStatManager().getApplicationsCount(_user);
        }
    }

    private AppdefStatManagerLocal getAppdefStatManager() {
        return AppdefStatManagerEJBImpl.getOne();
    }

    /* With groups, we have multiple types and each type may or may not
       break down to further group types. So we're not returning a "map" per
       se. Rather, we'll encapsulate a map-like structure and provide
       accessors to the group summary count information. This also has the
       added benefit of keeping our types ordered where maps lose this attr. */
    private void getGroupSummary() {
        Map grpTypeMap = getAppdefStatManager().getGroupCountsMap(this._user);

        groupCntAdhocApp = ((Integer)grpTypeMap.get(new Integer(
            AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP))).intValue();
        groupCntAdhocGroup = ((Integer)grpTypeMap.get(new Integer(
            AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP))).intValue();
        groupCntAdhocPSS = ((Integer)grpTypeMap.get(new Integer(
            AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS))).intValue();
        clusterCount = ((Integer)grpTypeMap.get(new Integer(
            AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC))).intValue();
        compatGroupCount = (((Integer)grpTypeMap.get(new Integer(
            AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC))).intValue() + 
            ((Integer)grpTypeMap.get(new Integer(
            AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS))).intValue());
    }

    private int countMapTotals(Map integerMap) {
        int total = 0;
        for (Iterator i = integerMap.keySet().iterator();i.hasNext();) {
            int incr = ((Integer)integerMap.get(i.next())).intValue();
            total += incr;
        }
        return total;
    }
}
