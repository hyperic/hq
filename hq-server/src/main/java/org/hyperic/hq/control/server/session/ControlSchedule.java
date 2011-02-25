/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.control.server.session;

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
import org.hyperic.hq.scheduler.ScheduleValue;

@Entity
@Table(name="EAM_CONTROL_SCHEDULE")
public class ControlSchedule implements Serializable
{
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;

    @Column(name="ENTITY_TYPE",nullable=false)
    @Index(name="CTL_SCHEDULE_ENTITY_IDX")
    private Integer entityType;
    
    @Column(name="ENTITY_ID",nullable=false)
    @Index(name="CTL_SCHEDULE_ENTITY_IDX")
    private Integer entityId;
    
    @Column(name="SUBJECT",nullable=false,length=32)
    private String subject;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="SCHEDULEVALUEBYTES",nullable=false)
    private byte[] scheduleValueBytes;
    
    @Column(name="NEXTFIRETIME",nullable=false)
    @Index(name="CTL_SCHEDULE_NEXTFIRETIME_IDX")
    private long nextFireTime;
    
    @Column(name="TRIGGERNAME",nullable=false,length=128,unique=true)
    private String triggerName;
    
    @Column(name="JOBNAME",nullable=false,length=128,unique=true)
    private String jobName;
    
    @Column(name="JOB_ORDER_DATA",length=500)
    private String jobOrderData;
    
    @Column(name="ACTION",nullable=false,length=32)
    private String action;


    /**
     * default constructor
     */
    public ControlSchedule()
    {
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

    // Property accessors
    public Integer getEntityType()
    {
        return this.entityType;
    }

    protected void setEntityType(Integer entityType)
    {
        this.entityType = entityType;
    }

    public Integer getEntityId()
    {
        return this.entityId;
    }

    protected void setEntityId(Integer entityId)
    {
        this.entityId = entityId;
    }

    public String getSubject()
    {
        return this.subject;
    }

    protected void setSubject(String subject)
    {
        this.subject = subject;
    }

    public byte[] getScheduleValueBytes()
    {
        return this.scheduleValueBytes;
    }

    protected void setScheduleValueBytes(byte[] scheduleValueBytes)
    {
        this.scheduleValueBytes = scheduleValueBytes;
    }

    public long getNextFireTime()
    {
        return this.nextFireTime;
    }

    protected void setNextFireTime(long nextFireTime)
    {
        this.nextFireTime = nextFireTime;
    }

    public String getTriggerName()
    {
        return this.triggerName;
    }

    protected void setTriggerName(String triggerName)
    {
        this.triggerName = triggerName;
    }

    public String getJobName()
    {
        return this.jobName;
    }

    protected void setJobName(String jobName)
    {
        this.jobName = jobName;
    }

    public String getJobOrderData()
    {
        return this.jobOrderData;
    }

    protected void setJobOrderData(String jobOrderData)
    {
        this.jobOrderData = jobOrderData;
    }

    public String getAction()
    {
        return this.action;
    }

    protected void setAction(String action)
    {
        this.action = action;
    }

    protected void setScheduleValue(ScheduleValue schedule) throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(os);
        o.writeObject(schedule);
        o.flush();
        os.close();

        setScheduleValueBytes(os.toByteArray());
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

    public boolean equals(Object obj)
    {
        return (obj instanceof ControlSchedule) && super.equals(obj);
    }
    
    public int hashCode() {
        int result = 17;
        result = 37*result + (getId() != null ? getId().hashCode() : 0);
        return result;      
    }

}


