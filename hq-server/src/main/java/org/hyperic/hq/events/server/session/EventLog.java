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

package org.hyperic.hq.events.server.session;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Index;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.util.data.IEventPoint;

@Immutable
@Entity
@Table(name = "EAM_EVENT_LOG")
public class EventLog implements IEventPoint, Serializable {

    @Column(name = "DETAIL", nullable = false, length = 4000)
    private String detail;

    // Not persisted
    private transient int eventId;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "INSTANCE_ID")
    private Integer instanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESOURCE_ID", nullable = false)
    @Index(name = "EVENT_LOG_RES_ID_IDX")
    private Resource resource;

    @Column(name = "STATUS", length = 100)
    private String status;

    @Column(name = "SUBJECT", length = 100)
    private String subject;

    @Column(name = "TIMESTAMP", nullable = false)
    @Index(name = "EVENT_LOG_IDX", columnNames = { "TIMESTAMP", "RESOURCE_ID" })
    private long timestamp;

    @Column(name = "TYPE", nullable = false, length = 100)
    private String type;

    protected EventLog() {
    }

    public EventLog(Resource r, String subject, String type, String detail, long timestamp,
                       String status, Integer instanceId) {
        resource = r;
        this.subject = subject;
        this.type = type;
        this.detail = detail;
        this.timestamp = timestamp;
        this.status = status;
        this.instanceId = instanceId;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof EventLog)) {
            return false;
        }
        Integer objId = ((EventLog) obj).getId();

        return getId() == objId || (getId() != null && objId != null && getId().equals(objId));
    }

    public String getDetail() {
        return detail;
    }

    public int getEventID() {
        return eventId;
    }

    public Integer getId() {
        return id;
    }

    public Integer getInstanceId() {
        return instanceId;
    }

    public Resource getResource() {
        return resource;
    }

    public String getStatus() {
        return status;
    }

    public String getSubject() {
        return subject;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + (getId() != null ? getId().hashCode() : 0);
        return result;
    }

    protected void setDetail(String detail) {
        this.detail = detail;
    }

    public void setEventID(int eventId) {
        this.eventId = eventId;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setInstanceId(Integer instanceId) {
        this.instanceId = instanceId;
    }

    protected void setResource(Resource r) {
        resource = r;
    }

    protected void setStatus(String status) {
        this.status = status;
    }

    protected void setSubject(String subject) {
        this.subject = subject;
    }

    protected void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    protected void setType(String type) {
        this.type = type;
    }
}
