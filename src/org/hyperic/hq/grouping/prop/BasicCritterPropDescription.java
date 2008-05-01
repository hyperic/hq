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

public class BasicCritterPropDescription  
    implements CritterPropDescription
{
    private final CritterPropType _type;
    private final String          _id;
    private final String          _name;
    private final String          _purpose;
    private final boolean         _required;

    public BasicCritterPropDescription(CritterPropType type, String id,
                                       String name, String purpose, 
                                       boolean required) 
    {
        _id       = id;
        _name     = name;
        _type     = type;
        _purpose  = purpose;
        _required = required;
    }

    /**
     * Create a new description for the {@link CritterProp}, defaulting
     * to being a required property. 
     */
    public BasicCritterPropDescription(CritterPropType type, String id, 
                                       String name, String purpose) 
    {
        this(type, id, name, purpose, true);
    }

    public String getId() {
        return _id;
    }
    
    public String getName() {
        return _name;
    }

    public CritterPropType getType() {
        return _type;
    }

    public String getPurpose() {
        return _purpose;
    }

    public boolean isRequired() {
        return _required;
    }
}
