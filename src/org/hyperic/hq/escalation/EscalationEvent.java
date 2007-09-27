/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006, 2007], Hyperic, Inc.
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

package org.hyperic.hq.escalation;

import java.io.Serializable;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.LoggableInterface;
import org.hyperic.hq.events.ResourceEventInterface;
import org.hyperic.hq.measurement.shared.ResourceLogEvent;
import org.hyperic.hq.product.LogTrackPlugin;

public class EscalationEvent extends AbstractEvent
    implements Serializable, ResourceEventInterface, LoggableInterface {

    private static final long serialVersionUID = -1512758076642974170L;

    private AppdefEntityID _aeid;
    private String _msg;
    private String _alertName;
    
    public EscalationEvent(Escalatable alert, String msg) {
        super();
        setTimestamp(System.currentTimeMillis());
        _aeid = new AppdefEntityID(
            alert.getDefinition().getDefinitionInfo().getAppdefType(),
            alert.getDefinition().getDefinitionInfo().getAppdefId());
        _alertName = alert.getDefinition().getName();
        _msg = msg;
    }

    public AppdefEntityID getResource() {
        return _aeid;
    }

    public String toString() {
        return _msg;
    }

    public String getAlertDefName() {
        return _alertName;
    }

    public String getLevelString() {
        return ResourceLogEvent.getLevelString(LogTrackPlugin.LOGLEVEL_INFO);
    }

    public String getSubject() {
        return getAlertDefName();
    }

}
