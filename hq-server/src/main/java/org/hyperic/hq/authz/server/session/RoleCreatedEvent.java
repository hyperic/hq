package org.hyperic.hq.authz.server.session;

/**
 * Indicates that a role has been created
 * @author jhickey
 *
 */
public class RoleCreatedEvent extends AuthzApplicationEvent {

    public RoleCreatedEvent(Role role) {
        super(role);
    }
    
    public Role getRole() {
        return (Role) getSource();
    }

}
