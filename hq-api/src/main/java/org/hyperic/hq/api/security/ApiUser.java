package org.hyperic.hq.api.security;

import org.hyperic.hq.api.common.InterfaceUser;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;

public class ApiUser implements InterfaceUser {      
    
    public ApiUser(final AuthzSubjectValue subject, final Integer sessionId, final boolean hasPrincipal) {
        this.sessionId = sessionId;
        this.subject = subject;
        this.hasPrincipal = hasPrincipal;
    }

    private final Integer sessionId;
    private final AuthzSubjectValue subject;
    private final boolean hasPrincipal;

    public AuthzSubjectValue getSubject() {
        return this.subject;
    }

    public Integer getId() {
        if (subject == null) {
            return null;
        }
        return subject.getId();
    }

    public Integer getSessionId() {
        return this.sessionId;
    }

}
