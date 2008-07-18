package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.authz.server.session.RoleManagerEJBImpl as RoleMan
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl as SubMan
import org.hyperic.hq.authz.server.session.Role
import org.hyperic.hq.authz.shared.RoleValue
import org.hyperic.hq.authz.server.session.Operation

class RoleHelper extends BaseHelper {
    def subMan = SubMan.one
    def roleMan = RoleMan.one

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
    private Map makeOpNameToOpMap() {
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

        def allOps = makeOpNameToOpMap()
        def ops = []
        operations.each {operation ->
            ops += allOps[operation]
        }

        Integer roleId = roleMan.createOwnedRole(user, role, ops as Operation[],
                                                 subjectIds, groupIds)
        getRoleById(roleId)
    }

    /**
     * Update a Role
     *
     * @param role The role to update
     * @param name The new name for the role.  If null, the name will not
     * be set.
     * @param description The new description for the role.  If null,
     * the description will not be set.
     */
    public void updateRole(Role role, String name, String description) {

        RoleValue rv = role.getRoleValue()
        rv.setName(name)
        rv.setDescription(description)

        // Save role
        roleMan.saveRole(user, rv)
    }
    
    /**
     * Set the operations for a Role
     */
    public void setOperations(Role role, String[] operations)
    {
        RoleValue rv = role.getRoleValue()

        def allOps = makeOpNameToOpMap()
        def ops = []
        operations.each {operation ->
            ops += allOps[operation]
        }

        roleMan.setOperations(user, rv, ops as Operation[])
    }
}
