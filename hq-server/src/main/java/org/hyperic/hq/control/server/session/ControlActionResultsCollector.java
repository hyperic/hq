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

import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.control.ControlActionResult;
import org.hyperic.hq.control.GroupControlActionResult;
import org.hyperic.hq.control.shared.ControlActionTimeoutException;

/**
 * Waits for control action results from the agent
 * @author jhickey
 * 
 */
public interface ControlActionResultsCollector {

    /**
     * 
     * @param jobId The ID of the job to wait for
     * @param timeout The wait timeout
     * @return The result of the control action
     * @throws ControlActionTimeoutException
     * @throws ApplicationException
     */
    ControlActionResult waitForResult(Integer jobId, int timeout)
        throws ControlActionTimeoutException, ApplicationException;

    /**
     * 
     * @param groupId The ID of the group against which the control action was
     *        executed
     * @param ids The IDs of the group members
     * @param timeout The timeout to wait for the entire group's results
     * @return The result of the group control action
     * @throws ControlActionTimeoutException
     * @throws ApplicationException
     */
    GroupControlActionResult waitForGroupResults(AppdefEntityID groupId, List<Integer> ids,
                                                 int timeout) throws ControlActionTimeoutException,
        ApplicationException;

    /**
     * Returns the control timeout configured for a specific resource, or the
     * default timeout if control timeout is not set or could not be obtained
     * @param subject
     * @param id The ID of the resource
     * @param defaultTimeout The default timeout to use if control timeout is
     *        not configured
     * @return
     */
    int getTimeout(AuthzSubject subject, AppdefEntityID id, int defaultTimeout);
}
