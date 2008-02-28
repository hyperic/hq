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

package org.hyperic.hq.galerts.server.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.galerts.ResourceAuxLogProvider;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.AlertAuxLog;
import org.hyperic.hq.events.AlertAuxLogProvider;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.measurement.galerts.MetricAuxLogProvider;

public class GalertEscalatable
    implements Escalatable
{
    static {
        AlertAuxLogProvider[] types = new AlertAuxLogProvider[] {
                GalertAuxLogProvider.INSTANCE,
                MetricAuxLogProvider.INSTANCE,
                ResourceAuxLogProvider.INSTANCE
        };
    }
    
    private static final Log _log = LogFactory.getLog(GalertEscalatable.class);
    private GalertLog _alert;
    private List      _auxLogs;
    
    GalertEscalatable(GalertLog alert) {
        _alert = alert;
    }
    
    public AlertInterface getAlertInfo() {
        return _alert;
    }

    /**
     * The aux logs stored in the DB come out in a strange formation. 
     * They look like this:
     * 
     * GalertLogs
     *   |
     *   \-> GalertAuxLog
     *           | (recursive)
     *           \-> GalertAuxLog
     *                  |
     *                  \-> Aux Log Specifics (defined by auxtype)
     * 
     * We need to pull data out of the DB and re-constitute the tree
     *
     * @return a list of {@link AlertAuxLog]s
     */
    private List composeAuxLogs() {
        List gAuxLogs = _alert.getAuxLogs();
        List res = new ArrayList();
        Map idToLog = new HashMap(); 
        
        // First load all the aux logs
        for (Iterator i=gAuxLogs.iterator(); i.hasNext(); ) {
            GalertAuxLog gAuxLog = (GalertAuxLog)i.next();

            AlertAuxLogProvider provider = 
                AlertAuxLogProvider.findByCode(gAuxLog.getAuxType());
            AlertAuxLog auxLog = provider.load(gAuxLog.getId().intValue(), 
                                               gAuxLog.getTimestamp(),
                                               gAuxLog.getDescription());
            idToLog.put(gAuxLog.getId(), auxLog);
            
            if (gAuxLog.getParent() == null)
                res.add(auxLog);
        }
        
        // Now process the hierarchy and make sure all the children are setup
        for (Iterator i=gAuxLogs.iterator(); i.hasNext(); ) {
            GalertAuxLog gAuxLog = (GalertAuxLog)i.next();
            
            if (gAuxLog.getParent() == null) 
                continue;
            
            AlertAuxLog child  = (AlertAuxLog)idToLog.get(gAuxLog.getId());
            AlertAuxLog parent = (AlertAuxLog)
                idToLog.get(gAuxLog.getParent().getId());
            parent.addChild(child);
        }
        return res;
    }        
    
    public List getAuxLogs() {
        if (_auxLogs == null)
            _auxLogs = composeAuxLogs();
        
        return _auxLogs;
    }

    public PerformsEscalations getDefinition() {
        return _alert.getDefinition();
    }

    public Integer getId() {
        return _alert.getId();
    }

    public String getLongReason() {
        return _alert.getLongReason();
    }

    public String getShortReason() {
        return _alert.getShortReason();
    }

    public boolean isAcknowledgeable() {
        return _alert.isAcknowledgeable();
    } 
}
