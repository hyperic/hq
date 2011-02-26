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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class QzTriggerId  implements Serializable {

    @Column(name="TRIGGER_NAME",nullable=false,length=200)
    private String triggerName;
    
    @Column(name="TRIGGER_GROUP",nullable=false,length=200)
    private String triggerGroup;

   
    public QzTriggerId() {
    }


    public String getTriggerName() {
        return triggerName;
    }
    
    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
    }

    public String getTriggerGroup() {
        return triggerGroup;
    }
    
    public void setTriggerGroup(String triggerGroup) {
        this.triggerGroup = triggerGroup;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((triggerGroup == null) ? 0 : triggerGroup.hashCode());
        result = prime * result + ((triggerName == null) ? 0 : triggerName.hashCode());
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
        QzTriggerId other = (QzTriggerId) obj;
        if (triggerGroup == null) {
            if (other.triggerGroup != null)
                return false;
        } else if (!triggerGroup.equals(other.triggerGroup))
            return false;
        if (triggerName == null) {
            if (other.triggerName != null)
                return false;
        } else if (!triggerName.equals(other.triggerName))
            return false;
        return true;
    }
    
    
}
