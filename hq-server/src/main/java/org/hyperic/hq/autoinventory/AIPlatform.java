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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.product.PlatformDetector;

@Entity
@Table(name="EAM_AIQ_PLATFORM")
public class AIPlatform implements ContainerManagedTimestampTrackable, Serializable
{
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;
    
    @Column(name="NAME",nullable=false,length=255,unique=true)
    private String name;
    
    @Column(name="DESCRIPTION",length=300)
    private String description;
    
    @Column(name="OS",length=80)
    private String platformTypeName;
    
    @Column(name="OSVERSION",length=80)
    private String osversion;
    
    @Column(name="ARCH",length=80)
    private String arch;
    
    @Column(name="FQDN",nullable=false,length=200,unique=true)
    private String fqdn;
    
    @Column(name="AGENTTOKEN",nullable=false,length=100)
    @Index(name="AIQ_PLATFORM_AGENTTOKEN_IDX")
    private String agentToken;
    
    @Column(name="CERTDN",length=200,unique=true)
    private String certdn;
    
    @Column(name="QUEUESTATUS")
    private Integer queueStatus;
    
    @Column(name="DIFF")
    private long diff;
    
    @Column(name="IGNORED")
    private boolean ignored;
    
    @Column(name="CTIME")
    private Long creationTime;
    
    @Column(name="MTIME")
    private Long modifiedTime;
    
    @Column(name="LASTAPPROVED")
    private Long lastApproved;
    
    @Column(name="LOCATION",length=100)
    private String location;
    
    @Column(name="CPU_SPEED")
    private Integer cpuSpeed;
    
    @Column(name="CPU_COUNT")
    private Integer cpuCount;
    
    @Column(name="RAM")
    private Integer ram;
    
    @Column(name="GATEWAY",length=64)
    private String gateway;
    
    @Column(name="DHCP_SERVER",length=64)
    private String dhcpServer;
    
    @Column(name="DNS_SERVER",length=64)
    private String dnsServer;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="CUSTOM_PROPERTIES",columnDefinition="BLOB")
    private byte[] customProperties;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="PRODUCT_CONFIG",length=256,columnDefinition="BLOB")
    private byte[] productConfig;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="CONTROL_CONFIG",length=256,columnDefinition="BLOB")
    private byte[] controlConfig;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="MEASUREMENT_CONFIG",length=256,columnDefinition="BLOB")
    private byte[] measurementConfig;
    
    @OneToMany(fetch=FetchType.LAZY,mappedBy="aIPlatform",cascade=CascadeType.ALL)
    @OnDelete(action=OnDeleteAction.CASCADE)
    private Collection<AIIp> aiips = new ArrayList<AIIp>();
    
    @OneToMany(fetch=FetchType.LAZY,mappedBy="aIPlatform",cascade=CascadeType.ALL,orphanRemoval=true)
    @OnDelete(action=OnDeleteAction.CASCADE)
    private Collection<AIServer> aiservers =  new ArrayList<AIServer>();
 
   
     

    public AIPlatform()
    {
        super();
    }

    public AIPlatform(AIPlatformValue apv)
    {
        super();
        setFqdn(apv.getFqdn());
        setCertdn(apv.getCertdn());
        setQueueStatus(apv.getQueueStatus());
        setDescription(apv.getDescription());
        setDiff(apv.getDiff());
        setPlatformTypeName(apv.getPlatformTypeName());
        setLastApproved(new Long(0));
        setIgnored(false);
        setName(apv.getName());
        setAgentToken (apv.getAgentToken());
        setCpuCount   (apv.getCpuCount());
        setCustomProperties(apv.getCustomProperties());
        setProductConfig(apv.getProductConfig());
        setMeasurementConfig(apv.getMeasurementConfig());
        setControlConfig(apv.getControlConfig());
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

    public long getCreationTime()
    {
        return creationTime;
    }

    public void setCreationTime(Long creationTime)
    {
        this.creationTime = creationTime;
    }

    public long getModifiedTime()
    {
        return modifiedTime;
    }

    public void setModifiedTime(Long modifiedTime)
    {
        this.modifiedTime = modifiedTime;
    }
    
    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

   

    public String getLocation()
    {
        return this.location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

  
    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

   
    
   
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getFqdn()
    {
        return this.fqdn;
    }

    public void setFqdn(String fqDN)
    {
        this.fqdn = fqDN;
        if (getName() == null) {
            setName(fqDN);
        }
    }

    public AppdefEntityID getEntityId()
    {
        return AppdefEntityID.newPlatformID(getId());
    }

    public String getCertdn()
    {
        return this.certdn;
    }

    public void setCertdn(String certDN)
    {
        this.certdn = certDN;
    }

    public Integer getCpuCount()
    {
        return this.cpuCount;
    }

    public void setCpuCount(Integer cpuCount)
    {
        this.cpuCount = cpuCount;
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result + (fqdn != null ? fqdn.hashCode() : 0);
        result = 37*result + (certdn != null ? certdn.hashCode() : 0);

        return result;
    }

    public String getPlatformTypeName()
    {
        return this.platformTypeName;
    }

    public void setPlatformTypeName(String platformTypeName)
    {
        this.platformTypeName = platformTypeName;
    }

    public String getOsversion()
    {
        return this.osversion;
    }

    public void setOsversion(String osversion)
    {
        this.osversion = osversion;
    }

    public String getArch()
    {
        return this.arch;
    }

    public void setArch(String arch)
    {
        this.arch = arch;
    }

    public String getAgentToken()
    {
        return this.agentToken;
    }

    public void setAgentToken(String agentToken)
    {
        this.agentToken = agentToken;
    }

    public int getQueueStatus()
    {
        return queueStatus;
    }

    public void setQueueStatus(Integer queueStatus)
    {
        this.queueStatus = queueStatus;
    }

    /**
     * @depreated use setQueueStatus(Integer)
     * @param queueStatus
     */
    public void setQueueStatus(int queueStatus)
    {
        setQueueStatus(new Integer(queueStatus));
    }

    public long getDiff()
    {
        return this.diff;
    }

    public void setDiff(long diff)
    {
        this.diff = diff;
    }

    public void setDiff(Long diff)
    {
        this.diff = diff;
    }

    public boolean isIgnored()
    {
        return this.ignored;
    }

    public void setIgnored(boolean ignored)
    {
        this.ignored = ignored;
    }

    public long getLastApproved()
    {
        return lastApproved;
    }

    public void setLastApproved(Long lastApproved)
    {
        this.lastApproved = lastApproved;
    }

    public Integer getCpuSpeed()
    {
        return this.cpuSpeed;
    }

    public void setCpuSpeed(Integer cpuSpeed)
    {
        this.cpuSpeed = cpuSpeed;
    }

    public Integer getRam()
    {
        return this.ram;
    }

    public void setRam(Integer ram)
    {
        this.ram = ram;
    }

    public String getGateway()
    {
        return this.gateway;
    }

    public void setGateway(String gateway)
    {
        this.gateway = gateway;
    }

    public String getDhcpServer()
    {
        return this.dhcpServer;
    }

    public void setDhcpServer(String dhcpServer)
    {
        this.dhcpServer = dhcpServer;
    }

    public String getDnsServer()
    {
        return this.dnsServer;
    }

    public void setDnsServer(String dnsServer)
    {
        this.dnsServer = dnsServer;
    }

    public byte[] getCustomProperties()
    {
        return this.customProperties;
    }

    public void setCustomProperties(byte[] customProperties)
    {
        this.customProperties = customProperties;
    }

    public byte[] getProductConfig()
    {
        return this.productConfig;
    }

    public void setProductConfig(byte[] productConfig)
    {
        this.productConfig = productConfig;
    }

    public byte[] getControlConfig()
    {
        return this.controlConfig;
    }

    public void setControlConfig(byte[] controlConfig)
    {
        this.controlConfig = controlConfig;
    }

    public byte[] getMeasurementConfig()
    {
        return this.measurementConfig;
    }

    public void setMeasurementConfig(byte[] measurementConfig)
    {
        this.measurementConfig = measurementConfig;
    }

    public Collection<AIIp> getAIIps()
    {
        return this.aiips;
    }

    public void setAIIps(Collection<AIIp> aiips)
    {
        this.aiips = aiips;
    }

    public Collection<AIServer> getAIServers()
    {
        return this.aiservers;
    }

    public void setAIServers(Collection<AIServer> aiservers)
    {
        this.aiservers = aiservers;
    }

    public void addAIServer(AIServer server)
    {
        if (server != null) {
            aiservers.add(server);
            server.setAIPlatform(this);
        }
    }

    public boolean isPlatformDevice() {
        return !PlatformDetector.isSupportedPlatform(getPlatformTypeName());
    }
    
    /**
     * @deprecated use (this) AIPlatformValue object
     * @return
     */
    public AIPlatformValue getAIPlatformValue()
    {
        AIPlatformValue aipValue = new AIPlatformValue();
        aipValue.setAgentToken(
            (getAgentToken() == null) ? "" : getAgentToken());
        aipValue.setQueueStatus(getQueueStatus());
        aipValue.setCustomProperties(getCustomProperties());
        aipValue.setProductConfig(getProductConfig());
        aipValue.setControlConfig(getControlConfig());
        aipValue.setMeasurementConfig(getMeasurementConfig());
        aipValue.setDiff(getDiff());
        aipValue.setIgnored(isIgnored());
        aipValue.setPlatformTypeName(
            (getPlatformTypeName() == null) ? "" : getPlatformTypeName());
        aipValue.setLastApproved(new Long(getLastApproved()));
        aipValue.setCertdn(getCertdn());
        aipValue.setFqdn(getFqdn());
        aipValue.setName(getName());
        aipValue.setLocation(getLocation());
        aipValue.setDescription(getDescription());
        aipValue.setCpuCount(getCpuCount());
        aipValue.setId(getId());
        aipValue.setMTime(getModifiedTime());
        aipValue.setCTime(getCreationTime());
        aipValue.removeAllAIIpValues();
        Iterator<AIIp> iAIIpValue = getAIIps().iterator();
        while (iAIIpValue.hasNext()){
            aipValue.addAIIpValue(
                (iAIIpValue.next()).getAIIpValue() );
        }
        aipValue.cleanAIIpValue();
        aipValue.removeAllAIServerValues();
        Iterator<AIServer> iAIServerValue = getAIServers().iterator();
        while (iAIServerValue.hasNext()){
            aipValue.addAIServerValue(
                (iAIServerValue.next()).getAIServerValue() );
        }
        aipValue.cleanAIServerValue();
        return aipValue;
    }

    public boolean equals(Object obj)
    {
        return (obj instanceof AIPlatform) && super.equals(obj);
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

}