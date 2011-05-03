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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Parameter;
import org.hyperic.hq.control.shared.ControlConstants;

@Entity
@Table(name = "EAM_CONTROL_HISTORY")
public class ControlHistory implements Serializable {
    @Column(name = "ACTION", length = 32, nullable = false)
    private String action;

    @Column(name = "ARGS", length = 500)
    private String args;

    @Column(name = "BATCH_ID")
    private Integer batchId;

    @Column(name = "DATE_SCHEDULED", nullable = false)
    private long dateScheduled;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "ENDTIME", nullable = false)
    private long endTime;

    @Column(name = "GROUP_ID")
    private Integer groupId;

    @Id
    @GeneratedValue(generator = "combo")
    @GenericGenerator(name = "combo", parameters = { @Parameter(name = "sequence", value = "EAM_CONTROL_HISTORY_ID_SEQ") }, 
        strategy = "org.hyperic.hibernate.id.ComboGenerator")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "MESSAGE", length = 500)
    private String message;

    @Column(name = "RESOURCE_ID", nullable = false)
    private Integer resource;

    @Column(name = "SCHEDULED", nullable = false)
    private boolean scheduled;

    @Column(name = "STARTTIME", nullable = false)
    @Index(name = "CTL_HISTORY_STARTTIME_IDX")
    private long startTime;

    @Column(name = "STATUS", nullable = false, length = 64)
    private String status;

    @Column(name = "SUBJECT", nullable = false, length = 32)
    private String subject;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    /**
     * default constructor
     */
    public ControlHistory() {
    }

    public ControlHistory(Integer resource, Integer groupId, Integer batchId, String subject,
                          String action, String args, Boolean scheduled, long startTime,
                          long endTime, long dateScheduled, String status, String description,
                          String message) {
        setGroupId(groupId);
        setBatchId(batchId);
        setResource(resource);
        setSubject(subject);
        setScheduled(scheduled);
        setStartTime(startTime);
        setEndTime(endTime);
        setDateScheduled(dateScheduled);
        setStatus(status);
        setDescription(description);
        setAction(action);
        setArgs(args);
        setMessage(message);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ControlHistory other = (ControlHistory) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    public String getAction() {
        return this.action;
    }

    public String getArgs() {
        return this.args;
    }

    public Integer getBatchId() {
        return this.batchId;
    }

    public long getDateScheduled() {
        return this.dateScheduled;
    }

    public String getDescription() {
        return this.description;
    }

    public long getDuration() {
        if (getStatus().equals(ControlConstants.STATUS_INPROGRESS)) {
            return System.currentTimeMillis() - getStartTime();
        } else {
            return getEndTime() - getStartTime();
        }
    }

    public long getEndTime() {
        return this.endTime;
    }

    public Integer getGroupId() {
        return this.groupId;
    }

    public Integer getId() {
        return id;
    }

    public String getMessage() {
        return this.message;
    }

    public Integer getResource() {
        return resource;
    }

    public Boolean getScheduled() {
        return new Boolean(isScheduled());
    }

    public long getStartTime() {
        return this.startTime;
    }

    public String getStatus() {
        return this.status;
    }

    public String getSubject() {
        return this.subject;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    public boolean isScheduled() {
        return this.scheduled;
    }

    protected void setAction(String action) {
        this.action = action;
    }

    protected void setArgs(String args) {
        this.args = args;
    }

    protected void setBatchId(Integer batchId) {
        this.batchId = batchId;
    }

    protected void setDateScheduled(long dateScheduled) {
        this.dateScheduled = dateScheduled;
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    protected void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    protected void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setMessage(String message) {
        this.message = message;
    }

    public void setResource(Integer resource) {
        this.resource = resource;
    }

    protected void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }

    protected void setScheduled(Boolean scheduled) {
        setScheduled(scheduled.booleanValue());
    }

    protected void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    protected void setStatus(String status) {
        this.status = status;
    }

    protected void setSubject(String subject) {
        this.subject = subject;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
