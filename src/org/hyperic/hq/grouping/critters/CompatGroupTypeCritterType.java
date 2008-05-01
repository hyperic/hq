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
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.type.IntegerType;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterDump;
import org.hyperic.hq.grouping.CritterTranslationContext;
import org.hyperic.hq.grouping.CritterType;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.prop.CritterPropType;
import org.hyperic.hq.grouping.prop.EnumCritterProp;
import org.hyperic.hq.grouping.prop.ResourceCritterProp;

public class CompatGroupTypeCritterType extends BaseCritterType {

    private static final String PROTO_NAME_PROP = "protoName";

    public CompatGroupTypeCritterType() {
        super();
        initialize("org.hyperic.hq.grouping.Resources", "compatGroupType");
        addPropDescription(PROTO_NAME_PROP, CritterPropType.ENUM, false);
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

    public Critter newInstance(List props) throws GroupException {
        validate(props);
        Resource resource = null;
        if (props.size() > 0) {
            resource = ((ResourceCritterProp) props.get(1)).getResource();
        }
        return new CompatGroupTypeCritter(resource, this);
    }

    public Critter newInstance(Resource proto)
        throws GroupException {
        return new CompatGroupTypeCritter(proto, this);
    }

    public Critter newInstance() throws GroupException {
        return new CompatGroupTypeCritter(null, this);
    }

    /**
     * Fetches all Groups (not members) from the EAM_RESOURCE table joined by
     * instance_id from EAM_RESOURCE_GROUP
     */
    class CompatGroupTypeCritter implements Critter {

        private final List _groupTypes = new ArrayList();
        private final List _props;
        private final CompatGroupTypeCritterType _type;
        private Resource _proto;
        private final Integer COMPAT_GROUP_PLATFORM_SERVER = 
            new Integer(AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS);
        private final Integer COMPAT_SERVICE_CLUSTER =
            new Integer(AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC);

        /**
         * @param groupTypes
         *            List of Integers which represent grouptypes
         */
        public CompatGroupTypeCritter(Resource prototype,
            CompatGroupTypeCritterType type) {
            _groupTypes.add(this.COMPAT_GROUP_PLATFORM_SERVER);
            _groupTypes.add(this.COMPAT_SERVICE_CLUSTER);
            _type = type;
            List props = new ArrayList();
            if (prototype != null) {
                props.add(new ResourceCritterProp(
                    type.getComponentName(PROTO_NAME_PROP), prototype));
                _proto = prototype;
            }
            _props = Collections.unmodifiableList(props);
        }

        private void setIdList(List groupTypes) {
            for (Iterator i = groupTypes.iterator(); i.hasNext();) {
                Integer type = (Integer) i.next();
                _groupTypes.add(type);
            }
        }

        public List getProps() {
            return _props;
        }

        public void bindSqlParams(CritterTranslationContext ctx, Query q) {
            if (_proto != null) {
                q.setParameter(ctx.escape("proto"), _proto.getId());
            }
            q.setParameterList(ctx.escape("groupTypes"), _groupTypes,
                new IntegerType());
        }

        public String getConfig() {
            if (_proto == null) {
                Object[] args = { _groupTypes };
                return _type.getInstanceConfig().format(args);
            } else {
                Object[] args = { _groupTypes, _proto };
                return _type.getInstanceConfig().format(args);
            }
        }

        public CritterType getCritterType() {
            return _type;
        }

        public String getSql(CritterTranslationContext ctx, String resourceAlias) {
            if (_proto == null) {
                return "(@grp@.grouptype in (:@groupTypes@))";
            } else {
                return new StringBuilder().append(
                    "(@grp@.grouptype in (:@groupTypes@) and ").append(
                    "@grp@.resource_prototype = :@proto@)").toString();
            }
        }

        public String getSqlJoins(CritterTranslationContext ctx,
            String resourceAlias) {
            return new StringBuilder().append(
                "JOIN EAM_RESOURCE_GROUP @grp@ on ").append(resourceAlias)
                .append(".instance_id = @grp@.id ").toString();
        }

        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof CompatGroupTypeCritter)) return false;
            
            // make assumptions explicit
            assert _groupTypes != null;
            
            CompatGroupTypeCritter critter = (CompatGroupTypeCritter) other;
            if (!_groupTypes.equals(critter._groupTypes)) return false;
            return true;
        }

        public int hashCode() {
            int rtn = 0;
            if (_groupTypes == null) {
                return 0;
            }
            for (Iterator i = _groupTypes.iterator(); i.hasNext();) {
                rtn += i.next().hashCode();
            }
            return rtn;
        }
    }

}
