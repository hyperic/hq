package org.hyperic.hq.hqu.rendit.util

import org.hyperic.hq.common.shared.HQConstants
import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl

class HQUtil {
    private static final Object LOCK = new Object()
    private static String BASE_URL

    /**
     * Get the base URL which can be used to contact HQ
     */
    static String getBaseURL() {
        synchronized (LOCK) {
            if (BASE_URL == null) {
                BASE_URL = ServerConfigManagerEJBImpl.one.
                                 getConfig().getProperty(HQConstants.BaseURL)
                if (BASE_URL[-1] == '/')
                    BASE_URL = BASE_URL[0..-2]
            }
        }
        BASE_URL
    }
}