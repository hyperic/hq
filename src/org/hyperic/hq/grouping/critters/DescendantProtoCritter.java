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
import java.util.Collections;
import java.util.List;

import org.hibernate.Query;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterType;
import org.hyperic.hq.grouping.prop.ResourceCritterProp;

class DescendantProtoCritter
    implements Critter
{
    private final Resource                   _root;
    private final Resource                   _proto;
    private final List                       _props; 
    private final DescendantProtoCritterType _type;
    
    DescendantProtoCritter(Resource root, Resource proto,
                           DescendantProtoCritterType type) 
    {
        _root  = root;
        _proto = proto;

        List c = new ArrayList(2);
        c.add(new ResourceCritterProp(root));
        c.add(new ResourceCritterProp(proto));
        _props = Collections.unmodifiableList(c);
        _type = type;
    }
    
    public List getProps() {
        return _props;
    }
    
    public String getSql(String resourceAlias) {
        return "proto.id = :protoId and edge.from_id = :rootId";
    }
    
    public String getSqlJoins(String resourceAlias) {
        return "join EAM_RESOURCE proto on " + 
                   resourceAlias + ".proto_id = proto.id " +
               "join EAM_RESOURCE_EDGE edge on " + 
                   resourceAlias + ".id = edge.to_id";
    }
    
    public void bindSqlParams(Query q) {
        q.setParameter("protoId", _proto.getId());
        q.setParameter("rootId", _root.getId());
    }

    public CritterType getCritterType() {
        return _type;
    }
}
