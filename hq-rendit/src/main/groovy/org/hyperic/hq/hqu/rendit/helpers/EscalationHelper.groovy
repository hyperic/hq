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
