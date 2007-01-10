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

package org.hyperic.hq.galerts.processor;

import java.util.Set;

import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.galerts.server.session.ExecutionStrategy;
import org.hyperic.hq.zevents.Zevent;


/**
 * The Gtrigger is a trigger which can participate in group-alerting activities.
 * 
 * The purpose of the trigger is to monitor events, and 'fire' when it deems
 * necessary.
 * 
 * The lifecycle of the trigger is as follows:
 * 
 *   - setGroup is called to inform the trigger of the environment it is in
 *   - getInterestedEvents is then called to tell the alerting system which
 *     events the trigger will be listenting to.
 */
public abstract class Gtrigger {
    private MemGalertDef      _alertDef;
    private ExecutionStrategy _strategy;
    
    public abstract void processEvent(Zevent event);

    public abstract Set getInterestedEvents();

    public abstract void setGroup(ResourceGroup g);
    
    void setAlertDef(MemGalertDef def) {
        _alertDef = def;
    }

    void setStrategy(ExecutionStrategy strat) {
        _strategy = strat;
    }
    
    ExecutionStrategy getStrategy() {
        return _strategy;
    }
    
    /**
     * Called by the subclass to fire based on the passed reason. 
     * 
     * @param reason Reason the trigger fired.  This is used to compose
     *               alert messages, etc.
     */
    protected void setFired(FireReason reason) {
        _alertDef.triggerFired(this, reason);
    }
    
    protected void setNotFired() {
        _alertDef.triggerNotFired(this);
    }
}
