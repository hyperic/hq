package org.hyperic.hq.ui.rendit.helpers

import org.hyperic.hq.common.shared.HQConstants
import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl

class HQHelper
    extends BaseHelper
{
    HQHelper(user) {
        super(user)
    }

    private getServerMan() { ServerConfigManagerEJBImpl.one }
    
    String getServerURL() {
        def res = serverMan.config.getProperty(HQConstants.BaseURL)
        if (!res.endsWith('/')) {
            res += '/'
        }
        res
    }
}
