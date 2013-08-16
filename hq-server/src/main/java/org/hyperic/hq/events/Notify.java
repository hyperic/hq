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

import java.util.Set;

import javax.mail.internet.InternetAddress;

import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.EscalationStateChange;
import org.hyperic.hq.events.server.session.Action;

/**
 * {@link Action}s implementing this interface and existing within an 
 * escalation chain are called when the state of an escalation changes.
 * 
 * DevNote:  This should probably be in the escalation package..
 */
public interface Notify {
    /**
     * Send a notification about the change of state in an escalation.
     * 
     * @param e       Escalatable which originally kicked off an escalation
     * @param change  New state of the escalation
     * @param message Message about the state change
     */
    public void send(Escalatable e, EscalationStateChange change, String message, Set<InternetAddress> notified)
        throws ActionExecuteException;
}
