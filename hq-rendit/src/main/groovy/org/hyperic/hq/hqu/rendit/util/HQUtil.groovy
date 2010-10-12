/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.hqu.rendit.util


import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.common.shared.HQConstants
import org.hyperic.hq.common.server.session.ServerConfigManagerImpl
import org.hyperic.hq.context.Bootstrap;

class HQUtil {
    private static final Object LOCK = new Object()
    private static String  BASE_URL
    private static Boolean IS_EE = null

    /**
     * Get the base URL which can be used to contact HQ
     */
    static String getBaseURL() {
        synchronized (LOCK) { 
            if (BASE_URL == null) {
                BASE_URL = Bootstrap.getBean(ServerConfigManager).
                                 getConfig().getProperty(HQConstants.BaseURL)
                if (BASE_URL[-1] == '/')
                    BASE_URL = BASE_URL[0..-2]
            }
        }
        BASE_URL
    }
    
    static boolean isEnterpriseEdition() {
        synchronized (LOCK) {
            if (IS_EE != null) 
                return IS_EE
                
            try {
                Class.forName("com.hyperic.hq.authz.shared.PermissionManagerImpl")
                IS_EE = true
            } catch(Exception e) {
                IS_EE = false
            }
            
            return IS_EE
        }
    }
    
    static AuthzSubject getOverlord() {
        Bootstrap.getBean(AuthzSubjectManager.class).overlordPojo
    }    
}