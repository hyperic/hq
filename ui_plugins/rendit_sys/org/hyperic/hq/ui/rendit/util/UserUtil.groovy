package org.hyperic.hq.ui.rendit.util

import org.hyperic.hq.ui.util.ContextUtils
import org.hyperic.hq.ui.util.RequestUtils
import org.hyperic.hq.ui.rendit.InvocationBindings
import org.hyperic.hq.authz.server.session.AuthzSubject

class UserUtil {
    static AuthzSubject getUser(InvocationBindings b) {
        def sessId = RequestUtils.getSessionId(b.request)
        def ctx    = b.request.session.servletContext
   
        return ContextUtils.getAuthzBoss(ctx).getCurrentSubject(sessId)
    }
}