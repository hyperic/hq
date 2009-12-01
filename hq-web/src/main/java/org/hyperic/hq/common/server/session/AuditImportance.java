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
package org.hyperic.hq.common.server.session;

import java.util.ResourceBundle;

import org.hyperic.util.HypericEnum;

public class AuditImportance
    extends HypericEnum 
{
    private static final String P = "org.hyperic.hq.common.Resources";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(P);

    public static final AuditImportance LOW = 
        new AuditImportance(0, "low", "audit.importance.low", BUNDLE);
    public static final AuditImportance MEDIUM = 
        new AuditImportance(1, "medium", "audit.importance.medium", BUNDLE);
    public static final AuditImportance HIGH = 
        new AuditImportance(2, "high", "audit.importance.high", BUNDLE);
    
    protected AuditImportance(int code, String desc, String localeProp,
                              ResourceBundle bundle) 
    {
        super(AuditImportance.class, code, desc, localeProp, bundle);
    }
    
    public static AuditImportance findByCode(int code) {
        return (AuditImportance)findByCode(AuditImportance.class, code); 
    }
}
