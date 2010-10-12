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

package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.context.Bootstrap;

import java.util.Collection

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.escalation.server.session.Escalation
import org.hyperic.hq.events.ActionInterface

class EscalationHelper extends BaseHelper {
    private escMan = Bootstrap.getBean(EscalationManager.class)

    EscalationHelper(AuthzSubject user) {
        super(user)
    }

    /**
     * Find Escalation by ID or name
     *
     * @param id the ID of the escalation (optional if searching by name)
     * @param name the name of the escalation (optional if searching by ID)
     * @return Escalation
     */
    Escalation getEscalation(Integer id, String name) {
        if (id) {
            return escMan.findById(user, id)
        }
        else {
            return escMan.findByName(user, name)
        }
    }

    /**
     * Return a list of all Escalations
     * @return a list of escalations
     */
    Collection getAllEscalations() {
        escMan.findAll(user)
    }

    /**
     * Delete an Escalation by ID
     * @param id the ID of the escalation (optional if searching by name)
     */
    def deleteEscalation(int id) {
        def esc = escMan.findById(id)
        if (esc) {
            escMan.deleteEscalation(user, esc)
        }
    }

    /**
     * Create an Escalation
     */
     Escalation createEscalation(String name, String description,
                                 boolean pauseAllowed, long maxWaitTime,
                                 boolean notifyAll, boolean repeat) {
        escMan.createEscalation(name, description, pauseAllowed, maxWaitTime,
                                notifyAll, repeat)
    }

    /**
     * Update an Escalation
     */
     def updateEscalation(Escalation esc, String name,
                          String description, boolean pauseAllowed,
                          long maxWaitTime, boolean notifyAll,
                          boolean repeat) {
        escMan.updateEscalation(user, esc, name, description, pauseAllowed,
                                maxWaitTime, notifyAll, repeat)
    }
    
    /**
     * Delete an action for a specific escalation
     */
     def deleteAction(Escalation esc, Integer actionId) {
         escMan.removeAction(esc, actionId)
     }
    
    /**
     * Add an action to a specific escalation
     */
     def addAction(Escalation esc, ActionInterface action, long wait) {
        escMan.addAction(esc, action, wait)
    }
}
