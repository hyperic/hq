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
import org.hibernate.annotations.Parameter;
import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.IpValue;

@Entity
@Table(name = "EAM_AIQ_IP", uniqueConstraints = { @UniqueConstraint(name = "aIIpId", columnNames = { "AIQ_PLATFORM_ID",
                                                                                                    "ADDRESS" }) })
public class AIIp implements ContainerManagedTimestampTrackable, Serializable {
    @Column(name = "ADDRESS", nullable = false, length = 64)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AIQ_PLATFORM_ID")
    private AIPlatform aIPlatform;

    @Column(name = "CTIME")
    private Long creationTime;

    @Column(name = "DIFF")
    private long diff;

    @Id
    @GeneratedValue(generator = "combo")
    @GenericGenerator(name = "combo", parameters = { @Parameter(name = "sequence", value = "EAM_AIQ_IP_ID_SEQ") }, 
        strategy = "org.hyperic.hibernate.id.ComboGenerator")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "IGNORED")
    private boolean ignored;

    @Column(name = "MAC_ADDRESS", length = 64)
    private String macAddress;

    @Column(name = "MTIME")
    private Long modifiedTime;

    @Column(name = "NETMASK", length = 64)
    private String netmask;

    @Column(name = "QUEUESTATUS")
    private Integer queueStatus;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

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
        AIIp other = (AIIp) obj;
        if (aIPlatform == null) {
            if (other.aIPlatform != null)
                return false;
        } else if (!aIPlatform.equals(other.aIPlatform))
            return false;
        if (address == null) {
            if (other.address != null)
                return false;
        } else if (!address.equals(other.address))
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

    public String getAddress() {
        return address;
    }

    /**
     * legacy DTO pattern
     * @deprecated use (this) AIIp object instead
     */
    public AIIpValue getAIIpValue() {

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

    public AIPlatform getAIPlatform() {
        return aIPlatform;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public long getDiff() {
        return diff;
    }

    public Integer getId() {
        return id;
    }

    /**
     * legacy DTO pattern
     * @deprecated use (this) Ip Object instead
     */
    public IpValue getIpValue() {
        IpValue ipValue = new IpValue();
        ipValue.setAddress(getAddress());
        ipValue.setNetmask(getNetmask());
        ipValue.setMACAddress(getMacAddress());
        ipValue.setId(getId());
        ipValue.setMTime(getModifiedTime());
        ipValue.setCTime(getCreationTime());
        return ipValue;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public Long getModifiedTime() {
        return modifiedTime;
    }

    public String getNetmask() {
        return netmask;
    }

    public int getQueueStatus() {
        return queueStatus != null ? queueStatus.intValue() : 0;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((aIPlatform == null) ? 0 : aIPlatform.hashCode());
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        result = prime * result + ((creationTime == null) ? 0 : creationTime.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setAIPlatform(AIPlatform aIPlatform) {
        this.aIPlatform = aIPlatform;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
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

    /**
     * convenience method for copying simple values from the legacy Value Object
     * 
     * @deprecated
     */
    public void setIpValue(IpValue valueHolder) {
        setAddress(valueHolder.getAddress());
        setNetmask(valueHolder.getNetmask());
        setMacAddress(valueHolder.getMACAddress());
    }

    public void setMacAddress(String MACAddress) {
        macAddress = MACAddress;
    }

    public void setModifiedTime(Long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public void setQueueStatus(Integer queueStatus) {
        this.queueStatus = queueStatus;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
