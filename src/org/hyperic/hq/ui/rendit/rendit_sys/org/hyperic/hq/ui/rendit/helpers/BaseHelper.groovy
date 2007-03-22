package org.hyperic.hq.ui.rendit.helpers

import org.hyperic.hq.authz.shared.AuthzSubjectValue
import org.hyperic.hq.authz.server.session.AuthzSubject

abstract class BaseHelper {
    private user
    private userVal
    
    BaseHelper(user) {
        this.user    = user
        this.userVal = user.valueObject 
    }
        
    protected AuthzSubject      getUser()    { user }
    protected AuthzSubjectValue getUserVal() { userVal } 
}