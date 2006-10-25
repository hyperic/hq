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

public class EventLog  
    extends PersistedObject
{
    private String  _detail;
    private String  _type;
    private Integer _entityType;
    private Integer _entityId;
    private long    _timestamp;
    private String  _subject;
    private String  _status;

    protected EventLog() {
    }

    public EventLog(String detail, String type, Integer entityType, 
                    Integer entityId, long timestamp, String subject, 
                    String status) 
    {
        _detail     = detail;
        _type       = type;
        _entityType = entityType;
        _entityId   = entityId;
        _timestamp  = timestamp;
        _subject    = subject;
        _status     = status;
    }
   
    public String getDetail() {
        return _detail;
    }
    
    public void setDetail(String detail) {
        _detail = detail;
    }
    
    public String getType() {
        return _type;
    }
    
    public void setType(String type) {
        _type = type;
    }
    
    public Integer getEntityType() {
        return _entityType;
    }
    
    public void setEntityType(Integer entityType) {
        _entityType = entityType;
    }
    
    public Integer getEntityId() {
        return _entityId;
    }
    
    public void setEntityId(Integer entityId) {
        _entityId = entityId;
    }
    
    public long getTimestamp() {
        return _timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        _timestamp = timestamp;
    }
    
    public String getSubject() {
        return _subject;
    }
    
    public void setSubject(String subject) {
        _subject = subject;
    }

    public String getStatus() {
        return _status;
    }
    
    public void setStatus(String status) {
        _status = status;
    }
}
