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
import org.hibernate.annotations.Parameter;
import org.hyperic.hq.appdef.shared.AppdefEntityID;

@Entity
@Table(name="EAM_AUTOINV_HISTORY")
public class AIHistory implements Serializable
{
    @Column(name="BATCH_ID")
    private Integer batchId;

    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="CONFIG",nullable=false)
    private byte[] config;
    
    @Column(name="DATE_SCHEDULED",nullable=false)
    private long dateScheduled;
    
    @Column(name="DESCRIPTION",length=500)
    private String description;
    
    @Column(name="DURATION",nullable=false)
    private long duration;
    
    @Column(name="ENDTIME",nullable=false)
    private long endTime;
    
    @Column(name="ENTITY_ID",nullable=false)
    private Integer entityId;
    
    @Column(name="ENTITY_TYPE",nullable=false)
    private Integer entityType;
    
    @Column(name="GROUP_ID")
    private Integer groupId;
    
    @Id
    @GeneratedValue(generator = "combo")
    @GenericGenerator(name = "combo", parameters = { @Parameter(name = "sequence", value = "EAM_AUTOINV_HISTORY_ID_SEQ") }, 
        strategy = "org.hyperic.hibernate.id.ComboGenerator")
    @Column(name = "ID")
    private Integer id;
    
    @Column(name="MESSAGE",length=500)
    private String message;
    
    @Column(name="SCANDESC",length=200)
    private String scanDesc;
    
    @Column(name="SCANNAME",length=100)
    @Index(name="AI_HIST_SCANNAME_IDX")
    private String scanName;
    
    @Column(name="SCHEDULED",nullable=false)
    private boolean scheduled;
    
    @Column(name="STARTTIME",nullable=false)
    private long startTime;
    
    @Column(name="STATUS",nullable=false,length=64)
    private String status;
    
    @Column(name="SUBJECT",nullable=false,length=32)
    private String subject;
    
    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;

    /**
     * default constructor
     */
    public AIHistory()
    {
        super();
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

    public Integer getBatchId()
    {
        return this.batchId;
    }

    public byte[] getConfig()
    {
        return this.config;
    }

    public ScanConfigurationCore getConfigObj() throws AutoinventoryException
    {
        return ScanConfigurationCore.deserialize(getConfig());
    }

    public long getDateScheduled()
    {
        return this.dateScheduled;
    }

    public String getDescription()
    {
        return this.description;
    }

    public long getDuration()
    {
        return this.duration;
    }

    public long getEndTime()
    {
        return this.endTime;
    }

    public Integer getEntityId()
    {
        return this.entityId;
    }

    public String getEntityName()
    {
        AppdefEntityID id = new AppdefEntityID(getEntityType().intValue(),
                                               getEntityId());
        return id.toString();
    }

    public Integer getEntityType()
    {
        return this.entityType;
    }

    public Integer getGroupId()
    {
        return this.groupId;
    }

    public Integer getId() {
        return id;
    }

    public String getMessage()
    {
        return this.message;
    }

    public String getScanDesc()
    {
        return this.scanDesc;
    }

    public String getScanName()
    {
        return this.scanName;
    }

    public Boolean getScheduled()
    {
        return new Boolean(isScheduled());
    }

    public long getStartTime()
    {
        return this.startTime;
    }

    public String getStatus()
    {
        return this.status;
    }

    public String getSubject()
    {
        return this.subject;
    }

    public Long getVersion() {
        return version;
    }

    public int hashCode() {
        int result = 17;
        result = 37*result + (getId() != null ? getId().hashCode() : 0);
        return result;      
    }

    public boolean isScheduled()
    {
        return this.scheduled;
    }

    public void setBatchId(Integer batchId)
    {
        this.batchId = batchId;
    }

    public void setConfig(byte[] config)
    {
        this.config = config;
    }

    public void setConfigObj(ScanConfigurationCore core)
    throws AutoinventoryException
    {
        setConfig(core.serialize());
    }

    public void setDateScheduled(long dateScheduled)
    {
        this.dateScheduled = dateScheduled;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setDuration(long duration)
    {
        this.duration = duration;
    }

    public void setEndTime(long endTime)
    {
        this.endTime = endTime;
    }

    public void setEntityId(Integer entityId)
    {
        this.entityId = entityId;
    }

    public void setEntityName()
    {
        // no op
    }

    public void setEntityType(Integer entityType)
    {
        this.entityType = entityType;
    }

    public void setGroupId(Integer groupId)
    {
        this.groupId = groupId;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public void setScanDesc(String scanDesc)
    {
        this.scanDesc = scanDesc;
    }

    public void setScanName(String scanName)
    {
        this.scanName = scanName;
    }
    
    public void setScheduled(boolean scheduled)
    {
        this.scheduled = scheduled;
    }

    public void setScheduled(Boolean scheduled)
    {
        setScheduled(scheduled.booleanValue());
    }

    public void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
