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

package org.hyperic.hq.hqu.rendit.helpers


import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerImpl
import org.hyperic.hq.authz.server.session.Role
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.RoleManager;
import org.hyperic.hq.authz.shared.RoleValue
import org.hyperic.hq.authz.server.session.Operation

class RoleHelper extends BaseHelper {
    def subMan = Bootstrap.getBean(AuthzSubjectManager.class)
    def roleMan = Bootstrap.getBean(RoleManager.class)

    RoleHelper(AuthzSubject user) {
        super(user)
    }

    /**
     * Find all Roles
     *
     * @return A Collection of Roles in the system.
     */
    public Collection getAllRoles() {
        roleMan.allRoles
    }

    /**
     * Find a Role by name.
     * @param name The role name to search for.
     * @return The Role with the given name, or null if that role does not
     * exist.
     */
    public Role findRoleByName(String name) {
        roleMan.findRoleByName(name)
    }

    /**
     * Find a Role by id.
     * @param id The role id to search for.
     * @return The Role with the given id, or null if that role does not exist.
     */
    public Role getRoleById(int id) {
        roleMan.getRoleById(id)
    }

    /**
     * Return a map of Operation name to Operation
     */
    public Map getOperationMap() {
        def res = [:]
        roleMan.findAllOperations().each {op ->
            res[op.name] = op
        }
        res
    }

    /**
     * Create a Role.
     */
    public Role createRole(String roleName, String roleDescription,
                           String[] operations,
                           Integer[] subjectIds, Integer[] groupIds) {

        def role = [name: roleName,
                    description: roleDescription,
                    system: false] as RoleValue

        def allOps = operationMap
        def ops = []
        operations.each {operation ->
            ops += allOps[operation]
        }

        Integer roleId = roleMan.createOwnedRole(user, role, ops as Operation[],
                                                 subjectIds, groupIds)
        getRoleById(roleId)
    }
}
