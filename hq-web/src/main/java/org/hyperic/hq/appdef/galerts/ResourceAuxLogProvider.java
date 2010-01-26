package org.hyperic.hq.appdef.galerts;

import java.util.ResourceBundle;

import org.hyperic.hq.appdef.server.session.ResourceAuxLogPojo;
import org.hyperic.hq.appdef.shared.ResourceAuxLogManager;
import org.hyperic.hq.events.AlertAuxLog;
import org.hyperic.hq.events.AlertAuxLogProvider;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.shared.GalertManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ResourceAuxLogProvider extends AlertAuxLogProvider {
    private static final String BUNDLE = "org.hyperic.hq.appdef.Resources";
    private GalertManager galertManager;
    private ResourceAuxLogManager resourceAuxLogManager;

    public static /*final*/ResourceAuxLogProvider INSTANCE;
    
    @Autowired
    public ResourceAuxLogProvider(GalertManager galertManager, ResourceAuxLogManager resourceAuxLogManager) {
        super(0xf00ff00f, "Auxillary Resource Data", "auxlog.appdef", ResourceBundle.getBundle(BUNDLE));
        this.galertManager = galertManager;
        this.resourceAuxLogManager = resourceAuxLogManager;
        INSTANCE = this;
    }
    
    private GalertAuxLog findGAuxLog(int id) {
        return galertManager.findAuxLogById(new Integer(id));
    }

    public AlertAuxLog load(int auxLogId, long timestamp, String desc) {
        GalertAuxLog gAuxLog = findGAuxLog(auxLogId);
        ResourceAuxLogPojo auxLog = resourceAuxLogManager.find(gAuxLog);
        
        return new ResourceAuxLog(gAuxLog, auxLog);
    }

    public void save(int auxLogId, AlertAuxLog log) {
        ResourceAuxLog logInfo = (ResourceAuxLog)log;
        GalertAuxLog gAuxLog = findGAuxLog(auxLogId);
        
        resourceAuxLogManager.create(gAuxLog, logInfo);
    }

    public void deleteAll(GalertDef def) {
        resourceAuxLogManager.removeAll(def);
    }
}
