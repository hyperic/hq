/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AllConfigResponses;
import org.hyperic.hq.zevents.ZeventManager;

/**
 * Event that indicates a resource has been updated.
 */
public class ResourceUpdatedZevent extends ResourceZevent {
    private boolean verifyConfig = true;

    static {
        ZeventManager.getInstance().registerEventClass(ResourceUpdatedZevent.class);
    }

    public ResourceUpdatedZevent(AuthzSubject subject, AppdefEntityID id) {
        super(subject.getId(), id);
    }

    public ResourceUpdatedZevent(AuthzSubject subject, AppdefEntityID id, boolean verifyConfig) {
        super(subject.getId(), id);
        this.verifyConfig = verifyConfig;
    }
    
    public ResourceUpdatedZevent(AuthzSubject subject, AppdefEntityID id, AllConfigResponses allConfgs) {
        super(new ResourceZeventSource(id), new ResourceConfigZeventPayload(subject.getId(), id, allConfgs));
    }
    
    public AllConfigResponses getAllConfigs() {
         ResourceZeventPayload payload = (ResourceZeventPayload) getPayload();
         if (payload instanceof ResourceConfigZeventPayload)
             return ((ResourceConfigZeventPayload) payload).getAllConfigs();
         
         return null;
    }
    
    private static class ResourceConfigZeventPayload 
        extends ResourceZeventPayload {
        private AllConfigResponses _allConfigs;
        
        public ResourceConfigZeventPayload(Integer subject, AppdefEntityID id, AllConfigResponses allConfgs) {
            super(subject, id);
            _allConfigs = allConfgs;
        }
        
        public AllConfigResponses getAllConfigs() {
            return _allConfigs;
        }
    }

    public boolean verifyConfig() {
        return verifyConfig;
    }
}
