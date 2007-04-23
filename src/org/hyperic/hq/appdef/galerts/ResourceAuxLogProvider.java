package org.hyperic.hq.appdef.galerts;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.ResourceAuxLogManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ResourceAuxLogPojo;
import org.hyperic.hq.events.AlertAuxLog;
import org.hyperic.hq.events.AlertAuxLogProvider;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertManagerEJBImpl;

public class ResourceAuxLogProvider 
    extends AlertAuxLogProvider
{
    private static final Log _log = 
        LogFactory.getLog(ResourceAuxLogProvider.class);

    public static final ResourceAuxLogProvider INSTANCE =  
        new ResourceAuxLogProvider(0xf00ff00f, "Auxillary Resource Data");

    private ResourceAuxLogProvider(int code, String desc) {
        super(code, desc);
    }
    
    private GalertAuxLog findGAuxLog(int id) {
        return GalertManagerEJBImpl.getOne().findAuxLogById(new Integer(id));
    }

    public AlertAuxLog load(int auxLogId, long timestamp, String desc) {
        GalertAuxLog gAuxLog = findGAuxLog(auxLogId);
        ResourceAuxLogPojo auxLog = 
            ResourceAuxLogManagerEJBImpl.getOne().find(gAuxLog);
        
        return new ResourceAuxLog(gAuxLog, auxLog);
    }

    public void save(int auxLogId, AlertAuxLog log) {
        ResourceAuxLog logInfo = (ResourceAuxLog)log;
        GalertAuxLog gAuxLog = findGAuxLog(auxLogId);
        
        ResourceAuxLogManagerEJBImpl.getOne().create(gAuxLog, logInfo);
    }

    public void delete(int auxLogId) {
        GalertAuxLog gAuxLog = findGAuxLog(auxLogId);
        
        ResourceAuxLogManagerEJBImpl.getOne().remove(gAuxLog);
    }
}
