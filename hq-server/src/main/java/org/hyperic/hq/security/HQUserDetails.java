package org.hyperic.hq.security;

import java.util.Collection;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class HQUserDetails extends User {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1076990652087735299L;
    protected AuthzSubject authzSubject;

    public HQUserDetails(AuthzSubject authzSubject, String password, Collection<? extends GrantedAuthority> authorities) {
        
        super(authzSubject.getName(), password, authzSubject.getActive(), authzSubject.getActive(), authzSubject.getActive(), authzSubject.getActive(), authorities);
        this.authzSubject = authzSubject;
    }

    
    public HQUserDetails(AuthzSubjectValue authzSubject, String password, Collection<? extends GrantedAuthority> authorities) {
        
        super(authzSubject.getName(), password, authzSubject.getActive(), authzSubject.getActive(), authzSubject.getActive(), authzSubject.getActive(), authorities);
        this.authzSubject = new AuthzSubject(authzSubject.getActive(), authzSubject.getAuthDsn(), authzSubject.getDepartment(), authzSubject.getEmailAddress(), true, authzSubject.getFirstName(), authzSubject.getLastName(), authzSubject.getName(), authzSubject.getPhoneNumber(), authzSubject.getSMSAddress(), authzSubject.getSystem());
        this.authzSubject.setId(authzSubject.getId());
    }
    
    public AuthzSubject getAuthzSubject() {
        return authzSubject;
    }

    public void setAuthzSubject(AuthzSubject authzSubject) {
        this.authzSubject = authzSubject;
    }
    

}
