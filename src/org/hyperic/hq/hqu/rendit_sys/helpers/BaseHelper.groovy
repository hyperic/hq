package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.shared.AuthzSubjectValue

abstract class BaseHelper {
    private AuthzSubject overlord = AuthzSubjectManagerEJBImpl.one.overlordPojo 
    AuthzSubject      user
	AuthzSubjectValue userValue    
	
    BaseHelper(AuthzSubject user) {
        this.user      = user
        this.userValue = user.authzSubjectValue
    }
    
    protected AuthzSubject getOverlord() {
        this.overlord
    }
