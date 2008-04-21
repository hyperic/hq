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

import java.util.Collections;
import java.util.List;

import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterDump;
import org.hyperic.hq.grouping.GroupException;

/**
 * Filters out system resources.
 */
public class NonSystemCritterType
    extends BaseCritterType
{
    public NonSystemCritterType() {
        initialize("org.hyperic.hq.grouping.Resources", "nonSystem"); 
    }

    public Critter newInstance(List critterProps)
        throws GroupException
    {
        validate(critterProps);
        return new NonSystemCritter(this);
    }
    
    public Critter compose(CritterDump dump) throws GroupException {
        return newInstance(Collections.EMPTY_LIST);
    }

    public void decompose(Critter critter, CritterDump dump)
        throws GroupException 
    {
        // verify that critter is of the right type
        if (!(critter instanceof NonSystemCritter)) {
            throw new GroupException("Critter is not of valid type " + 
                                     "NonSystemCritter");
        }
    }
    
    public boolean isSystem() {
        return true;
    }
}
