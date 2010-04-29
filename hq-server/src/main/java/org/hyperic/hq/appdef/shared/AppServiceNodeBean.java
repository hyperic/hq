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

package org.hyperic.hq.appdef.shared;

import org.hyperic.hq.authz.shared.ResourceGroupValue;

/**
 * This is a display helper bean for the screens
 * 
 * The columns are
 * <ul>
 * <li>icon with link to view AppServiceNodeBean's list 
 * <li>service label linked by id to ViewService page 
 * <li>service type label (no link)
 * <li>service's parent server label linked by id
 *  </ul>
 * On 2.1.6 whether the AppService is an <i>entry point</i>
 * or not is also displayed.
 */
public class AppServiceNodeBean extends ServiceValue implements java.io.Serializable {

    // the way we're differentiating services that are AppdefGroupValues
    // and ServiceValues is a total hack, this whole thing is regrettable...
    // someday, the DependencyNode will have to be refitted to Do The Right
    // Thing
    public static final Integer CLUSTER_PARENT_ID = new Integer(Integer.MIN_VALUE);
    
    private Integer appdefType;
    private Integer appServiceId;
    private Boolean entryPoint;
    private AppdefEntityID entityId;

    private AppServiceNodeBean() {} // don't let anyone use the empty constructor
    
    public AppServiceNodeBean(AppdefResourceValue service, Integer appServiceId, 
            Boolean entryPoint) {
         init(service, appServiceId,entryPoint);
    }
    
    public AppServiceNodeBean(AppdefResourceValue service, DependencyNode node) {
        init(service, node.getAppService().getId(),new Boolean(node.isEntryPoint()));
    }

    private void init(AppdefResourceValue service, Integer appServiceId, 
            Boolean entryPoint) {
        this.appServiceId = appServiceId;
        this.entryPoint = entryPoint;
        this.appdefType = new Integer(service.getEntityId().getType());
        this.setCTime(service.getCTime());
        this.setDescription(service.getDescription());
        this.setId(service.getId());
        this.setLocation(service.getLocation());
        this.setModifiedBy(service.getModifiedBy());
        this.setMTime(service.getMTime());
        this.setName(service.getName());
        this.setOwner(service.getOwner());
        this.setServiceType((ServiceTypeValue)service
            .getAppdefResourceTypeValue());
        this.setEntityId(service.getEntityId()); 
        if (service instanceof ServiceValue) {
            this.setParentId(((ServiceValue)service).getParentId());
            this.setServer(((ServiceValue)service).getServer());
            this.setResourceGroup(((ServiceValue)service).getResourceGroup());
        } else if (service instanceof AppdefGroupValue) {
            AppdefGroupValue group = (AppdefGroupValue)service;        
            switch (group.getGroupType()) {
                case AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC:
                    break;
                default: // yo mama's got fleas or lice or crawly bugs
                    throw new IllegalStateException("dependency nodes that are groups " +
                        "must be compatible groups of services, not " + group.getGroupType());
            }        
            this.setCluster(Boolean.TRUE); // sets the parent id
            this.setServer(new ServerLightValue());
            this.setResourceGroup(new ResourceGroupValue());
        } else {
            // you really really really suck
            throw new IllegalStateException("dependency nodes must be services " +
                " or groups, not " + service.getClass().getName());
        }
    }
        
    /**
     * Returns the appServiceId.
     * @return Integer
     */
    public Integer getAppServiceId() {
        return appServiceId;
    }

    /**
     * Sets the appServiceId.
     * @param appServiceId The appServiceId to set
     */
    public void setAppServiceId(Integer appServiceId) {
        this.appServiceId = appServiceId;
    }
    /**
     * Returns the entryPoint.
     * @return Boolean
     */
    public Boolean isEntryPoint() {
        return entryPoint;
    }
    public Boolean getEntryPoint() {
        return entryPoint;
    }

    /**
     * Sets the entryPoint.
     * @param entryPoint The entryPoint to set
     */
    public void setEntryPoint(Boolean entryPoint) {
        this.entryPoint = entryPoint;
    }

    /**
     * Returns the appdefType.
     * @return Integer
     */
    public Integer getAppdefType() {
        return appdefType;
    }

    /**
     * Sets the appdefType.
     * @param serviceId The appdefType to set
     */
    public void setAppdefType(Integer appdefType) {
        this.appdefType = appdefType;
    }

    /**
     * @return
     */
    public AppdefEntityID getEntityId() {
        return entityId;
    }

    /**
     * @param entityID
     */
    public void setEntityId(AppdefEntityID entityID) {
        entityId = entityID;
    }

    /**
     * Indicator whether or not the resource backing up the app-service
     * relationship is a cluster or not
     * 
     * @return Boolean
     */
    public Boolean getCluster() {
        return new Boolean(CLUSTER_PARENT_ID.equals(getParentId()));
    }

    /**
     * Sets the cluster flag.
     * @param cluster The cluster flag to set
     */
    public void setCluster(Boolean compatibleGroup) {
        if (compatibleGroup.booleanValue())
            setParentId(CLUSTER_PARENT_ID);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append("[").append(super.toString()).append("]");
        sb.append("[appServiceId = ").append(appServiceId);
        sb.append(",entryPoint = ").append(entryPoint);
        sb.append(",parentId = ").append(getParentId());
        sb.append("]");
        
        return sb.toString();
    }

}
