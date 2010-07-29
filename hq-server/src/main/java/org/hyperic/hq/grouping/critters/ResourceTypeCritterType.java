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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
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
import org.hyperic.hq.grouping.prop.StringCritterProp;

/**
 * Meta data for ResourceTypeCritter which matches ResourceTypeName joined from
 * EAM_RESOURCE_TYPE table
 */
public class ResourceTypeCritterType
    extends BaseCritterType {

    private static final String PROP_NAME = "typeName";

    public ResourceTypeCritterType() {
        super();
        initialize("org.hyperic.hq.grouping.Resources", "resourceType");
        addPropDescription(PROP_NAME, CritterPropType.STRING);
    }

    public Critter compose(CritterDump dump) {
        return new ResourceTypeCritter(dump.getStringProp(), this, null);

    }

    public void decompose(Critter c, CritterDump dump) {
        dump.setStringProp(((ResourceTypeCritter) c).getResourceTypeName());
    }

    public boolean isUserVisible() {
        return false;
    }

    public boolean isSystem() {
        return true;
    }

    public Critter newInstance(String resTypeName, Integer protoToExclude) {
        List list = (protoToExclude == null) ? Collections.EMPTY_LIST : Collections
            .singletonList(protoToExclude);
        return new ResourceTypeCritter(resTypeName, this, list);
    }

    public Critter newInstance(Map critterProps) throws GroupException {
        validate(critterProps);
        StringCritterProp prop = (StringCritterProp) critterProps.get(PROP_NAME);
        return new ResourceTypeCritter(prop.getString(), this, null);
    }

    /**
     * Fetches all Resources which match the ResourceTypeName, joins the
     * EAM_RESOURCE_TYPE table, doesn't use proto
     */
    public class ResourceTypeCritter
        extends Object implements Critter {
        private String _resTypeName;
        private List _props;
        private ResourceTypeCritterType _type;
        private Collection _excludes;

        public ResourceTypeCritter(String resTypeName, ResourceTypeCritterType type,
                                   Collection excludes) {
            _resTypeName = resTypeName;
            _excludes = excludes;
            List c = new ArrayList();
            c.add(new StringCritterProp(PROP_NAME, resTypeName));
            _props = Collections.unmodifiableList(c);
            _type = type;
        }

        public void bindSqlParams(CritterTranslationContext ctx, Query q) {
            q.setParameter(ctx.escape("resTypeName"), _resTypeName);
        }

        public String getResourceTypeName() {
            return _resTypeName;
        }

        public String getConfig() {
            Object[] args = { _resTypeName };
            return _type.getInstanceConfig().format(args);
        }

        public CritterType getCritterType() {
            return _type;
        }

        public List getProps() {
            return _props;
        }

        public String getSql(CritterTranslationContext ctx, String resourceAlias) {
            String bool = ctx.getDialect().toBooleanValueString(false);
            return new StringBuilder().append("(@type@.name = :@resTypeName@ and ").append(
                resourceAlias).append(".fsystem = ").append(bool).append(")").toString();
        }

        public String getSqlJoins(CritterTranslationContext ctx, String resourceAlias) {
            StringBuilder rtn = new StringBuilder().append("JOIN EAM_RESOURCE_TYPE @type@ on ")
                .append(resourceAlias).append(".resource_type_id = @type@.id ");
            setExcludes(resourceAlias, rtn);
            return rtn.toString();
        }

        private void setExcludes(String resourceAlias, StringBuilder sql) {
            if (_excludes == null || _excludes.size() == 0) {
                return;
            }
            for (Iterator it = _excludes.iterator(); it.hasNext();) {
                Integer protoId = (Integer) it.next();
                if (protoId == null) {
                    continue;
                }
                sql.append(" AND ").append(resourceAlias).append(".proto_id != " + protoId);
            }
        }

        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (!(other instanceof ResourceTypeCritter))
                return false;

            // make assumptions explicit
            assert _resTypeName != null;

            ResourceTypeCritter critter = (ResourceTypeCritter) other;
            if (!_resTypeName.equals(critter._resTypeName))
                return false;
            return true;
        }

        public int hashCode() {
            int result = _resTypeName != null ? _resTypeName.hashCode() : 0;
            return result;
        }
        
        public boolean meets(Resource resource) {
            //TODO implement
            throw new UnsupportedOperationException();
        }
    }
}
