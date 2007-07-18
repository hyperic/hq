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

package org.hyperic.hq.events;

import java.util.List;
import java.util.ResourceBundle;

import org.hyperic.util.HypericEnum;

public class AlertSeverity 
    extends HypericEnum
{
    private static final String BUNDLE = "org.hyperic.hq.events.Resources";
    
    public static final AlertSeverity LOW = 
        new AlertSeverity(EventConstants.PRIORITY_LOW,
                    EventConstants.getPriority(EventConstants.PRIORITY_LOW),
                    "alert.severity.low");
    public static final AlertSeverity MEDIUM = 
        new AlertSeverity(EventConstants.PRIORITY_MEDIUM,
                    EventConstants.getPriority(EventConstants.PRIORITY_MEDIUM),
                    "alert.severity.medium");
    public static final AlertSeverity HIGH =  
        new AlertSeverity(EventConstants.PRIORITY_HIGH,
                    EventConstants.getPriority(EventConstants.PRIORITY_HIGH),
                    "alert.severity.high");

    private AlertSeverity(int code, String desc, String localeProp) {
        super(code, desc, localeProp, 
              ResourceBundle.getBundle(BUNDLE)); 
    }
    
    public static List getAll() {
        return HypericEnum.getAll(AlertSeverity.class);
    }
    
    public static AlertSeverity findByCode(int code) {
        return (AlertSeverity)findByCode(AlertSeverity.class, code);
    }
}
