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
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.events.shared.ActionManagerLocal;
import org.hyperic.hq.events.shared.ActionManagerUtil;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.Action;
import org.json.JSONException;
import org.json.JSONObject;

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
    private ActionDAO          _actDAO;
    private AlertDAO           _alertDAO;
    private AlertDefinitionDAO _defDAO;
    
    public ActionManagerEJBImpl() {
        DAOFactory f = DAOFactory.getDAOFactory();
        
        _actDAO   = new ActionDAO(f);
        _alertDAO = new AlertDAO(f);
        _defDAO   = new AlertDefinitionDAO(f);
    }

    /**
     * Get a collection of all actions
     *
     * @return a collection of {@link ActionValue}s
     *
     * @ejb:interface-method
     */
    public Collection getAllActions() {
        return actionsToActionValues(_actDAO.findAll());
    }

    /**
     * Get all the actions for a given alert
     *
     * @return a collection of {@link ActionValue}s
     * @ejb:interface-method
     */
    public List getActionsForAlert(int alertId) {
        Alert a = _alertDAO.findById(new Integer(alertId)); 
        Collection actions = _actDAO.findByAlert(a);
        
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
     * @ejb:interface-method
     */
    public Action createAction(AlertDefinition def, ActionValue val,
                               Action parent) {
        Action action = def.createAction(val, parent);
        def.setMtime(System.currentTimeMillis());
        return action;
    }

    /**
     * Update an action
     *
     * @ejb:interface-method
     */
    public ActionValue updateAction(ActionValue val) { 
        // First update the primary action
        Action action = _actDAO.findById(val.getId());
        
        // Delete it if no configuration
        if (val.getConfig() == null) {
            _actDAO.removeAction(action);
            return null;
        }
            
        action.setActionValue(val);
        setParentAction(val, action);
        long mtime = System.currentTimeMillis();
        
        // HQ 942: We have seen orphaned actions on upgrade from 
        // 3.0.5 to 3.1.1 where the action has no associated alert def.
        // Prevent the NPE.
        if (action.getAlertDefinition() != null) {
            action.getAlertDefinition().setMtime(mtime);            
        }

        // Then find and update the child actions.

        /* It would be nice to have a more explicit method that
           does this kind of update.  XXX -- JMT */ 
        Collection children = action.getChildren();
            
        val.setParentId(val.getId());
        for (Iterator i = children.iterator(); i.hasNext(); ) {
            Action act = (Action) i.next();
            act.setActionValue(val);
            setParentAction(val, act);
            
            // HQ 942: We have seen orphaned actions on upgrade from 
            // 3.0.5 to 3.1.1 where the action has no associated alert def.
            // Prevent the NPE.
            if (act.getAlertDefinition() != null) {
                act.getAlertDefinition().setMtime(mtime);                
            }
        }
        
        return action.getActionValue();
    }

    /**
     * Create a free-standing action.  These are linked to from things like
     * escalations actions.
     * 
     * XXX:  This should really be removed -- the JSON object sucks.
     *
     * @ejb:interface-method
     */
    public Action createAction(JSONObject json) 
        throws JSONException
    {
        Action a = Action.newInstance(json); 
        
        _actDAO.save(a);
        return a;
    }

    /**
     * Create a free-standing action.  These are linked to from things like
     * escalations actions.
     * 
     * @ejb:interface-method
     */
    public Action createAction(ActionConfigInterface cfg) {
        Action a = Action.createAction(cfg); 
        
        _actDAO.save(a);
        return a;
    }

    /**
     * Mark a free-standing action as deleted.  These actions will later be 
     * deleted by a cleanup thread. 
     *
     * @ejb:interface-method
     */
    public void markActionDeleted(Action a) {
        a.setDeleted(true);
    }
    
    private void setParentAction(ActionValue val, Action action) {
        if (val.getParentId() == null) {
            action.setParent(null);
        } else {
            action.setParent(_actDAO.findById(val.getParentId()));
        }
    }
    
    public static ActionManagerLocal getOne() {
        try {
            return ActionManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}
}
