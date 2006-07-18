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
import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.ActionCreateException;
import org.hyperic.hq.events.shared.ActionLocal;
import org.hyperic.hq.events.shared.ActionLocalHome;
import org.hyperic.hq.events.shared.ActionPK;
import org.hyperic.hq.events.shared.ActionUtil;
import org.hyperic.hq.events.shared.ActionValue;

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

    private ActionLocalHome actHome = null;
    private ActionLocalHome getActHome() {
        if (actHome == null) {
            try {
                actHome = ActionUtil.getLocalHome();
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return actHome;
    }
    
    /**
     * Get a collection of all actions
     *
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public Collection getAllActions() {
        ActionLocalHome actionHome;
        ArrayList actionValues;

        actionValues = new ArrayList();

        try {
            Collection actions = getActHome().findAll();
            actionValues.addAll( _actionsToActionValues(actions) );
        } catch (FinderException e) {
            // No actions found, just return an empty list, then
        }

        return actionValues;
    }

    /**
     * Get actions for a given alert id.
     *
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public List getActionsForAlert(Integer aid) throws FinderException {
        Collection actions = getActHome().findByAlertId(aid);
        return _actionsToActionValues(actions);
    }

    private List _actionsToActionValues(Collection actions) {
        ArrayList actionValues = new ArrayList( actions.size() );

        for (Iterator it=actions.iterator(); it.hasNext();) {
            ActionLocal action = (ActionLocal)it.next();
            actionValues.add( action.getActionValue() );
        }

        return actionValues;
    }

    /**
     * Create a new action
     *
     * @return an ActionValue
     *
     * @ejb:interface-method
     */
    public ActionValue createAction(ActionValue val)
        throws ActionCreateException {
        try {
            ActionLocal action = getActHome().create(val);
            return action.getActionValue();
        } catch (CreateException e) {
            throw new ActionCreateException(e);
        }
    }

    /**
     * Update an action
     *
     * @return an ActionValue
     *
     * @ejb:interface-method
     */
    public ActionValue updateAction(ActionValue val) throws FinderException {
        // First update the primary action
        ActionLocal action =
            getActHome().findByPrimaryKey(new ActionPK(val.getId()));
        action.setActionValue(val);

        // Then find and update the child actions
        try {
            Collection children = getActHome().findChildrenActions(val.getId());
            
            val.setParentId(val.getId());
            for (Iterator it = children.iterator(); it.hasNext(); ) {
                ActionLocal act = (ActionLocal) it.next();
                act.setActionValue(val);
            }
        } catch (FinderException e) {
            // Ignore, there are no children
        }
        
        return action.getActionValue();
    }

    /**
     * Delete an action.
     *
     * @ejb:interface-method
     */
    public void deleteAction(int actionID)
        throws FinderException, RemoveException {
        ActionLocalHome actionHome;
        ActionLocal local;
        ActionPK pk;

        pk         = new ActionPK(new Integer(actionID));
        local      = getActHome().findByPrimaryKey(pk);
        local.remove();
    }

    public void ejbCreate() {}

    public void ejbRemove() {}

    public void ejbActivate() {}

    public void ejbPassivate() {}

    public void setSessionContext(SessionContext ctx) {}
}
