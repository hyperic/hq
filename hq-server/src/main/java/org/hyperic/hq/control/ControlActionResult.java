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

import org.hyperic.hq.appdef.shared.AppdefEntityID;

/**
 * Represents results of a single control action
 * @author jhickey
 * 
 */
public class ControlActionResult {
    private final String message;
    private final String status;
    private final AppdefEntityID resource;

    /**
     * 
     * @param resource The resource against which the action was executed
     * @param status The status of the action, one of
     *        ControlConstants.STATUS_COMPLETED, STATUS_FAILED, or
     *        STATUS_INPROGRESS
     * @param message The message associated with the control action
     */
    public ControlActionResult(AppdefEntityID resource, String status, String message) {
        this.status = status;
        this.message = message;
        this.resource = resource;
    }

    /**
     * 
     * @return The message associated with the control action
     */
    public String getMessage() {
        return message;
    }

    /**
     * 
     * @return The status of the action, one of
     *         ControlConstants.STATUS_COMPLETED, STATUS_FAILED, or
     *         STATUS_INPROGRESS
     */
    public String getStatus() {
        return status;
    }

    /**
     * 
     * @return The resource against which the action was executed
     */
    public AppdefEntityID getResource() {
        return resource;
    }

    /**
     * Results objects will be unique by resource within a collection
     */
    @Override
    public boolean equals(Object obj) {
        if (!(getClass().equals(obj.getClass()))) {
            return false;
        }
        return resource.equals(((ControlActionResult) obj).getResource());
    }

    @Override
    public int hashCode() {
        return resource.hashCode();
    }

    @Override
    public String toString() {
        return "ControlActionResult[resource=" + resource + ", status=" + status + ", message=" +
               message + "]";
    }

}
