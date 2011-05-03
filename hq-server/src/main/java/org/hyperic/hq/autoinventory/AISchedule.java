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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import org.hyperic.hq.autoinventory.shared.AIScheduleValue;
import org.hyperic.hq.scheduler.ScheduleValue;

@Entity
@Table(name="EAM_AUTOINV_SCHEDULE")
public class AISchedule implements Serializable
{
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="CONFIG")
    private byte[] config;

    @Column(name="ENTITY_ID",nullable=false)
    @Index(name="AI_SCHEDULE_ENTITY_IDX")
    private Integer entityId;
    
    @Column(name="ENTITY_TYPE",nullable=false)
    @Index(name="AI_SCHEDULE_ENTITY_IDX")
    private Integer entityType;
    
    @Id
    @GeneratedValue(generator = "combo")
    @GenericGenerator(name = "combo", parameters = { @Parameter(name = "sequence", value = "EAM_AUTOINV_SCHEDULE_ID_SEQ") }, 
        strategy = "org.hyperic.hibernate.id.ComboGenerator")
    @Column(name = "ID")
    private Integer id;
    
    @Column(name="JOBNAME",nullable=false,length=128,unique=true)
    private String jobName;
    
    @Column(name="JOB_ORDER_DATA",length=500)
    private String jobOrderData;
    
    @Column(name="NEXTFIRETIME",nullable=false)
    @Index(name="AI_SCHEDULE_NEXTFIRETIME_IDX")
    private long nextFireTime;
    
    @Column(name="SCANDESC",length=200)
    private String scanDesc;
    
    @Column(name="SCANNAME",length=100,unique=true)
    private String scanName;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="SCHEDULEVALUEBYTES")
    private byte[] scheduleValueBytes;
    
    @Column(name="SUBJECT",nullable=false,length=32)
    private String subject;
    
    @Column(name="TRIGGERNAME",nullable=false,length=128,unique=true)
    private String triggerName;
    
    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;

    /**
     * default constructor
     */
    public AISchedule()
    {
        super();
    }
    
   
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof AISchedule)) {
            return false;
        }
        Integer objId = ((AISchedule)obj).getId();
  
        return getId() == objId ||
        (getId() != null && 
         objId != null && 
         getId().equals(objId));     
    }



    /**
     * legacy DTO pattern
     * @deprecated use (this) AISchedule object
     * @return
     */
    public AIScheduleValue getAIScheduleValue()
    {   AIScheduleValue aIScheduleValue = new AIScheduleValue();
        try {   
            aIScheduleValue.setId(getId());
            aIScheduleValue.setEntityType(getEntityType());
            aIScheduleValue.setEntityId(getEntityId());
            aIScheduleValue.setSubject(
                (getSubject() == null) ? "" : getSubject());
            aIScheduleValue.setScheduleValue(getScheduleValue());
            aIScheduleValue.setScheduleValueBytes(getScheduleValueBytes());
            aIScheduleValue.setTriggerName(
                (getTriggerName() == null) ? "" : getTriggerName());
            aIScheduleValue.setJobName(
                (getJobName() == null) ? "" : getJobName());
            aIScheduleValue.setNextFireTime(getNextFireTime());
            aIScheduleValue.setJobOrderData(
                (getJobOrderData() == null) ? "" : getJobOrderData());
            aIScheduleValue.setScanName(
                (getScanName() == null) ? "" : getScanName());
            aIScheduleValue.setScanDesc(
                (getScanDesc() == null) ? "" : getScanDesc());
            
            if (getConfig() != null) {
                aIScheduleValue.setConfig(getConfig());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return aIScheduleValue;
    }



    public byte[] getConfig()
    {
        return this.config;
    }



    public Integer getEntityId()
    {
        return this.entityId;
    }



    public Integer getEntityType()
    {
        return this.entityType;
    }

    public Integer getId() {
        return id;
    }

    public String getJobName()
    {
        return this.jobName;
    }

    public String getJobOrderData()
    {
        return this.jobOrderData;
    }

    public long getNextFireTime()
    {
        return this.nextFireTime;
    }

    public String getScanDesc()
    {
        return this.scanDesc;
    }

    public String getScanName()
    {
        return this.scanName;
    }

    public ScheduleValue getScheduleValue()
    throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream is =
            new ByteArrayInputStream(getScheduleValueBytes());
        ObjectInputStream o = new ObjectInputStream(is);

        ScheduleValue schedule = (ScheduleValue)o.readObject();

        return schedule;
    }

    public byte[] getScheduleValueBytes()
    {
        return this.scheduleValueBytes;
    }

    public String getSubject()
    {
        return this.subject;
    }

    public String getTriggerName()
    {
        return this.triggerName;
    }

    public Long getVersion() {
        return version;
    }

    public int hashCode() {
        int result = 17;
        result = 37*result + (getId() != null ? getId().hashCode() : 0);
        return result;      
    }

    public void setAIScheduleValue(AIScheduleValue valueHolder)
    {
        try {
            setEntityType( valueHolder.getEntityType() );
            setEntityId( valueHolder.getEntityId() );
            setSubject( valueHolder.getSubject() );
            setScheduleValue( valueHolder.getScheduleValue() );
            setScheduleValueBytes( valueHolder.getScheduleValueBytes() );
            setTriggerName( valueHolder.getTriggerName() );
            setJobName( valueHolder.getJobName() );
            setNextFireTime( valueHolder.getNextFireTime() );
            setJobOrderData( valueHolder.getJobOrderData() );
            setScanName( valueHolder.getScanName() );
            setScanDesc( valueHolder.getScanDesc() );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setConfig(byte[] config)
    {
        this.config = config;
    }

    public void setEntityId(Integer entityId)
    {
        this.entityId = entityId;
    }

    public void setEntityType(Integer entityType)
    {
        this.entityType = entityType;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setJobName(String jobName)
    {
        this.jobName = jobName;
    }

    public void setJobOrderData(String jobOrderData)
    {
        this.jobOrderData = jobOrderData;
    }

    public void setNextFireTime(long nextFireTime)
    {
        this.nextFireTime = nextFireTime;
    }

    public void setScanDesc(String scanDesc)
    {
        this.scanDesc = scanDesc;
    }

    public void setScanName(String scanName)
    {
        this.scanName = scanName;
    }

    public void setScheduleValue(ScheduleValue schedule) throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(os);
        o.writeObject(schedule);
        o.flush();
        os.close();

        setScheduleValueBytes(os.toByteArray());
    }

    public void setScheduleValueBytes(byte[] scheduleValueBytes)
    {
        this.scheduleValueBytes = scheduleValueBytes;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }
    
    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}