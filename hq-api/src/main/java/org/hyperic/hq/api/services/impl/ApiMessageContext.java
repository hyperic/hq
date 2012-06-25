/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
 *
 * **********************************************************************
 * 20 June 2012
 * Maya Anderson
 * *********************************************************************/
package org.hyperic.hq.api.services.impl;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;

/**
 * Message context holder to be passed between api methods.
 * @author Maya Anderson
 *
 */
public class ApiMessageContext {

    private final Integer sessionId;
    private final AuthzSubject authzSubject;
    


    public ApiMessageContext(Integer sessionId, AuthzSubject authzSubject) {
        this.sessionId = sessionId;
           this.authzSubject = authzSubject;
    }

    public AuthzSubject getAuthzSubject() {
        return this.authzSubject;
    }
    
    public Integer getSessionId() {
        return this.sessionId;
    }
}
