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

package org.hyperic.hq.grouping.critters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterDump;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.prop.CritterPropType;
import org.hyperic.hq.grouping.prop.StringCritterProp;

/**
 * Metadata for GroupTypeCritter which fetches all Resources joined to
 * EAM_RES_GRP_RES_MAP and EAM_RESOURCE_GROUP by grouptype
 */
public class GroupTypeCritterType extends BaseCritterType {
    
    public GroupTypeCritterType() {
        super();
        initialize("org.hyperic.hq.grouping.Resources", "groupType"); 
        addPropDescription("typeId", CritterPropType.ENUM);
    }

    public Critter compose(CritterDump dump) throws GroupException {
        throw new GroupException("compose is not supported");
    }

    public void decompose(Critter critter, CritterDump dump)
        throws GroupException {
        throw new GroupException("decompose is not supported");
    }

    public boolean isSystem() {
        return true;
    }

    public Critter newInstance(List critterProps) throws GroupException {
        List list = new ArrayList();
        list.add(critterProps.get(0));
        validate(list);
        return new GroupTypeCritter(critterProps, this);
    }
}
