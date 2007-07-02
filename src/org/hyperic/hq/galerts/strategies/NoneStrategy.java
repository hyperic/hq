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

package org.hyperic.hq.galerts.strategies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.galerts.processor.FireReason;
import org.hyperic.hq.galerts.processor.Gtrigger;
import org.hyperic.hq.galerts.server.session.ExecutionReason;
import org.hyperic.hq.galerts.server.session.ExecutionStrategy;
import org.hyperic.hq.galerts.server.session.GalertDefPartition;

/**
 * This strategy will fire only when all of the triggers are 'not fired'.
 */
public class NoneStrategy 
    implements ExecutionStrategy
{
    private static final Log _log = LogFactory.getLog(NoneStrategy.class);

    private GalertDefPartition _partition;
    private String             _defName;
    private FireReason         _lastReason;
    private List               _triggers;
    private Map                _firedMap;  // Triggers -> Boolean
    private int                _numFired;
    
    public void configure(GalertDefPartition partition, String defName, 
                          List triggers) 
    {
        _partition = partition;
        _defName   = defName;
        _triggers  = new ArrayList(triggers);
        _numFired  = _triggers.size();
        _firedMap  = new HashMap(_numFired);

        reset();
        _log.debug("Configure called: partition=" + partition + 
                   " defName=" + defName + " triggers=" + triggers);
    }
    
    public GalertDefPartition getPartition() {
        return _partition;
    }

    public void setDefinitionName(String name) {
        _defName = name;
    }
    
    public void reset() {
        // We default the firedMap to say all triggers are currently firing
        for (Iterator i=_triggers.iterator(); i.hasNext(); ) {
            Gtrigger t = (Gtrigger)i.next();
            
            _firedMap.put(t, Boolean.TRUE);
        }
        _numFired = _triggers.size();
        _log.info("Resetting None strategy.  NumFired=" + _numFired);
    }

    public ExecutionReason shouldFire() {
        if (_numFired == 0) {
            return new ExecutionReason("The condition is no longer met",
                                       "None of the triggers are fired",
                                       Collections.EMPTY_LIST, _partition);
        } 
        return null;
    }

    public void triggerFired(Gtrigger trigger, FireReason reason) {
        Boolean oldFired = (Boolean)_firedMap.get(trigger);

        if (oldFired.equals(Boolean.FALSE)) {
            _firedMap.put(trigger, Boolean.TRUE);
            _numFired++;
        }
        
        _log.info(trigger + " fired.  Numfired= " + _numFired);
    }

    public void triggerNotFired(Gtrigger trigger) {
        Boolean oldFired = (Boolean)_firedMap.get(trigger);
        
        if (oldFired.equals(Boolean.TRUE)) {
            _firedMap.put(trigger, Boolean.FALSE);
            _numFired--;
        }
        _log.info(trigger + " unfired.  Numfired= " + _numFired);
    }
}
