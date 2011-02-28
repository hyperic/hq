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

package org.hyperic.hq.scheduler;

import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="QRTZ_TRIGGER_LISTENERS")
public class QzTriggerListener  implements java.io.Serializable {

    @EmbeddedId
    private QzTriggerListenerId id;
    
    @ManyToOne(cascade=CascadeType.ALL,fetch=FetchType.LAZY)
    @JoinColumns({@JoinColumn(name="TRIGGER_NAME",referencedColumnName="TRIGGER_NAME",insertable=false,updatable=false),
       @JoinColumn(name="TRIGGER_GROUP",referencedColumnName="TRIGGER_GROUP",insertable=false,updatable=false)})
    private QzTrigger trigger;

   
    public QzTriggerListener() {
    }

    public QzTriggerListenerId getId() {
        return id;
    }
    
    public void setId(QzTriggerListenerId id) {
        this.id = id;
    }

    public QzTrigger getTrigger() {
        return trigger;
    }
    
    public void setTrigger(QzTrigger trigger) {
        this.trigger = trigger;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        QzTriggerListener other = (QzTriggerListener) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
    
    
}
