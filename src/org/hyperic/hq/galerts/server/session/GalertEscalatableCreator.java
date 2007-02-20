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
package org.hyperic.hq.galerts.server.session;

import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.EscalatableCreator;
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl;
import org.hyperic.hq.escalation.shared.EscalationManagerLocal;
import org.hyperic.hq.galerts.shared.GalertManagerLocal;

class GalertEscalatableCreator 
    implements EscalatableCreator
{
    private static final GalertManagerLocal _gMan = 
        GalertManagerEJBImpl.getOne();
    private static final EscalationManagerLocal _eMan =
        EscalationManagerEJBImpl.getOne();
    
    private GalertDef          _def;
    private ExecutionReason    _reason;
    
    GalertEscalatableCreator(GalertDef def, ExecutionReason reason) {
        _def    = def;
        _reason = reason;
    }
    
    public Escalatable createEscalatable() {
        GalertLog log = _gMan.createAlertLog(_def, _reason);

        return createEscalatable(log);
    }
    
    public static Escalatable createEscalatable(GalertLog log) {
        boolean ackable = _eMan.isAlertAcknowledgeable(log.getId(), 
                                                       log.getDefinition());
        
        return new GalertEscalatable(log, ackable);
    }
}
