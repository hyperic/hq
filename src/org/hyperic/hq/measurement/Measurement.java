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

package org.hyperic.hq.measurement;

import org.hyperic.hibernate.PersistedObject;

public class Measurement extends PersistedObject 
    implements java.io.Serializable {

    // Fields    
    private Integer _instanceId;
    private MeasurementTemplate _template;
    private Integer _cid;
    private long _mtime;
    
    // Constructors
    public Measurement() {
    }

    public Measurement(Integer instanceId, MeasurementTemplate template) {
        _instanceId = instanceId;
        _template = template;
    }
   
    // Property accessors
    public Integer getInstanceId() {
        return _instanceId;
    }
    
    public void setInstanceId(Integer instanceId) {
        _instanceId = instanceId;
    }

    public MeasurementTemplate getTemplate() {
        return _template;
    }
    
    public void setTemplate(MeasurementTemplate template) {
        _template = template;
    }

    public Integer getCid() {
        return _cid;
    }
    
    public void setCid(Integer cid) {
        _cid = cid;
    }

    public long getMtime() {
        return _mtime;
    }
    
    public void setMtime(long mtime) {
        _mtime = mtime;
    }
}
