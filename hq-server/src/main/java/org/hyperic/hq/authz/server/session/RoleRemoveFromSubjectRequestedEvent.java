package org.hyperic.hq.authz.server.session;

/**
 * Sent before a role is removed from a subject
 * @author jhickey
 * 
 */
public class RoleRemoveFromSubjectRequestedEvent
    extends AuthzApplicationEvent {
    private Role role;

    public RoleRemoveFromSubjectRequestedEvent(AuthzSubject subject, Role role) {
        super(subject);
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public AuthzSubject getSubject() {
        return (AuthzSubject) getSource();
    }

}
