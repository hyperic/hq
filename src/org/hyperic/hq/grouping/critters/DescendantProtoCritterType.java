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

import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterDump;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.prop.CritterPropType;
import org.hyperic.hq.grouping.prop.ProtoCritterProp;
import org.hyperic.hq.grouping.prop.ResourceCritterProp;

/**
 * This type of criteria is used to provide the same functionality as
 * autogroups.
 *
 * "Show me all descendants of this platform that are of type 'Fileserver File'"
 */
public class DescendantProtoCritterType
    extends BaseCritterType
{
    private Resource _root;
    private Resource _proto;
    
    public DescendantProtoCritterType() {
        initialize("org.hyperic.hq.grouping.Resources", "descendantProto"); 
        addPropDescription("root", CritterPropType.RESOURCE);
        addPropDescription("protoType", CritterPropType.PROTO);
    }

    public Resource getRoot() {
        return _root;
    }
    
    public Resource getPrototype() {
        return _proto;
    }

    public DescendantProtoCritter newInstance(Resource root, Resource proto) {
        return new DescendantProtoCritter(root, proto, this);
    }
    
    public Critter newInstance(List critterProps)
        throws GroupException
    {
        validate(critterProps);
        
        ResourceCritterProp root = (ResourceCritterProp) critterProps.get(0);
        ProtoCritterProp proto = (ProtoCritterProp) critterProps.get(1);
        return new DescendantProtoCritter(root.getResource(), 
                                          proto.getProtoType(), this);
    }

    public Critter compose(CritterDump dump) throws GroupException {
        throw new GroupException("compose from dump not supported");
    }

    public void decompose(Critter critter, CritterDump dump)
        throws GroupException 
    {
        throw new GroupException("decompose not supported");
    }
    
    public boolean isSystem() {
        return false;
    }
}
