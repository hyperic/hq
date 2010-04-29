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

package org.hyperic.hq.bizapp.shared.uibeans;

/**
 * Monitoring pages show list of child/associated resources and their
 * current health and the number of alerts they have.  This bean represents
 * a row in the UI tables that render that.
 * 
 * If getResourceTypeId().intValue()==getResourceTypeId().intValue() 
 * then the resources better be services, since that's the only valid case
 * where that is true.
 */
public class ResourceDisplaySummary extends SingletonDisplaySummary
    implements java.io.Serializable {

    private Boolean showAlerts;        
    private Boolean hasMetrics;
    private String resourceName;
    private String parentResourceName;
    private Integer parentResourceId;
    private String resourceEntityTypeName;
    private Integer parentResourceTypeId;
    private Boolean hasParentResource;
    private String resourceTypeName;
    private Boolean monitorable;
        
    /**
     * Returns the hasMetrics.
     * 
     * If the resource has metrics associated with it, either because the
     * current timeframe of interest has metrics or perhaps it doesn't have
     * metrics because the plugin doesn't define any this flag must be set.
     * 
     * @return Boolean
     */
    public Boolean getHasMetrics() {
        return hasMetrics;
    }

    /**
     * Sets the hasMetrics.
     * @param hasMetrics The hasMetrics to set
     */
    public void setHasMetrics(Boolean hasMetrics) {
        this.hasMetrics = hasMetrics;
    }

    /**
     * Returns the showAlerts.
     * @return Boolean
     */
    public Boolean getShowAlerts() {
        return showAlerts;
    }

    /**
     * Sets the showAlerts.
     * @param showAlerts The showAlerts to set
     */
    public void setShowAlerts(Boolean showAlerts) {
        this.showAlerts = showAlerts;
    }

    /**
     * If this is null, it's assumed that there is no parent, so the
     * caller should check getHasParentResource()
     * 
     * 
     * @return Integer
     */
    public Integer getParentResourceId() {
        return parentResourceId;
    }

    /**
     * The parent resource, if any, of the current resource.  For a webapp, that
     * might be the server itself or it could be another service, such as an
     * EAR.  
     * 
     * @return String
     */
    public String getParentResourceName() {
        return parentResourceName;
    }

    /**
     * @return Integer
     */
    public Integer getResourceId() {
        return getEntityId().getId();
    }

    /**
     * @return String
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * @return String
     */
    public String getResourceEntityTypeName() {
        return resourceEntityTypeName;
    }

    /**
     * Sets the parentResourceId.
     * @param parentResourceId The parentResourceId to set
     */
    public void setParentResourceId(Integer parentResourceId) {
        this.parentResourceId = parentResourceId;
    }

    /**
     * Sets the parentResourceName.
     * @param parentResourceName The parentResourceName to set
     */
    public void setParentResourceName(String parentResourceName) {
        this.parentResourceName = parentResourceName;
    }

    /**
     * Sets the resourceName.
     * @param resourceName The resourceName to set
     */
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    /**
     * Sets the resourceEntityTypeName.
     * @param resourceEntityTypeName The resourceEntityTypeName to set
     */
    public void setResourceEntityTypeName(String resourceEntityTypeName) {
        this.resourceEntityTypeName = resourceEntityTypeName;
    }

    /**
     * @return Integer
     */
    public Integer getParentResourceTypeId() {
        return parentResourceTypeId;
    }

    /**
     * @return Integer
     */
    public Integer getResourceTypeId() {
        return new Integer(getEntityId().getType());
    }

    /**
     * Sets the parentResourceTypeId.
     * @param parentResourceTypeId The parentResourceTypeId to set
     */
    public void setParentResourceTypeId(Integer parentResourceTypeId) {
        this.parentResourceTypeId = parentResourceTypeId;
    }

    /**
     * Returns the resourceTypeName.
     * @return String
     */
    public String getResourceTypeName() {
        return resourceTypeName;
    }

    /**
     * Sets the resourceTypeName.
     * @param resourceTypeName The resourceTypeName to set
     */
    public void setResourceTypeName(String resourceTypeName) {
        this.resourceTypeName = resourceTypeName;
    }
    
    /**
     * @return Boolean
     */
    public Boolean getHasParentResource() {
        return hasParentResource;
    }

    /**
     * Sets the hasParentResource.
     * @param hasParentResource The hasParentResource to set
     */
    public void setHasParentResource(Boolean hasParentResource) {
        this.hasParentResource = hasParentResource;
    }

    /**
     * @return Boolean
     */
    public Boolean getMonitorable() {
        return monitorable;
    }

    /**
     * Sets the monitorable flag.
     * @param monitorable The monitorable to set
     */
    public void setMonitorable(Boolean monitorable) {
        this.monitorable = monitorable;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append(super.toString());
        sb.append("(showAlerts=").append(showAlerts);
        sb.append(",hasMetrics=").append(hasMetrics);
        sb.append(",resourceName=").append(resourceName);
        sb.append(",parentResourceName=").append(parentResourceName);
        sb.append(",parentResourceId=").append(parentResourceId);
        sb.append(",parentResourceTypeId=").append(parentResourceTypeId);
        sb.append(")");
        return sb.toString();
    }
}
