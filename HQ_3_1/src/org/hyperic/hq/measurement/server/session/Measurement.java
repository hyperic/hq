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

import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hibernate.PersistedObject;

public abstract class Measurement extends PersistedObject 
    implements ContainerManagedTimestampTrackable, Serializable 
{
    private Integer             _instanceId;
    private MeasurementTemplate _template;
    private Integer             _cid;
    private long                _mtime;
    
    protected Measurement() {
    }

    public Measurement(Integer instanceId, MeasurementTemplate template) {
        _instanceId = instanceId;
        _template   = template;
    }
    
    /**
     * @see org.hyperic.hibernate.ContainerManagedTimestampTrackable#allowContainerManagedLastModifiedTime()
     * @return <code>false</code> by default.
     */
    public boolean allowContainerManagedCreationTime() {
        return false;
    }
    
    /**
     * @see org.hyperic.hibernate.ContainerManagedTimestampTrackable#allowContainerManagedLastModifiedTime()
     * @return <code>true</code> by default.
     */
    public boolean allowContainerManagedLastModifiedTime() {
        return true;
    }
   
    public Integer getInstanceId() {
        return _instanceId;
    }
    
    protected void setInstanceId(Integer instanceId) {
        _instanceId = instanceId;
    }

    public MeasurementTemplate getTemplate() {
        return _template;
    }
    
    protected void setTemplate(MeasurementTemplate template) {
        _template = template;
    }

    public Integer getCid() {
        return _cid;
    }
    
    protected void setCid(Integer cid) {
        _cid = cid;
    }

    public long getMtime() {
        return _mtime;
    }
    
    protected void setMtime(long mtime) {
        _mtime = mtime;
    }
    
    public abstract boolean isDerived();

    public boolean equals(Object obj) {
        if (!(obj instanceof Measurement) || !super.equals(obj)) {
            return false;
        }
        Measurement o = (Measurement)obj;
        return ((_instanceId == o.getInstanceId() ||
                 (_instanceId!=null && o.getInstanceId()!=null &&
                  _instanceId.equals(o.getInstanceId()))))
               &&
               ((_template == o.getTemplate() ||
                 (_template!=null && o.getTemplate()!=null &&
                  _template.equals(o.getTemplate()))));
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37*result + (_instanceId != null ? _instanceId.hashCode(): 0);
        result = 37*result + (_template != null ? _template.hashCode(): 0);
        return result;
    }
}
