package org.hyperic.hq.galerts.server.session;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.common.SystemException;

public class GtriggerTypeInfo 
    extends PersistedObject
{
    private Class _typeClass;
    
    protected GtriggerTypeInfo() {}
    
    GtriggerTypeInfo(Class typeClass) {
        _typeClass = typeClass;
    }
    
    /**
     * Get the trigger type class which extends {@link GtriggerType}
     */
    protected Class getTypeClass() {
        return _typeClass;
    }
    
    protected void setTypeClass(Class typeClass) {
        _typeClass = typeClass;
    }
    
    public GtriggerType getType() {
        try {
            return (GtriggerType)_typeClass.newInstance();
        } catch(InstantiationException e) {
            throw new SystemException(e);
        } catch(IllegalAccessException e) {
            throw new SystemException(e);
        }
    }
}
