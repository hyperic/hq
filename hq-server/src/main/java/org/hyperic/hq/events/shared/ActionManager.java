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
package org.hyperic.hq.events.shared;

import java.util.List;

import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Local interface for ActionManager.
 */
public interface ActionManager {
    /**
     * Get all the actions for a given alert
     * @return a collection of {@link ActionValue}s
     */
    public List<ActionValue> getActionsForAlert(int alertId);

    /**
     * Create a new action
     */
    public Action createAction(AlertDefinition def, String className, ConfigResponse config, Action parent)
        throws EncodingException;

    /**
     * Update an action
     */
    public Action updateAction(ActionValue val);

    /**
     * Create a free-standing action. These are linked to from things like
     * escalations actions. XXX: This should really be removed -- the JSON
     * object sucks.
     */
    public Action createAction(JSONObject json) throws JSONException;

    /**
     * Create a free-standing action. These are linked to from things like
     * escalations actions.
     */
    public Action createAction(ActionConfigInterface cfg);

    /**
     * Mark a free-standing action as deleted. These actions will later be
     * deleted by a cleanup thread.
     */
    public void markActionDeleted(Action a);

}
