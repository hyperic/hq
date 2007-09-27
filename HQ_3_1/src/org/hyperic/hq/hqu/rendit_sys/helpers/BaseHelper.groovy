package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.shared.AuthzSubjectValue

abstract class BaseHelper {
    AuthzSubject      user
	AuthzSubjectValue userValue    
	
    BaseHelper(AuthzSubject user) {
        this.user      = user
        this.userValue = user.authzSubjectValue
    }
}
