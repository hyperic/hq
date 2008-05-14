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
import org.hyperic.hq.grouping.CritterTranslator;
import org.hyperic.hq.grouping.CritterType;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.prop.CritterPropType;
import org.hyperic.hq.grouping.prop.ProtoCritterProp;

public class CompatGroupTypeCritterType extends BaseCritterType {

    private static final String PROTO_NAME_PROP = "protoName";

    public CompatGroupTypeCritterType() {
        super();
        initialize("org.hyperic.hq.grouping.Resources", "compatGroupType");
        addPropDescription(PROTO_NAME_PROP, CritterPropType.PROTO, false);
    }

    public Critter compose(CritterDump dump) throws GroupException {
        return new CompatGroupTypeCritter(dump.getResourceProp(), this);
    }

    public void decompose(Critter critter, CritterDump dump)
        throws GroupException 
    {
        CompatGroupTypeCritter c = (CompatGroupTypeCritter)critter;
        dump.setResourceProp(c.getPrototype());
    }

    public boolean isSystem() {
        return false;
    }

    public Critter newInstance(Map props) throws GroupException {
        validate(props);
        Resource resource = 
            ((ProtoCritterProp)props.get(PROTO_NAME_PROP)).getProtoType();
            
        return new CompatGroupTypeCritter(resource, this);
    }

    public Critter newInstance(Resource proto) { 
        return new CompatGroupTypeCritter(proto, this);
    }

    public Critter newInstance() { 
        return new CompatGroupTypeCritter(null, this);
    }

    /**
     * Fetches all Groups (not members) from the EAM_RESOURCE table joined by
     * instance_id from EAM_RESOURCE_GROUP
     */
    class CompatGroupTypeCritter implements Critter {
        private final List _props;
        private final CompatGroupTypeCritterType _type;
        private final Resource _proto;

        public CompatGroupTypeCritter(Resource prototype,
                                      CompatGroupTypeCritterType type) 
        {
            _proto = prototype;
            _type  = type;
            List props = new ArrayList();
            if (prototype != null) {
                props.add(new ProtoCritterProp(PROTO_NAME_PROP, prototype));
            }
            _props = Collections.unmodifiableList(props);
        }
        
        public Resource getPrototype() {
            return _proto;
        }

        public List getProps() {
            return _props;
        }

        public void bindSqlParams(CritterTranslationContext ctx, Query q) {
            if (_proto != null) {
                q.setParameter(ctx.escape("proto"), _proto.getId());
            }
        }

        public String getConfig() {
            if (_proto == null) {
                Object[] args = { "any" };
                return _type.getInstanceConfig().format(args);
            } else {
                Object[] args = { _proto.getName() };
                return _type.getInstanceConfig().format(args);
            }
        }

        public CritterType getCritterType() {
            return _type;
        }

        public String getSql(CritterTranslationContext ctx, String resourceAlias) 
        {
            if (_proto == null) {
                return "@grp@.resource_prototype is not null";
            } else {
                return  "@grp@.resource_prototype = :@proto@";
            }
        }

        public String getSqlJoins(CritterTranslationContext ctx,
                                  String resourceAlias) 
        {
            return new StringBuilder()
                .append("JOIN EAM_RESOURCE_GROUP @grp@ on ")
                .append(resourceAlias)
                .append(".id = @grp@.resource_id").toString();
        }

        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof CompatGroupTypeCritter)) return false;
            
            // make assumptions explicit
            assert _proto != null;
            
            CompatGroupTypeCritter critter = (CompatGroupTypeCritter) other;
            return  _proto == critter._proto ||
                    (_proto != null && _proto.equals(critter._proto)) ;
        }

        public int hashCode() {
            if (_proto == null) {
                return 17;
            }
            return _proto.hashCode();
        }
    }

}
