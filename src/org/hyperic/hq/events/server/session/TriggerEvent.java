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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.hyperic.hibernate.LongIdPersistedObject;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.util.ArrayUtil;

public class TriggerEvent extends LongIdPersistedObject {
    private byte[]     _eventObject;
    private Integer    _triggerId;
    private long       _ctime;
    private long       _expiration;

    /**
     * Creates an instance where the unique id is currently unknown but will 
     * be assigned by the underlying persistence engine.
     *
     * @param eventObject The event.
     * @param triggerId The trigger id.
     * @param ctime The creation time in milliseconds.
     * @param expiration The expiration time in milliseconds.
     */
    protected TriggerEvent(AbstractEvent eventObject, 
                           Integer triggerId, 
                           long ctime, 
                           long expiration) {

        setEventObject(eventObject);
        setTriggerId(triggerId);
        setCtime(ctime);
        setExpiration(expiration);
    }
    
    /**
     * Creates an instance where the unique id is already known.
     *
     * @param id The unique id.
     * @param eventObject The event.
     * @param triggerId The trigger id.
     * @param ctime The creation time in milliseconds.
     * @param expiration The expiration time in milliseconds.
     */
    protected TriggerEvent(Long id, 
                           AbstractEvent eventObject, 
                           Integer triggerId, 
                           long ctime, 
                           long expiration) {
        this(eventObject, triggerId, ctime, expiration);
        setId(id);
    }
    
    protected TriggerEvent() {
    }
    
    public byte[] getEventObject() {
        return ArrayUtil.clone(_eventObject);
    }
    
    protected void setEventObject(AbstractEvent eventObject) {
        // Event objects are typically around 1k in size
        ByteArrayOutputStream baOs = new ByteArrayOutputStream(1024);
        
        try {
            ObjectOutputStream objectOs = new ObjectOutputStream(baOs);
            objectOs.writeObject(eventObject);
            objectOs.flush();    
            setEventObject(baOs.toByteArray());
        } catch (IOException e) {
            assert false : "Shouldn't have IOException since we are " +
            		       "writing to a byte array stream.";
        }        
    }
    
    protected void setEventObject(byte[] eventObject) {
        _eventObject = eventObject;
    }
    
    public Integer getTriggerId() {
        return _triggerId;
    }
    
    protected void setTriggerId(Integer triggerId) {
        _triggerId = triggerId;
    }
    
    public long getCtime() {
        return _ctime;
    }
    
    protected void setCtime(long ctime) {
        _ctime = ctime;
    }
    
    public long getExpiration() {
        return _expiration;
    }
    
    protected void setExpiration(long expiration) {
        _expiration = expiration;
    }    

}
