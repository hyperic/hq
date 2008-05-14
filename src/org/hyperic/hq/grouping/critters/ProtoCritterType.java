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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterDump;
import org.hyperic.hq.grouping.CritterTranslationContext;
import org.hyperic.hq.grouping.CritterType;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.prop.CritterPropType;
import org.hyperic.hq.grouping.prop.ProtoCritterProp;
import org.hyperic.hq.grouping.prop.ResourceCritterProp;

/**
 * Metadata for ProtoCritter which matches all Prototypes in EAM_RESOURCE
 * by proto_id
 */
public class ProtoCritterType extends BaseCritterType {
    
    private static final String PROP_NAME = "protoType";

    public ProtoCritterType() {
        super();
        initialize("org.hyperic.hq.grouping.Resources", "proto"); 
        addPropDescription(PROP_NAME, CritterPropType.PROTO);
    }

    public Critter compose(CritterDump dump) throws GroupException {
        return newInstance(dump.getResourceProp());
    }

    public void decompose(Critter critter, CritterDump dump)
        throws GroupException
    {
        ProtoCritter protoCritter = (ProtoCritter)critter;
        dump.setResourceProp(protoCritter.getProto());
    }

    public boolean isSystem() {
        return false;
    }
    
    public ProtoCritter newInstance(Resource name) { 
        return new ProtoCritter(name, this);
    }

    public Critter newInstance(Map critterProps) throws GroupException {
        validate(critterProps);
        ProtoCritterProp protoProp = (ProtoCritterProp)
            critterProps.get(PROP_NAME);
        return new ProtoCritter(protoProp.getProtoType(), this);
    }
    
    /**
     * Fetches all the Prototypes from EAM_RESOURCE by proto_id
     */
    public class ProtoCritter implements Critter {
        
        private final List _props; 
        private final Resource _proto;
        private final ProtoCritterType _type;
        
        ProtoCritter(Resource proto, ProtoCritterType type) {
            _proto = proto;
            List c = new ArrayList();
            c.add(new ResourceCritterProp(PROP_NAME, proto));
            _props = Collections.unmodifiableList(c);
            _type  = type;
        }
        
        public Resource getProto() {
            return _proto;
        }
        
        public List getProps() {
            return _props;
        }
        
        public String getSql(CritterTranslationContext ctx, String resourceAlias) {
            return "@proto@.id = :@protoId@";
        }
        
        public String getSqlJoins(CritterTranslationContext ctx, 
                                  String resourceAlias) 
        {
            return new StringBuilder()
                .append("JOIN EAM_RESOURCE @proto@ on ")
                .append(resourceAlias)
                .append(".proto_id = @proto@.id ")
                .toString();
        }
        
        public void bindSqlParams(CritterTranslationContext ctx, Query q) {
            q.setParameter(ctx.escape("protoId"), _proto.getId());
        }

        public CritterType getCritterType() {
            return _type;
        }
        
        public String getConfig() {
            Object[] args = {_proto.getName()};
            return _type.getInstanceConfig().format(args);
        }
        
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ProtoCritter)) return false;
            
            // make assumptions explicit
            assert _proto != null;
            
            ProtoCritter critter = (ProtoCritter) other;
            if (!_proto.equals(critter._proto)) return false;
            return true;
        }

        public int hashCode() {
            int result = _proto != null ? _proto.hashCode() : 0;
            return result;
        }
    }

}
