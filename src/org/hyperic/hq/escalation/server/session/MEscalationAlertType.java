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
package org.hyperic.hq.escalation.server.session;

import org.hyperic.util.HypericEnum;

/**
 * This is a dynamic enumeration.  Other subsystems wishing to specify their
 * own alert type into the escalation manager must create a subclass of
 * this enumeration with a unique code.
 */
public abstract class MEscalationAlertType 
    extends HypericEnum 
    implements EscalatableFinder
{
    protected MEscalationAlertType(int code, String desc) {
        super(MEscalationAlertType.class, code, desc);
    }
    
    public static MEscalationAlertType findByCode(int code) {
        return (MEscalationAlertType)findByCode(MEscalationAlertType.class, 
                                                code);
    }
}
