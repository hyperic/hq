package org.hyperic.hq.galerts.processor;

import org.hyperic.hq.galerts.server.session.GalertDef;

public interface GalertProcessor {

    void startupInitialize();
    
    void alertDefUpdated(GalertDef def, final String newName);
    
    void loadReloadOrUnload(GalertDef def);
    
    boolean validateAlertDef(GalertDef def);
    
    void alertDefDeleted(final Integer defId);
}
