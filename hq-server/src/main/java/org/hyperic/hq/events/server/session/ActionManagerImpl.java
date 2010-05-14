/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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
import java.util.List;

import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.events.shared.ActionManager;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The action manager.
 * 
 */

@Service
@Transactional
public class ActionManagerImpl implements ActionManager {
    private ActionDAO actionDAO;
    private AlertDAO alertDAO;

    @Autowired
    public ActionManagerImpl(ActionDAO actionDAO, AlertDAO alertDAO) {
        this.actionDAO = actionDAO;
        this.alertDAO = alertDAO;
    }

    /**
     * Get all the actions for a given alert
     * 
     * @return a collection of {@link ActionValue}s
     */
    @Transactional(readOnly=true)
    public List<ActionValue> getActionsForAlert(int alertId) {
        Alert alert = alertDAO.findById(new Integer(alertId));
        Collection<Action> actions = actionDAO.findByAlert(alert);

        return actionsToActionValues(actions);
    }

    private List<ActionValue> actionsToActionValues(Collection<Action> actions) {
        List<ActionValue> res = new ArrayList<ActionValue>(actions.size());

        for (Action action : actions) {
            res.add(action.getActionValue());
        }

        return res;
    }

    /**
     * Create a new action
     */
    public Action createAction(AlertDefinition def, String className,
                               ConfigResponse config, Action parent)
        throws EncodingException {
        Action action = def.createAction(className, config.encode(), parent);
        def.setMtime(System.currentTimeMillis());
        return action;
    }

    /**
     * Update an action
     */
    public Action updateAction(ActionValue val) {
        // First update the primary action
        Action action = actionDAO.findById(val.getId());

        // Delete it if no configuration or logs
        if (val.getConfig() == null) {
            if (action.getLogEntriesBag().size() == 0) {
                actionDAO.removeAction(action);
            } else { // Disassociate from everything
                if (action.getAlertDefinition() != null) {
                    action.getAlertDefinition().getActionsBag().remove(action);
                    action.setAlertDefinition(null);
                }

                if (action.getParent() != null) {
                    action.getParent().getChildrenBag().remove(action);
                    action.setParent(null);
                }

                action.setDeleted(true);
            }
            return null;
        }

        // Set action properties from value object
        action.setClassName(val.getClassname());
        action.setConfig(val.getConfig());
        setParentAction(action, val.getParentId());
        long mtime = System.currentTimeMillis();

        // HQ 942: We have seen orphaned actions on upgrade from
        // 3.0.5 to 3.1.1 where the action has no associated alert def.
        // Prevent the NPE.
        if (action.getAlertDefinition() != null) {
            action.getAlertDefinition().setMtime(mtime);
        }

        // Then find and update the child actions.

        /*
         * It would be nice to have a more explicit method that
         * does this kind of update. XXX -- JMT
         */
        Collection<Action> children = action.getChildren();

        val.setParentId(val.getId());
        for (Action act : children) {
            act.setClassName(val.getClassname());
            act.setConfig(val.getConfig());
            setParentAction(act, val.getParentId());

            // HQ 942: We have seen orphaned actions on upgrade from
            // 3.0.5 to 3.1.1 where the action has no associated alert def.
            // Prevent the NPE.
            if (act.getAlertDefinition() != null) {
                act.getAlertDefinition().setMtime(mtime);
            }
        }

        return action;
    }

    /**
     * Create a free-standing action. These are linked to from things like
     * escalations actions.
     * 
     * XXX: This should really be removed -- the JSON object sucks.
     */
    public Action createAction(JSONObject json)
        throws JSONException {
        Action action = Action.newInstance(json);

        actionDAO.save(action);
        return action;
    }

    /**
     * Create a free-standing action. These are linked to from things like
     * escalations actions.
     */
    public Action createAction(ActionConfigInterface cfg) {
        Action action = Action.createAction(cfg);

        actionDAO.save(action);
        return action;
    }

    /**
     * Mark a free-standing action as deleted. These actions will later be
     * deleted by a cleanup thread.
     */
    public void markActionDeleted(Action action) {
        action.setDeleted(true);
    }

    private void setParentAction(Action action, Integer parent) {
        if (parent == null) {
            action.setParent(null);
        } else {
            action.setParent(actionDAO.findById(parent));
        }
    }

}
