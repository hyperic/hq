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

import org.hyperic.hq.appdef.shared.AppdefEntityID;

public class SrnId implements Serializable {
    private int appdefType;
    private int instanceId;

    public SrnId() {
    }
    
    public SrnId(AppdefEntityID aeid) {
        this.appdefType = aeid.getType();
        this.instanceId = aeid.getID();
    }
    
    public SrnId(int appdefType, int instanceId) {
        this.appdefType = appdefType;
        this.instanceId = instanceId;
    }
   
    public int getAppdefType() {
        return appdefType;
    }
    
    protected void setAppdefType(int appdefType) {
        this.appdefType = appdefType;
    }

    public int getInstanceId() {
        return instanceId;
    }
    
    protected void setInstanceId(int  instanceId) {
        this.instanceId = instanceId;
    }

    public boolean equals(Object rhs) {
        if (this == rhs) {
            return true;
        }
        if (rhs == null || !(rhs instanceof ScheduleRevNum)) {
            return false;
        }
        final SrnId s = (SrnId) rhs;
        final AppdefEntityID aeid = new AppdefEntityID(appdefType, instanceId);
        final AppdefEntityID aeid2 = new AppdefEntityID(s.appdefType, s.instanceId);
        return aeid.equals(aeid2);
    }

    public int hashCode() {
        final AppdefEntityID aeid = new AppdefEntityID(appdefType, instanceId);
        final int result = aeid.hashCode();
        return result;

    }
    
    public String toString() {
        return getAppdefType() + ":" + getInstanceId();
    }
}
