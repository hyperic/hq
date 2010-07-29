package org.hyperic.hq.galerts.processor;

import java.util.List;

import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.zevents.Zevent;

public interface GalertProcessor {

    void startupInitialize();
    
    void alertDefUpdated(GalertDef def, final String newName);
    
    void loadReloadOrUnload(GalertDef def);
    
    boolean validateAlertDef(GalertDef def);
    
    void alertDefDeleted(final Integer defId);
    
    void processEvents(final List<Zevent> events);
}
