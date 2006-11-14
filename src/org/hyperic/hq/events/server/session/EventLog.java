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

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.appdef.shared.AppdefEntityID;

import org.hyperic.hq.events.shared.EventLogValue;

public class EventLog  
    extends PersistedObject
{
    private String  _detail;
    private String  _type;
    private int     _entityType;
    private int     _entityId;
    private long    _timestamp;
    private String  _subject;
    private String  _status;

    private EventLogValue _valueObj;

    protected EventLog() {
    }

    EventLog(EventLogValue eVal) {
        _detail     = eVal.getDetail();
        _type       = eVal.getType();
        _entityType = eVal.getEntityType();
        _entityId   = eVal.getEntityId();
        _timestamp  = eVal.getTimestamp();
        _subject    = eVal.getSubject();
        _status     = eVal.getStatus();
    }
   
    public String getDetail() {
        return _detail;
    }
    
    protected void setDetail(String detail) {
        _detail = detail;
    }
    
    public String getType() {
        return _type;
    }
    
    protected void setType(String type) {
        _type = type;
    }
    
    public int getEntityType() {
        return _entityType;
    }
    
    protected void setEntityType(int entityType) {
        _entityType = entityType;
    }
    
    public int getEntityId() {
        return _entityId;
    }
    
    protected void setEntityId(int entityId) {
        _entityId = entityId;
    }
    
    public long getTimestamp() {
        return _timestamp;
    }
    
    protected void setTimestamp(long timestamp) {
        _timestamp = timestamp;
    }
    
    public String getSubject() {
        return _subject;
    }
    
    protected void setSubject(String subject) {
        _subject = subject;
    }

    public String getStatus() {
        return _status;
    }
    
    protected void setStatus(String status) {
        _status = status;
    }

    public EventLogValue getEventLogValue() {
        if (_valueObj == null)
            _valueObj = new EventLogValue();

        _valueObj.setId(getId());
        _valueObj.setSubject((getSubject() == null) ? "" : getSubject());
        _valueObj.setEntityType(getEntityType());
        _valueObj.setEntityId(getEntityId());
        _valueObj.setDetail((getDetail() == null) ? "" : getDetail());
        _valueObj.setTimestamp(getTimestamp());
        _valueObj.setType((getType() == null) ? "" : getType());
        _valueObj.setStatus((getStatus() == null) ? "" : getStatus());
        return _valueObj;
    }

    public void setEventLogValue(EventLogValue v) {
        setSubject(v.getSubject());
        setEntityType(v.getEntityType());
        setEntityId(v.getEntityId());
        setDetail(v.getDetail());
        setTimestamp(v.getTimestamp());
        setType(v.getType());
        setStatus(v.getStatus());
    }
}
