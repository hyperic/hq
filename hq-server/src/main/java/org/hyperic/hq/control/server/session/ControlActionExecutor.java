/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.control.server.session;

import java.util.Date;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.product.PluginException;

/**
 * Executes control actions by submitting them to the proper agent
 * @author jhickey
 * 
 */
public interface ControlActionExecutor {

    /**
     * Executes a control action by submitting it to a proper agent. Does NOT
     * wait for the agent to asynchronously return results
     * 
     * @param id The resource against which to execute the action
     * @param subjectName The name of the AuthzSubject performing the action
     * @param dateScheduled The Date scheduled
     * @param scheduled true if this was scheduled, false if "quick control"
     * @param description The description of the action
     * @param action The action to execute
     * @param args Arguments to the action or null if action has no args
     * @return The ID of the ControlHistory
     * @throws PluginException
     */
    Integer executeControlAction(AppdefEntityID id, String subjectName, Date dateScheduled,
                                 Boolean scheduled, String description, String action, String args)
        throws PluginException;

    /**
     * Executes a control action for a resource that is part of a group by
     * submitting it to a proper agent. Does NOT wait for the agent to
     * asynchronously return results
     * 
     * @param id The resource against which to execute the action
     * @param groupId The ID of the group containing the resource
     * @param batchId The ID of the group ControlHistory object
     * @param subjectName The name of the AuthzSubject performing the action
     * @param dateScheduled The Date scheduled
     * @param scheduled true if this was scheduled, false if "quick control"
     * @param description The description of the action
     * @param action The action to execute
     * @param args Arguments to the action or null if action has no args
     * @return The ID of the ControlHistory
     * @throws PluginException
     */
    Integer executeControlAction(AppdefEntityID id, AppdefEntityID groupId, Integer batchId,
                                 String subjectName, Date dateScheduled, Boolean scheduled,
                                 String description, String action, String args)
        throws PluginException;

}
