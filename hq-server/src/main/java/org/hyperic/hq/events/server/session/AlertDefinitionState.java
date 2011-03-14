/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2007], Hyperic, Inc. 
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
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name="EAM_ALERT_DEF_STATE")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class AlertDefinitionState implements Serializable {
    
    @Id
    private Integer id;
    
    @MapsId 
    @OneToOne
    @JoinColumn(name = "ALERT_DEFINITION_ID")
    private ResourceAlertDefinition alertDefinition;
    
    @Column(name="LAST_FIRED",nullable=false)
    private long lastFired;

    AlertDefinitionState() {}
    
    public AlertDefinitionState(ResourceAlertDefinition def) {
        alertDefinition = def;
        lastFired = 0;
    }
    
    
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ResourceAlertDefinition getAlertDefinition() {
        return alertDefinition;
    }
    
    void setAlertDefinition(ResourceAlertDefinition def) { 
        alertDefinition = def;
    }
    
    public long getLastFired() {
        return lastFired;
    }

    void setLastFired(long lastFired) {
        this.lastFired = lastFired;
    }
    
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof AlertDefinitionState)) {
            return false;
        }
        Integer objId = ((AlertDefinitionState)obj).getId();
  
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
