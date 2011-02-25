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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.IpValue;

@Entity
@Table(name="EAM_AIQ_IP", uniqueConstraints = { @UniqueConstraint(name = "aIIpId", columnNames = { "AIQ_PLATFORM_ID",
"ADDRESS" }) })
public class AIIp implements ContainerManagedTimestampTrackable, Serializable
{
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL")
    @Version
    private Long version;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="AIQ_PLATFORM_ID")
    private AIPlatform aIPlatform;
    
    @Column(name="ADDRESS",nullable=false,length=64)
    private String address;
    
    @Column(name="NETMASK",length=64)
    private String netmask;
    
    @Column(name="MAC_ADDRESS",length=64)
    private String macAddress;
    
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
   
    public AIIp() {
        super();
    }

    public AIIp(AIIpValue ipv) {
        super();
        setQueueStatus(new Integer(ipv.getQueueStatus()));
        setDiff(ipv.getDiff());
        setIgnored(ipv.getIgnored());
        setAddress(ipv.getAddress());
        setMacAddress(ipv.getMACAddress());
        setNetmask(ipv.getNetmask());
    }

    public AIPlatform getAIPlatform() {
        return aIPlatform;
    }

    public void setAIPlatform(AIPlatform aIPlatform) {
        this.aIPlatform = aIPlatform;
    }

    public int getQueueStatus() {
        return queueStatus != null ? queueStatus.intValue() : 0;
    }

    public void setQueueStatus(Integer queueStatus) {
        this.queueStatus = queueStatus;
    }

    public long getDiff() {
        return diff;
    }

    public void setDiff(long diff) {
        this.diff = diff;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    /**
     * legacy DTO pattern
     * @deprecated use (this) AIIp object instead
     */
    public AIIpValue getAIIpValue()
    {
        
            AIIpValue aIIpValue = new AIIpValue();
            aIIpValue.setId(getId());
            aIIpValue.setAddress(getAddress());
            aIIpValue.setMACAddress(getMacAddress());
            aIIpValue.setNetmask(getNetmask());
            aIIpValue.setCTime(getCreationTime());
        aIIpValue.setQueueStatus(getQueueStatus());
        aIIpValue.setDiff(getDiff());
        aIIpValue.setIgnored(isIgnored());
        aIIpValue.setMTime(getModifiedTime());
        return aIIpValue;
    }
    
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String MACAddress) {
        macAddress = MACAddress;
    }

    /**
     * convenience method for copying simple values
     * from the legacy Value Object
     *
     * @deprecated
     */
    public void setIpValue(IpValue valueHolder) {
        setAddress(valueHolder.getAddress());
        setNetmask(valueHolder.getNetmask());
        setMacAddress(valueHolder.getMACAddress());
    }

    /**
     * legacy DTO pattern
     * @deprecated use (this) Ip Object instead
     */
    public IpValue getIpValue()
    {
        IpValue ipValue = new IpValue();
        ipValue.setAddress(getAddress());
        ipValue.setNetmask(getNetmask());
        ipValue.setMACAddress(getMacAddress());
        ipValue.setId(getId());
        ipValue.setMTime(getModifiedTime());
        ipValue.setCTime(getCreationTime());
        return ipValue;
    }

   

    public boolean equals(Object obj) {
        if (!(obj instanceof AIIp) || !super.equals(obj)) {
            return false;
        }
        AIIp o = (AIIp) obj;
        return ((aIPlatform == o.getAIPlatform()) ||
                (aIPlatform != null && o.getAIPlatform() != null &&
                 aIPlatform.equals(o.getAIPlatform())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result + (aIPlatform != null ? aIPlatform.hashCode() : 0);

        return result;
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

    public Long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public Long getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }
    
    
}
