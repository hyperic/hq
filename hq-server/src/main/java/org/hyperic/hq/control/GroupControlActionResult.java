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
package org.hyperic.hq.control;

import java.util.Set;

import org.hyperic.hq.appdef.shared.AppdefEntityID;

/**
 * Represents the results of a control action executed against a group of
 * resources
 * @author jhickey
 * 
 */
public class GroupControlActionResult
    extends ControlActionResult {

    private final Set<ControlActionResult> individualResults;

    /**
     * @param individualResults The results of the operation for each member of
     *        the group. Depending on the execution strategy, not all group
     *        member results will be reported
     * @param resource The resource against which the action was executed
     * @param status The status of the action, one of
     *        ControlConstants.STATUS_COMPLETED, STATUS_FAILED, or
     *        STATUS_INPROGRESS
     * @param message The message associated with the control action
     */
    public GroupControlActionResult(Set<ControlActionResult> individualResults,
                                    AppdefEntityID groupId, String status, String message) {
        super(groupId, status, message);
        this.individualResults = individualResults;

    }

    /**
     * 
     * @return The results of the operation for each member of the group.
     *         Depending on the execution strategy, not all group member results
     *         will be reported
     */
    public Set<ControlActionResult> getIndividualResults() {
        return individualResults;
    }

    @Override
    public String toString() {
        return "GroupControlActionResult[group=" + getResource() + ", status=" + getStatus() +
               ", message=" + getMessage() + "]";
    }

}
