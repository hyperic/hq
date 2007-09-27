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
package org.hyperic.hq.events;

import java.util.Collection;
import java.util.ResourceBundle;

import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.util.HypericEnum;


/**
 * Abstracts the location of the source of auxillary log information.
 */
public abstract class AlertAuxLogProvider 
    extends HypericEnum 
{
    protected AlertAuxLogProvider(int code, String desc, String propLocale, 
                                  ResourceBundle bundle) 
    {
        super(AlertAuxLogProvider.class, code, desc, propLocale, bundle);
    }
    
    public static AlertAuxLogProvider findByCode(int code) {
        return (AlertAuxLogProvider)findByCode(AlertAuxLogProvider.class, code); 
    }
    
    /**
     * Load a provider-specific AlertAuxLog 
     *
     * @param auxLogId ID of the auxillary log to which to load aux info for
     */
    public abstract AlertAuxLog load(int auxLogId, long timestamp,
                                     String description);
    
    /**
     * Save an auxillary log (log) to the DB.  This aux log is tied to a real
     * log, as specified by the auxLogId.  For the time being, this
     * auxLogId specifies a {@link GalertAuxLog}  
     */
    public abstract void save(int auxLogId, AlertAuxLog log);
    
    /**
     * Delete any auxillary logs associated with the specified auxLogId
     */
    public abstract void deleteAll(GalertDef def);
    
    public static Collection findAll() {
        return HypericEnum.getAll(AlertAuxLogProvider.class);
    }
}
