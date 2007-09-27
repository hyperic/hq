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

package org.hyperic.hq.appdef.shared;

import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.bizapp.shared.AllConfigResponses;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ResourceEventInterface;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AppdefEvent extends AbstractEvent implements java.io.Serializable,
        ResourceEventInterface {

    private static final long serialVersionUID = -7397655604166961233L;

    public static final int ACTION_CREATE = 0;

    public static final int ACTION_UPDATE = 1;

    public static final int ACTION_DELETE = 2;

    /** Sent when something has gotten a new config and we should
        turn on default metrics and runtime-AI */
    public static final int ACTION_NEWCONFIG = 3;

    /** When metrics have been enabled */
    public static final int ACTION_METRIC_ENABLED = 4;

    private static final Log log = LogFactory.getLog(AppdefEvent.class);

    private AppdefEntityID resource = null;
    
    private AllConfigResponses allConfigs = null;

    private int action;

    private AuthzSubjectValue subject;

    /** Creates a new instance of MeasurementEvent */
    public AppdefEvent(AuthzSubjectValue subject, AppdefEntityID resource,
                       int action) {
        super.setInstanceId(resource.getId());
        super.setTimestamp(System.currentTimeMillis());
        this.subject = subject;
        this.resource = resource;
        this.action = action;
    }

    public AppdefEvent(AuthzSubjectValue subject, AppdefEntityID resource,
                       int action, AllConfigResponses allConfigs) {
        this(subject, resource, action);
        this.allConfigs = allConfigs;
    }
    
    public AppdefEntityID getResource() {
        return this.resource;
    }

    public int getAction() {
        return this.action;
    }

    public void setAction(int action) {
        this.action = action;
    }
    
    public AuthzSubjectValue getSubject() {
        return subject;
    }

    public AllConfigResponses getAllConfigs() {
        return allConfigs;
    }
}
