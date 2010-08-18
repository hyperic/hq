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
package org.hyperic.hq.appdef.shared;

import java.util.List;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.NotFoundException;

/**
 * Local interface for VirtualManager.
 */
public interface VirtualManager {
    /**
     * Find virtual platforms in a VM Process
     * @return a list of virtual platform values
     */
    public List<PlatformValue> findVirtualPlatformsByVM(AuthzSubject subject, Integer vmId)
        throws PlatformNotFoundException, PermissionException;

    /**
     * Find virtual servers in a VM Process
     * @return a list of virtual server values
     */
    public List<ServerValue> findVirtualServersByVM(AuthzSubject subject, Integer vmId) throws ServerNotFoundException,
        PermissionException;

    /**
     * Find virtual services in a VM Process
     * @return a list of virtual service values
     */
    public List<ServiceValue> findVirtualServicesByVM(AuthzSubject subject, Integer vmId)
        throws ServiceNotFoundException, PermissionException;

    /**
     * Find virtual resources whose parent is the given physical ID
     * @return list of virtual resource values
     */
    public List<AppdefResourceValue> findVirtualResourcesByPhysical(AuthzSubject subject, AppdefEntityID aeid)
        throws AppdefEntityNotFoundException, PermissionException;

    /**
     * Associate an array of entities to a VM
     */
    public void associateEntities(AuthzSubject subj, Integer processId,
                                  org.hyperic.hq.appdef.shared.AppdefEntityID[] aeids);

    /**
     * Associate an array of entities to a VM
     * @throws NotFoundException
     */
    public void associateToPhysical(AuthzSubject subj, Integer physicalId, AppdefEntityID aeid) throws NotFoundException;

}
