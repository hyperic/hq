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

package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.appdef.shared.AppdefEntityConstants
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerImpl
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.events.server.session.Alert
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.galerts.server.session.GalertDef
import org.hyperic.hq.galerts.server.session.GalertLog

class AlertCategory {
    static String urlFor(Alert a, String context) {
        def d = a.alertDefinition
        "/alerts/Alerts.do?mode=viewAlert&eid=${d.appdefEntityId}&a=${a.id}" 
    }
    
    static String urlFor(GalertLog a, String context) {
        def d = a.alertDef
        "/alerts/Alerts.do?mode=viewAlert&eid=${d.appdefID}&a=${a.id}"
    }

    static String urlFor(GalertDef d, String context) {
        def groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP
        if (context == 'listAlerts') {
            return "/alerts/Alerts.do?mode=list&rid=${d.group.id}&type=${groupType}"            
        }
        "/alerts/Config.do?mode=viewGroupDefinition&eid=${groupType}:${d.group.id}&ad=${d.id}"       
    }

    static AuthzSubject getAcknowledgedBy(Alert a) {
        _getAcknowledgedBy(a.ackedBy)
    }

    static AuthzSubject getAcknowledgedBy(GalertLog a) {
        _getAcknowledgedBy(a.ackedBy)
    }
    
    private static AuthzSubject _getAcknowledgedBy(id) {
        if (id == null)
            return null
            
       Bootstrap.getBean(AuthzSubjectManager.class).getSubjectById(id.toInteger())
    }
    
    static void fix(Alert a, AuthzSubject subject, String reason) {
      Bootstrap.getBean(EscalationManager.class).fixAlert(subject, ClassicEscalationAlertType.CLASSIC,a.id,reason)
    }
}
