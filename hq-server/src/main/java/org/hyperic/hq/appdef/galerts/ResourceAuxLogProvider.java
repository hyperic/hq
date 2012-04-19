/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.appdef.galerts;

import java.util.ResourceBundle;

import javax.annotation.PreDestroy;

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
    private final GalertManager galertManager;
    private final ResourceAuxLogManager resourceAuxLogManager;

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

    @Override
    public AlertAuxLog load(int auxLogId, long timestamp, String desc) {
        GalertAuxLog gAuxLog = findGAuxLog(auxLogId);
        ResourceAuxLogPojo auxLog = resourceAuxLogManager.find(gAuxLog);
        
        return new ResourceAuxLog(gAuxLog, auxLog);
    }

    @Override
    public void save(int auxLogId, AlertAuxLog log) {
        ResourceAuxLog logInfo = (ResourceAuxLog)log;
        GalertAuxLog gAuxLog = findGAuxLog(auxLogId);
        
        resourceAuxLogManager.create(gAuxLog, logInfo);
    }

    @Override
    public void deleteAll(GalertDef def) {
        resourceAuxLogManager.removeAll(def);
    }
    
    @PreDestroy
    public final void destroy() {
        INSTANCE = null ; 
        this.unregister() ; 
    }//EOM 
}
