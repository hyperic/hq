package org.hyperic.hq.authz.server.session;

/**
 * Sent before a role is deleted
 * @author jhickey
 *
 */
public class RoleDeleteRequestedEvent extends AuthzApplicationEvent {

    public RoleDeleteRequestedEvent(Role role) {
        super(role);
    }
    
    public Role getRole() {
        return (Role) getSource();
    }

}
