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

package org.hyperic.hq.measurement.server.session;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name="EAM_SRN")
public class ScheduleRevNum implements Serializable {

    @EmbeddedId
    private SrnId id;
    
    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;
    
    @Column(name="SRN",nullable=false)
    private int   srn;
    
    
    private transient long  minInterval = 0;
    private transient long  lastReported = 0;
    
    public ScheduleRevNum() {
    }

    public ScheduleRevNum(SrnId id, int srn) {
        this.id  = id;
        this.srn = srn;
    }

    public SrnId getId() {
        return id;
    }
    
    protected void setId(SrnId id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }
    
    protected void setVersion(long version) {
        this.version = version;
    }

    public int getSrn() {
        return srn;
    }
    
    protected void setSrn(int srn) {
        this.srn = srn;
    }

    public long getMinInterval() {
        return minInterval;
    }
    
    public void setMinInterval(long minInterval) {
        this.minInterval = minInterval;
    }

    public long getLastReported() {
        return lastReported;
    }
    
    public void setLastReported(long lastReported) {
        this.lastReported = lastReported;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof ScheduleRevNum)) {
            return false;
        }
        ScheduleRevNum o = (ScheduleRevNum)obj;
        return id == o.getId() ||
               (id != null && o.getId() != null && id.equals(o.getId()));
    }

    public int hashCode() {
        int result = 17;

        result = 37*result + (id != null ? id.hashCode() : 0);

        return result;
    }
}
