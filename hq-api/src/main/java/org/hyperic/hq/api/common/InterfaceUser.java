package org.hyperic.hq.api.common;

import org.hyperic.hq.authz.shared.AuthzSubjectValue;

public interface InterfaceUser {
    

    /**
     * Return the AuthzSubjectValue represented by this web user.
     */
    AuthzSubjectValue getSubject();

    Integer getId();
    
    /**
     * Return the BizApp session id as an Integer for this web user
     */
    Integer getSessionId() ;
        

}