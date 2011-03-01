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
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;

@Entity
@Table(name="EAM_AIQ_SERVER",uniqueConstraints = { @UniqueConstraint(name = "AIQ_SERVER_UNIQUE_IDX", columnNames = { "AIQ_PLATFORM_ID",
"AUTOINVENTORYIDENTIFIER" }) })
public class AIServer implements ContainerManagedTimestampTrackable, Serializable
{
    @Column(name="ACTIVE",length=1)
    private Character active= 't';

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="AIQ_PLATFORM_ID")
    private AIPlatform aIPlatform;
    
    @Column(name="AUTOINVENTORYIDENTIFIER",length=255)
    private String autoinventoryIdentifier;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="CONTROL_CONFIG",length=256)
    private byte[] controlConfig;
    
    @Column(name="CTIME")
    private Long creationTime;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="CUSTOM_PROPERTIES")
    private byte[] customProperties;
    
    @Column(name="DESCRIPTION",length=300)
    private String description;
    
    @Column(name="DIFF")
    private long diff;
    
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;
    
    @Column(name="IGNORED")
    private boolean ignored;
    
    @Column(name="INSTALLPATH",length=255)
    private String installPath;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="MEASUREMENT_CONFIG",length=256)
    private byte[] measurementConfig;
    
    @Column(name="MTIME")
    private Long modifiedTime;
    
    @Column(name="NAME",length=255,nullable=false)
    @Index(name="AIQ_SERVER_NAME")
    private String name;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="PRODUCT_CONFIG",length=256)
    private byte[] productConfig;
    
    @Column(name="QUEUESTATUS")
    private Integer queueStatus;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="RESPONSETIME_CONFIG",length=256)
    private byte[] responseTimeConfig;
    
    @Column(name="SERVERTYPENAME",length=200,nullable=false)
    private String serverTypeName;
    
    @Column(name="SERVICESAUTOMANAGED")
    private boolean servicesAutomanaged;
    
    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;
    
    @Column(name="VIRTUAL")
    private boolean virtual;

    /**
     * default constructor
     */
    public AIServer()
    {
        super();
    }

    public AIServer(AIServerValue sv)
    {
        super();
        setAIServerValue(sv);
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
        AIServer other = (AIServer) obj;
        if (aIPlatform == null) {
            if (other.aIPlatform != null)
                return false;
        } else if (!aIPlatform.equals(other.aIPlatform))
            return false;
        if (autoinventoryIdentifier == null) {
            if (other.autoinventoryIdentifier != null)
                return false;
        } else if (!autoinventoryIdentifier.equals(other.autoinventoryIdentifier))
            return false;
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
        return true;
    }

    public Character getActive()
    {
        return this.active;
    }

    public AIPlatform getAIPlatform()
    {
        return this.aIPlatform;
    }

    /**
     * legacy DTO pattern
     * @deprecated use (this) AIServer object instead
     * @return
     */
    public AIServerValue getAIServerValue()
    {
        AIServerValue aIServerValue = new AIServerValue();
        aIServerValue.setQueueStatus(getQueueStatus());
        aIServerValue.setCustomProperties(getCustomProperties());
        aIServerValue.setProductConfig(getProductConfig());
        aIServerValue.setControlConfig(getControlConfig());
        aIServerValue.setMeasurementConfig(getMeasurementConfig());
        aIServerValue.setResponseTimeConfig(getResponseTimeConfig());
        aIServerValue.setDiff(getDiff());
        aIServerValue.setIgnored(isIgnored());
        aIServerValue.setServerTypeName(
            (getServerTypeName() == null) ? "" : getServerTypeName());
        aIServerValue.setName(getName());
        aIServerValue.setAutoinventoryIdentifier(getAutoinventoryIdentifier());
        aIServerValue.setInstallPath(getInstallPath());
        aIServerValue.setDescription(getDescription());
        aIServerValue.setServicesAutomanaged(isServicesAutomanaged());
        aIServerValue.setId(getId());
        aIServerValue.setMTime(getModifiedTime());
        aIServerValue.setCTime(getCreationTime());
        aIServerValue.setVirtual(isVirtual());
        return aIServerValue;
    }

    /**
     * For compatibility
     */
    public AppdefResourceType getAppdefResourceType() {
        return null;
    }

    /**
     * For compatibility
     */
   public AppdefResourceValue getAppdefResourceValue() {
        return null;
    }

    public String getAutoinventoryIdentifier()
    {
        return this.autoinventoryIdentifier;
    }

    public byte[] getControlConfig()
    {
        return this.controlConfig;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public byte[] getCustomProperties()
    {
        return this.customProperties;
    }

    public String getDescription() {
        return description;
    }

    public long getDiff()
    {
        return this.diff;
    }

    public AppdefEntityID getEntityId()
    {
        return AppdefEntityID.newServerID(getId());
    }

    public Integer getId() {
        return id;
    }

    /**
     * @deprecated use isIgnored()
     * @return
     */
    public boolean getIgnored()
    {
        return isIgnored();
    }

    public String getInstallPath()
    {
        return this.installPath;
    }

    public byte[] getMeasurementConfig()
    {
        return this.measurementConfig;
    }

    public Long getModifiedTime() {
        return modifiedTime;
    }

    public String getName() {
        return name;
    }

    public byte[] getProductConfig()
    {
        return this.productConfig;
    }

    public int getQueueStatus()
    {
        return queueStatus != null ? queueStatus.intValue() : 0;
    }

    public byte[] getResponseTimeConfig()
    {
        return this.responseTimeConfig;
    }

    public String getServerTypeName()
    {
        return this.serverTypeName;
    }

    /**
     * @deprecated use isServicesAutomanaged()
     * @return
     */
    public boolean getServicesAutomanaged()
    {
        return isServicesAutomanaged();
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((aIPlatform == null) ? 0 : aIPlatform.hashCode());
        result = prime * result +
                 ((autoinventoryIdentifier == null) ? 0 : autoinventoryIdentifier.hashCode());
        result = prime * result + ((creationTime == null) ? 0 : creationTime.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

   

    public boolean isIgnored()
    {
        return this.ignored;
    }

    public boolean isServicesAutomanaged()
    {
        return this.servicesAutomanaged;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public void setActive(Character active)
    {
        this.active = active;
    }

    public void setAIPlatform(AIPlatform aIPlatform)
    {
        this.aIPlatform = aIPlatform;
    }

    public void setAIServerValue(AIServerValue valueHolder)
    {
        setQueueStatus( valueHolder.getQueueStatus() );
        setCustomProperties( valueHolder.getCustomProperties() );
        setProductConfig( valueHolder.getProductConfig() );
        setControlConfig( valueHolder.getControlConfig() );
        setMeasurementConfig( valueHolder.getMeasurementConfig() );
        setResponseTimeConfig( valueHolder.getResponseTimeConfig() );
        setDiff( valueHolder.getDiff() );
        setIgnored( valueHolder.getIgnored() );
        setServerTypeName( valueHolder.getServerTypeName() );
        setName( valueHolder.getName() );
        setAutoinventoryIdentifier( valueHolder.getAutoinventoryIdentifier() );
        setInstallPath( valueHolder.getInstallPath() );
        setDescription( valueHolder.getDescription() );
        setServicesAutomanaged( valueHolder.getServicesAutomanaged() );
        setVirtual(valueHolder.isVirtual());
    }

    public void setAutoinventoryIdentifier(String autoinventoryIdentifier)
    {
        this.autoinventoryIdentifier = autoinventoryIdentifier;
    }

    public void setControlConfig(byte[] controlConfig)
    {
        this.controlConfig = controlConfig;
    }
    
    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public void setCustomProperties(byte[] customProperties)
    {
        this.customProperties = customProperties;
    }


    public void setDescription(String description) {
        this.description = description;
    }

    public void setDiff(long diff)
    {
        this.diff = diff;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setIgnored(boolean ignored)
    {
        this.ignored = ignored;
    }

    public void setInstallPath(String installPath)
    {
        this.installPath = installPath;
    }

    public void setMeasurementConfig(byte[] measurementConfig)
    {
        this.measurementConfig = measurementConfig;
    }
   
   public void setModifiedTime(Long modifiedTime) {
    this.modifiedTime = modifiedTime;
}
   
   public void setName(String name) {
    this.name = name;
}

    public void setProductConfig(byte[] productConfig)
    {
        this.productConfig = productConfig;
    }
    
    /**
     * @deprecated use setQueueStatus(Integer)
     * @param queueStatus
     */
    public void setQueueStatus(int queueStatus)
    {
        setQueueStatus(new Integer(queueStatus));
    }
    
    public void setQueueStatus(Integer queueStatus)
    {
        this.queueStatus = queueStatus;
    }
    
    public void setResponseTimeConfig(byte[] responseTime_Config)
    {
        this.responseTimeConfig = responseTime_Config;
    }
    
    public void setServerTypeName(String serverTypeName)
    {
        this.serverTypeName = serverTypeName;
    }
    
    public void setServicesAutomanaged(boolean servicesAutomanaged)
    {
        this.servicesAutomanaged = servicesAutomanaged;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }
   
}

