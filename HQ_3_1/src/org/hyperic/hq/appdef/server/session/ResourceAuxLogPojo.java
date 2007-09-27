package org.hyperic.hq.appdef.server.session;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.appdef.galerts.ResourceAuxLog;
import org.hyperic.hq.appdef.galerts.ResourceAuxLogProvider;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.AlertAuxLogProvider;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;

public class ResourceAuxLogPojo
    extends PersistedObject
{
    private GalertAuxLog  _auxLog;
    private int           _appdefType;
    private int           _appdefId;
    private GalertDef     _def;
    
    protected ResourceAuxLogPojo() {
    }

    ResourceAuxLogPojo(GalertAuxLog log, ResourceAuxLog logInfo, GalertDef def) 
    { 
        _auxLog     = log;
        _appdefType = logInfo.getEntity().getType();
        _appdefId   = logInfo.getEntity().getID();
        _def        = def;
    }
   
    public GalertAuxLog getAuxLog() {
        return _auxLog;
    }
    
    protected void setAuxLog(GalertAuxLog log) {
        _auxLog = log;
    }
    
    protected int getAppdefType() {
        return _appdefType;
    }
    
    protected void setAppdefType(int appdefType) {
        _appdefType = appdefType;
    }
    
    protected int getAppdefId() {
        return _appdefId;
    }
    
    protected void setAppdefId(int appdefId) {
        _appdefId = appdefId;
    }
    
    public AppdefEntityID getEntityId() {
        return new AppdefEntityID(getAppdefType(), getAppdefId());
    }
    
    public GalertDef getAlertDef() {
        return _def;
    }
    
    protected void setAlertDef(GalertDef def) {
        _def = def;
    }
    
    public AlertAuxLogProvider getProvider() {
        return ResourceAuxLogProvider.INSTANCE;
    }

    public int hashCode() {
        int hash = 1;

        hash = hash * 31 + getAuxLog().hashCode();
        hash = hash * 31 + getEntityId().hashCode();
        return hash;
    }
    
    public boolean equals(Object o) {
        if (o == this)
            return true;
        
        if (o == null || o instanceof ResourceAuxLogPojo == false)
            return false;
        
        ResourceAuxLogPojo oe = (ResourceAuxLogPojo)o;

        return oe.getAuxLog().equals(getAuxLog()) &&
               oe.getEntityId().equals(getEntityId());
    }
}