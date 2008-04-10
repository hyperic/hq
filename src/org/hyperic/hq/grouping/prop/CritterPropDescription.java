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

package org.hyperic.hq.grouping.prop;

/**
 * The CritterPropDescription objects are immutable beans, created by
 * {@link CritterType}s to inform the external world about what types
 * of arguments they take.
 * 
 * A list of {@link CritterPropDescription}s can easily be displayed in the UI
 * in the following format:
 * 
 * shortName:  purpose  (type.getValue())
 *  ...
 *  
 * For instance: 
 *     
 *     Name: a regex to match against the resource name (string) 
 *     Age:  only allow resources created before this time (date)
 */
public class CritterPropDescription  {
    private CritterPropType _type;
    private String          _name;
    private String          _purpose;
    
    public CritterPropDescription(CritterPropType type,
                                  String name, String purpose)
    {
        _name    = name;
        _type    = type;
        _purpose = purpose;
    }

    /**
     * Returns a localized name for this prop.  Used by the UI
     * to generically argument types for critters.
     */
    public String getName() {
        return _name;
    }
    
    /**
     * Return the type (I am a DATE, I am a STRING, etc.)
     */
    public CritterPropType getType() {
        return _type;
    }

    /**
     * Return a localized purpose.
     */
    public String getPurpose() {
        return _purpose;
    }
}
