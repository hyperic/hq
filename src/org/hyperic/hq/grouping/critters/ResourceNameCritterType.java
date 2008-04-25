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

package org.hyperic.hq.grouping.critters;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterDump;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.prop.CritterPropType;
import org.hyperic.hq.grouping.prop.StringCritterProp;

/**
 * This type of criteria is able to match resources by their name
 *
 * "Show me all resources named '.*web-infra.*'"
 */
public class ResourceNameCritterType
    extends BaseCritterType
{
    public ResourceNameCritterType() {
        initialize("org.hyperic.hq.grouping.Resources", "resourceName"); 
        addPropDescription("name", CritterPropType.STRING);
    }

    public ResourceNameCritter newInstance(String name) 
        throws GroupException, PatternSyntaxException
    {
        List props = new ArrayList(1);
        props.add(new StringCritterProp(name));
        return (ResourceNameCritter)newInstance(props);
    }
    
    public Critter newInstance(List critterProps)
        throws GroupException
    {
        validate(critterProps);
        
        StringCritterProp c = (StringCritterProp) critterProps.get(0);
        return new ResourceNameCritter(c.getString(), this);
    }
    
    public Critter compose(CritterDump dump) throws GroupException {
        return newInstance(dump.getStringProp());
    }

    public void decompose(Critter critter, CritterDump dump)
        throws GroupException 
    {
        // verify that critter is of the right type
        if (!(critter instanceof ResourceNameCritter))
            throw new GroupException("Critter is not of valid type " + 
                                     "ResourceNameCritter");
        
        ResourceNameCritter resourceCritter = (ResourceNameCritter)critter;
        dump.setStringProp(resourceCritter.getNameRegex());
    }

    public boolean isSystem() {
        return false;
    }
}
