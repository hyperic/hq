package org.hyperic.hq.events;

import java.io.Serializable;

/**
 * This class is currently not really used.  It has been auto-generated, and
 * is only a placeholder until a full conversion of Hibernate is finished.
 * 
 * We really only use the hbm file to initialze the database.
 */
public class TriggerEvent  
    implements Serializable 
{
    private TriggerEventId _id;
    private long           _expiration;

    public TriggerEvent() {
    }

    public TriggerEvent(TriggerEventId id) {
        _id = id;
    }

    public TriggerEvent(TriggerEventId id, long expiration) {
        _id         = id;
        _expiration = expiration;
    }
   
    public TriggerEventId getId() {
        return _id;
    }
    
    public void setId(TriggerEventId id) {
        _id = id;
    }

    public long getExpiration() {
        return _expiration;
    }
    
    public void setExpiration(long expiration) {
        _expiration = expiration;
    }
}
