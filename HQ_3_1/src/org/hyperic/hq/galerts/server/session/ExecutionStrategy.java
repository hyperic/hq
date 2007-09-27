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

import java.util.List;

import org.hyperic.hq.galerts.processor.FireReason;
import org.hyperic.hq.galerts.processor.Gtrigger;


public interface ExecutionStrategy {
    /**
     * Determine if the execution strategy should fire.  
     * 
     * @return null if the strategy should not fire
     */
    ExecutionReason shouldFire();

    /**
     * Reset the internal state of the execution strategy.  This should not  
     * affect the data setup by {@link #configure(String, List)}, only
     * the internal state (such as time of last fired trigger, etc.)
     * This method will be invoked after the alert definition has fired and
     * needs to be reset.
     */
    void reset();
    
    /**
     * Inform the strategy about the state in which it is running.  This 
     * method will be invoked when the the framework detects a change in
     * configuration of any of the triggers.
     */
    void configure(GalertDefPartition partition, String defName, List triggers);
    
    /**
     * Called when the name of the definition to which the strategy is bound
     * has been changed
     */
    void setDefinitionName(String defName);
    
    /**
     * Called when a trigger has fired.   
     */
    void triggerFired(Gtrigger trigger, FireReason reason);
    
    /**
     * Called when a condition is no longer firing (i.e. its condition is no
     * longer met)
     */
    void triggerNotFired(Gtrigger trigger);
    
    GalertDefPartition getPartition();
}
