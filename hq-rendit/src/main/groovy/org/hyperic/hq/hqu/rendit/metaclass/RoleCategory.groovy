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

package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Operation
import org.hyperic.hq.authz.server.session.Role
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.authz.shared.RoleManager;
import org.hyperic.hq.authz.shared.RoleValue

class RoleCategory {
    private static roleMan = Bootstrap.getBean(RoleManager.class)

    static void setSubjects(Role role, AuthzSubject user, Collection subjects) {
        roleMan.removeSubjects(user, role.id,
                               (role.subjects.collect {it.id}) as Integer[])
        roleMan.addSubjects(user, role.id, 
                            (subjects.collect {it.id}) as Integer[]) 
    }

    static void setGroups(Role role, AuthzSubject user, Collection groups) {
        roleMan.removeAllResourceGroups(user, role)
        roleMan.addResourceGroups(user, role.id,
                                  (groups.collect {it.id}) as Integer[])
    }

    /**
     * Update a Role
     */
    static void update(Role role, AuthzSubject user,
                       String name, String description) {

        RoleValue rv = role.getRoleValue()
        if (name) {
            rv.setName(name)
        }
        if (description) {
            rv.setDescription(description)
        }

        roleMan.saveRole(user, rv)
    }

    /**
     * Set the operations for a Role.
     */
    static void setOperations(Role role, AuthzSubject user, Collection ops) {
        roleMan.setOperations(user, role.id, ops as Operation[])
    }

    /**
     * Remove a Role.
     */
    static void remove(Role role, AuthzSubject user) {
        roleMan.removeRole(user, role.id)
    }
    
    /**
    * Get the Resource Groups for a Role
    */
    static Collection getGroups(Role role, AuthzSubject user) {
        roleMan.getResourceGroupsByRole(user, role)
    }
}
