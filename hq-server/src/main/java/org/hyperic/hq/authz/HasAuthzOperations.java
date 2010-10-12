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

package org.hyperic.hq.authz;

import org.hyperic.hq.authz.shared.AuthzConstants;


public interface HasAuthzOperations {
    /**
     * Get the Authz permission to perform the operation.
     *
     * e.g. getAuthzOp('create')   // -> "createServer"
     *      getAuthzOp('modify')   // -> "modifyServer"
     * 
     * remove, add, view, monitor, control, manage
     * 
     * 
     * Classes implementing this interface should return the associated
     * operation type (as in {@link AuthzConstants}), or throw an InvalidArgument 
     * exception.
     */
    String getAuthzOp(String op);
}
