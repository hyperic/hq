/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.auth.shared;

import org.hyperic.hq.authz.shared.AuthzSubjectValue;

public class AuthSession {

    private long _timeout = 0;
    private AuthzSubjectValue _subject = null;

    /**
     * The last access time for this subject.  Used for cache invalidation
     */
    private long _lastAccess = 0;

    /**
     * Default constructor.
     *
     * @param subject The subject to store
     * @param timeout The timeout for this session in milliseconds
     */
    protected AuthSession(AuthzSubjectValue subject, long timeout) {
        
        _subject = subject;
        _timeout = timeout;
        _lastAccess = System.currentTimeMillis();
    }
    
    /**
     * Return the AuthzSubjectValue for this session
     */
    protected AuthzSubjectValue getAuthzSubjectValue() {
        
        _lastAccess = System.currentTimeMillis();
        return _subject;
    }

    /**
     * Check if this session is expired
     */
    protected boolean isExpired() {
        
        if (System.currentTimeMillis() > (_lastAccess + _timeout)) {
            return true;
        }

        return false;
    }
}
