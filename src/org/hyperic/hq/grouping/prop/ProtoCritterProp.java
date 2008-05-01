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

import org.hyperic.hq.authz.server.session.Resource;

public class ProtoCritterProp
    implements CritterProp
{
    private final Resource _proto;
    private final String _name;
    
    public ProtoCritterProp(String name, Resource proto) {
        _proto = proto;
        _name = name;
    }
    
    public Resource getProtoType() {
        return _proto;
    }
    
    public String toString() {
        return _proto.getName();
    }

    public CritterPropType getType() {
        return CritterPropType.PROTO;
    }

    public String getName() {
        return _name;
    }
}
