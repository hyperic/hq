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
