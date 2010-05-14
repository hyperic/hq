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
package org.hyperic.hq.grouping.prop;

import java.util.ResourceBundle;

import org.hyperic.util.HypericEnum;

/**
 * This enumeration defines all of the datatypes that are possible to use
 * via {@link Critter}s.  Any UI must be able to interact with (render, set,
 * etc.) all values defined. 
 */
public final class CritterPropType 
    extends HypericEnum
{
    private static final ResourceBundle BUNDLE =
        ResourceBundle.getBundle("org.hyperic.hq.grouping.Resources");
    
    public static CritterPropType STRING = 
        new CritterPropType(0, "string", "critter.propType.string");
    public static CritterPropType DATE = 
        new CritterPropType(1, "date", "critter.propType.date");
    public static CritterPropType ENUM = 
        new CritterPropType(2, "enum", "critter.propType.enum");
    public static CritterPropType RESOURCE = 
        new CritterPropType(3, "resource", "critter.propType.resource");
    public static CritterPropType PROTO = 
        new CritterPropType(4, "proto", "critter.propType.proto");
    public static CritterPropType GROUP = 
        new CritterPropType(5, "group", "critter.propType.group");
    public static CritterPropType SUBJECT = 
        new CritterPropType(6, "subject", "critter.propType.subject");
    
    
    protected CritterPropType(int code, String desc, String localeProp) {
        super(CritterPropType.class, code, desc, localeProp, BUNDLE);
    }
    
    public static CritterPropType findByCode(int code) {
        return (CritterPropType)findByCode(CritterPropType.class, code);  
    }
}
