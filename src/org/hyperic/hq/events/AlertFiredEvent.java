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

package org.hyperic.hq.events;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.server.session.AlertDefinition;

/**
 * Event sent when alerts have fired
 */
public class AlertFiredEvent extends AbstractEvent
    implements java.io.Serializable, ResourceEventInterface, LoggableInterface {
    
    private static final long serialVersionUID = -3740509119080501003L;

    /** Holds value of alert ID. */
    private Integer alertId;
    
    /** Holds value of property message. */
    private String message;
    
    private AppdefEntityID resource;
    
    private String alertDefName;

    /** Creates a new instance of AlertFiredEvent */
    public AlertFiredEvent(TriggerFiredEvent e, Integer alertId,
                           AlertDefinition alertDef) {
        setMessage(e.getMessage());
        setInstanceId(alertDef.getId());
        setAlertId(alertId);
        setResource(new AppdefEntityID(alertDef.getAppdefType(),
                                       alertDef.getAppdefId()));
        setAlertDefName(alertDef.getName());
    }
    
    /** Getter for property message.
     * @return Value of property message.
     *
     */
    public String getMessage() {
        return this.message;
    }
    
    /** Setter for property message.
     * @param message New value of property message.
     *
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * @return Returns the alertId.
     */
    public Integer getAlertId() {
        return alertId;
    }
    
    /**
     * @param alertId The alertId to set.
     */
    public void setAlertId(Integer alertId) {
        this.alertId = alertId;
    }

    public AppdefEntityID getResource() {
        return resource;
    }
    
    public void setResource(AppdefEntityID resource) {
        this.resource = resource;
    }
    
    public String getAlertDefName() {
        return alertDefName;
    }
    
    public void setAlertDefName(String alertDefName) {
        this.alertDefName = alertDefName;
    }
    
    public String toString() {
        if (this.message != null) {
            return this.message;
        }
        else {
            return super.toString();
        }
    }

    public String getLevelString() {
        return "ALR";
    }

    public String getSubject() {
        return getAlertDefName();
    }
}
