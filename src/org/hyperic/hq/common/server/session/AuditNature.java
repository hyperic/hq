/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

public class AuditNature
    extends HypericEnum 
{
    private static final ResourceBundle BUNDLE =
        ResourceBundle.getBundle("org.hyperic.hq.common.Resources");
    
    public static AuditNature CREATE = 
        new AuditNature(0, "create", "audit.nature.create");
    public static AuditNature UPDATE = 
        new AuditNature(1, "update", "audit.nature.update");
    public static AuditNature DELETE = 
        new AuditNature(2, "delete", "audit.nature.delete");
    public static AuditNature START = 
        new AuditNature(3, "start", "audit.nature.start");
    public static AuditNature STOP = 
        new AuditNature(4, "stop", "audit.nature.stop");
    public static AuditNature ENABLE = 
        new AuditNature(5, "enable", "audit.nature.enable");
    public static AuditNature DISABLE = 
        new AuditNature(6, "disable", "audit.nature.disable");
    public static AuditNature MOVE =
        new AuditNature(7, "move", "audit.nature.move");

    protected AuditNature(int code, String desc, String localeProp) {
        super(AuditNature.class, code, desc, localeProp, BUNDLE);
    }
    
    public static AuditNature findByCode(int code) {
        return (AuditNature)findByCode(AuditNature.class, code); 
    }
}
