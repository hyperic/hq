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

package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.events.shared.ActionPK;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;

/** 
 * The action manager.
 *
 * @ejb:bean name="ActionManager"
 *      jndi-name="ejb/events/ActionManager"
 *      local-jndi-name="LocalActionManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:transaction type="REQUIRED"
 */

public class ActionManagerEJBImpl implements SessionBean {
    public ActionManagerEJBImpl() {}

    private ActionDAO getActHome() {
        return DAOFactory.getDAOFactory().getActionDAO();
    }

    private AlertDAO getAlertDAO() {
        return DAOFactory.getDAOFactory().getAlertDAO();
    }
    
    private AlertDefinitionDAO getAlertDefHome() {
        return DAOFactory.getDAOFactory().getAlertDefDAO();
    }

    /**
     * Get a collection of all actions
     *
     * @return a collection of {@link ActionValue}s
     *
     * @ejb:interface-method
     */
    public Collection getAllActions() {
        return actionsToActionValues(getActHome().findAll());
    }

    /**
     * Get all the actions for a given alert
     *
     * @return a collection of {@link ActionValue}s
     * @ejb:interface-method
     */
    public List getActionsForAlert(int alertId) {
        Alert a = getAlertDAO().findById(new Integer(alertId)); 
        Collection actions = getActHome().findByAlert(a);
        
        return actionsToActionValues(actions);
    }
    
    private List actionsToActionValues(Collection actions) {
        List res = new ArrayList(actions.size());

        for (Iterator i=actions.iterator(); i.hasNext();) {
            Action action = (Action)i.next();
            
            res.add(action.getActionValue());
        }

        return res;
    }

    /**
     * Create a new action
     *
     * @return an ActionValue
     *
     * @ejb:interface-method
     */
    public ActionValue createAction(AlertDefinitionValue def, ActionValue val) {
        AlertDefinition aDef;
        Action res, parent;
        
        parent = val.getParentId() == null ? null :
            getActHome().findById(val.getParentId());
            
        aDef = getAlertDefHome().findById(def.getId());
        res = aDef.createAction(val, parent);
        
        return res.getActionValue();
    }

    /**
     * Update an action
     *
     * @return an ActionValue
     *
     * @ejb:interface-method
     */
    public ActionValue updateAction(ActionValue val) { 
        ActionDAO aDao = getActHome();
        // First update the primary action
        Action action = aDao.findByPrimaryKey(new ActionPK(val.getId()));
            
        action.setActionValue(val);

        // Then find and update the child actions.

        /* It would be nice to have a more explicit method that
           does this kind of update.  XXX -- JMT */ 
        Collection children = action.getChildren();
            
        val.setParentId(val.getId());
        for (Iterator i = children.iterator(); i.hasNext(); ) {
            Action act = (Action) i.next();

            act.setActionValue(val);
        }
        
        return action.getActionValue();
    }

    public void ejbCreate() {}

    public void ejbRemove() {}

    public void ejbActivate() {}

    public void ejbPassivate() {}

    public void setSessionContext(SessionContext ctx) {}
}
