/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2009], Hyperic, Inc. 
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

package org.hyperic.hq.autoinventory;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.inventory.domain.Resource;

@Entity
@Table(name = "EAM_AIQ_SERVICE")
public class AIService implements ContainerManagedTimestampTrackable, Serializable {
    @Basic(fetch = FetchType.LAZY)
    @Lob
    @Column(name = "CONTROL_CONFIG", length = 256)
    private byte[] controlConfig;

    @Column(name = "CTIME")
    private Long creationTime;

    @Basic(fetch = FetchType.LAZY)
    @Lob
    @Column(name = "CUSTOM_PROPERTIES")
    private byte[] customProperties;

    @Column(name = "DESCRIPTION", length = 300)
    private String description;

    @Column(name = "DIFF")
    private long diff;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "IGNORED")
    private boolean ignored;

    @Basic(fetch = FetchType.LAZY)
    @Lob
    @Column(name = "MEASUREMENT_CONFIG", length = 256)
    private byte[] measurementConfig;

    @Column(name = "MTIME")
    private Long modifiedTime;

    @Column(name = "NAME", nullable = false, length = 255)
    @Index(name = "AIQ_SERVICE_NAME_IDX")
    private String name;

    @Basic(fetch = FetchType.LAZY)
    @Lob
    @Column(name = "PRODUCT_CONFIG", length = 256)
    private byte[] productConfig;

    @Column(name = "QUEUESTATUS")
    private Integer queueStatus;

    @Basic(fetch = FetchType.LAZY)
    @Lob
    @Column(name = "RESPONSETIME_CONFIG", length = 256)
    private byte[] responseTimeConfig;

    @ManyToOne
    @JoinColumn(name = "SERVER_ID", nullable = false)
    @Index(name = "AIQ_SVC_SERVER_ID_IDX")
    private Resource server;

    @Column(name = "SERVICETYPENAME", nullable = false, length = 200)
    private String serviceTypeName;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    /**
     * default constructor
     */
    public AIService() {
        super();
    }

    public AIService(AIServiceValue sv) {
        super();
        setAIServiceValue(sv);
    }

    /**
     * @see org.hyperic.hibernate.ContainerManagedTimestampTrackable#allowContainerManagedLastModifiedTime()
     * @return <code>true</code> by default.
     */
    public boolean allowContainerManagedCreationTime() {
        return true;
    }

    /**
     * @see org.hyperic.hibernate.ContainerManagedTimestampTrackable#allowContainerManagedLastModifiedTime()
     * @return <code>true</code> by default.
     */
    public boolean allowContainerManagedLastModifiedTime() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AIService other = (AIService) obj;
        if (creationTime == null) {
            if (other.creationTime != null)
                return false;
        } else if (!creationTime.equals(other.creationTime))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (server == null) {
            if (other.server != null)
                return false;
        } else if (!server.equals(other.server))
            return false;
        return true;
    }

    /**
     * legacy DTO pattern
     * @deprecated use (this) AIService object instead
     * @return
     */
    public AIServiceValue getAIServiceValue() {
        AIServiceValue aIServiceValue = new AIServiceValue();
        aIServiceValue.setServerId(getServerId());
        aIServiceValue.setServiceTypeName((getServiceTypeName() == null) ? ""
                                                                        : getServiceTypeName());
        aIServiceValue.setCustomProperties(getCustomProperties());
        aIServiceValue.setProductConfig(getProductConfig());
        aIServiceValue.setControlConfig(getControlConfig());
        aIServiceValue.setMeasurementConfig(getMeasurementConfig());
        aIServiceValue.setResponseTimeConfig(getResponseTimeConfig());
        aIServiceValue.setName(getName());
        aIServiceValue.setDescription(getDescription());
        aIServiceValue.setId(getId());
        aIServiceValue.setMTime(getModifiedTime());
        aIServiceValue.setCTime(getCreationTime());
        return aIServiceValue;
    }

    public byte[] getControlConfig() {
        return this.controlConfig;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public byte[] getCustomProperties() {
        return this.customProperties;
    }

    public String getDescription() {
        return description;
    }

    public long getDiff() {
        return this.diff;
    }

    public AppdefEntityID getEntityId() {
        return AppdefEntityID.newServiceID(getId());
    }

    public Integer getId() {
        return id;
    }

    public byte[] getMeasurementConfig() {
        return this.measurementConfig;
    }

    public Long getModifiedTime() {
        return modifiedTime;
    }

    public String getName() {
        return name;
    }

    public byte[] getProductConfig() {
        return this.productConfig;
    }

    public Integer getQueueStatus() {
        return this.queueStatus;
    }

    public byte[] getResponseTimeConfig() {
        return this.responseTimeConfig;
    }

    public Resource getServer() {
        return server;
    }

    public int getServerId() {
        return getServer() != null && getServer().getId() != null ? getServer().getId().intValue()
                                                                 : 0;
    }

    public String getServiceTypeName() {
        return this.serviceTypeName;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((creationTime == null) ? 0 : creationTime.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((server == null) ? 0 : server.hashCode());
        return result;
    }

    public boolean isIgnored() {
        return this.ignored;
    }

    public void setAIServiceValue(AIServiceValue valueHolder) {
        setServiceTypeName(valueHolder.getServiceTypeName());
        setCustomProperties(valueHolder.getCustomProperties());
        setProductConfig(valueHolder.getProductConfig());
        setControlConfig(valueHolder.getControlConfig());
        setMeasurementConfig(valueHolder.getMeasurementConfig());
        setResponseTimeConfig(valueHolder.getResponseTimeConfig());
        setName(valueHolder.getName());
        setDescription(valueHolder.getDescription());
    }

    public void setControlConfig(byte[] controlConfig) {
        this.controlConfig = controlConfig;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public void setCustomProperties(byte[] customProperties) {
        this.customProperties = customProperties;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDiff(long diff) {
        this.diff = diff;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    public void setMeasurementConfig(byte[] measurementConfig) {
        this.measurementConfig = measurementConfig;
    }

    public void setModifiedTime(Long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProductConfig(byte[] productConfig) {
        this.productConfig = productConfig;
    }

    public void setQueueStatus(Integer queueStatus) {
        this.queueStatus = queueStatus;
    }

    public void setResponseTimeConfig(byte[] responseTimeConfig) {
        this.responseTimeConfig = responseTimeConfig;
    }

    public void setServer(Resource server) {
        this.server = server;
    }

    public void setServiceTypeName(String serviceTypeName) {
        this.serviceTypeName = serviceTypeName;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
