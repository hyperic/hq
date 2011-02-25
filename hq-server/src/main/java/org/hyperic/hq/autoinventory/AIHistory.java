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

package org.hyperic.hq.autoinventory;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hyperic.hq.appdef.shared.AppdefEntityID;

@Entity
@Table(name="EAM_AUTOINV_HISTORY")
public class AIHistory implements Serializable
{
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL")
    @Version
    private Long version;
    
    @Column(name="GROUP_ID")
    private Integer groupId;
    
    @Column(name="BATCH_ID")
    private Integer batchId;
    
    @Column(name="ENTITY_TYPE",nullable=false)
    private Integer entityType;
    
    @Column(name="ENTITY_ID",nullable=false)
    private Integer entityId;
    
    @Column(name="SUBJECT",nullable=false,length=32)
    private String subject;
    
    @Column(name="SCHEDULED",nullable=false)
    private boolean scheduled;
    
    @Column(name="DATE_SCHEDULED",nullable=false)
    private long dateScheduled;
    
    @Column(name="STARTTIME",nullable=false)
    private long startTime;
    
    @Column(name="STATUS",nullable=false,length=64)
    private String status;
    
    @Column(name="ENDTIME",nullable=false)
    private long endTime;
    
    @Column(name="DURATION",nullable=false)
    private long duration;
    
    @Column(name="SCANNAME",length=100)
    @Index(name="AI_HIST_SCANNAME_IDX")
    private String scanName;
    
    @Column(name="SCANDESC",length=200)
    private String scanDesc;
    
    @Column(name="DESCRIPTION",length=500)
    private String description;
    
    @Column(name="MESSAGE",length=500)
    private String message;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="CONFIG",nullable=false)
    private byte[] config;

    /**
     * default constructor
     */
    public AIHistory()
    {
        super();
    }


    public Integer getGroupId()
    {
        return this.groupId;
    }

    public void setGroupId(Integer groupId)
    {
        this.groupId = groupId;
    }

    public Integer getBatchId()
    {
        return this.batchId;
    }

    public void setBatchId(Integer batchId)
    {
        this.batchId = batchId;
    }

    public Integer getEntityType()
    {
        return this.entityType;
    }

    public void setEntityType(Integer entityType)
    {
        this.entityType = entityType;
    }

    public Integer getEntityId()
    {
        return this.entityId;
    }

    public void setEntityId(Integer entityId)
    {
        this.entityId = entityId;
    }

    public String getSubject()
    {
        return this.subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public boolean isScheduled()
    {
        return this.scheduled;
    }

    public Boolean getScheduled()
    {
        return new Boolean(isScheduled());
    }

    public void setScheduled(boolean scheduled)
    {
        this.scheduled = scheduled;
    }

    public void setScheduled(Boolean scheduled)
    {
        setScheduled(scheduled.booleanValue());
    }

    public long getDateScheduled()
    {
        return this.dateScheduled;
    }

    public void setDateScheduled(long dateScheduled)
    {
        this.dateScheduled = dateScheduled;
    }

    public long getStartTime()
    {
        return this.startTime;
    }

    public void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }

    public String getStatus()
    {
        return this.status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public long getEndTime()
    {
        return this.endTime;
    }

    public void setEndTime(long endTime)
    {
        this.endTime = endTime;
    }

    public long getDuration()
    {
        return this.duration;
    }

    public void setDuration(long duration)
    {
        this.duration = duration;
    }

    public String getScanName()
    {
        return this.scanName;
    }

    public void setScanName(String scanName)
    {
        this.scanName = scanName;
    }

    public String getScanDesc()
    {
        return this.scanDesc;
    }

    public void setScanDesc(String scanDesc)
    {
        this.scanDesc = scanDesc;
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getMessage()
    {
        return this.message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public byte[] getConfig()
    {
        return this.config;
    }

    public void setConfig(byte[] config)
    {
        this.config = config;
    }

    public String getEntityName()
    {
        AppdefEntityID id = new AppdefEntityID(getEntityType().intValue(),
                                               getEntityId());
        return id.toString();
    }

    public void setEntityName()
    {
        // no op
    }

    public ScanConfigurationCore getConfigObj() throws AutoinventoryException
    {
        return ScanConfigurationCore.deserialize(getConfig());
    }

    public void setConfigObj(ScanConfigurationCore core)
    throws AutoinventoryException
    {
        setConfig(core.serialize());
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

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof AIHistory)) {
            return false;
        }
        Integer objId = ((AIHistory)obj).getId();
  
        return getId() == objId ||
        (getId() != null && 
         objId != null && 
         getId().equals(objId));     
    }

    public int hashCode() {
        int result = 17;
        result = 37*result + (getId() != null ? getId().hashCode() : 0);
        return result;      
    }

}
