package org.hyperic.hq.authz.server.session;

/**
 * Sent before a subject is deleted
 * @author jhickey
 *
 */
public class SubjectDeleteRequestedEvent extends AuthzApplicationEvent {

    public SubjectDeleteRequestedEvent(AuthzSubject subject) {
        super(subject);
    }
    
    public AuthzSubject getSubject() {
        return (AuthzSubject) getSource();
    }

}
