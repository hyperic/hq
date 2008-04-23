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

import org.hibernate.Query;
import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterTranslationContext;
import org.hyperic.hq.grouping.CritterType;
import org.hyperic.hq.grouping.prop.StringCritterProp;

/**
 * Fetches all Resources which match the ResourceTypeName, joins the
 * EAM_RESOURCE_TYPE table, doesn't use proto
 */
public class ResourceTypeCritter extends Object implements Critter {
    private String _resTypeName;
    private List _props;
    private ResourceTypeCritterType _type;

    public ResourceTypeCritter(String resTypeName,
                                    ResourceTypeCritterType type)
    {
        _resTypeName = resTypeName;
        List c = new ArrayList();
        c.add(new StringCritterProp(resTypeName));
        _props = Collections.unmodifiableList(c);
        _type  = type;
    }

    public void bindSqlParams(CritterTranslationContext ctx, Query q) {
        q.setParameter(ctx.escape("resTypeName"), _resTypeName);
    }

    public String getConfig() {
        Object[] args = {_resTypeName};
        return _type.getInstanceConfig().format(args);
    }

    public CritterType getCritterType() {
        return _type;
    }

    public List getProps() {
        return _props;
    }

    public String getSql(CritterTranslationContext ctx, String resourceAlias) {
        String  bool = ctx.getDialect().toBooleanValueString(false);
        return new StringBuilder()
            .append("(@type@.name = :@resTypeName@ and ")
            .append(resourceAlias).append(".fsystem = ")
            .append(bool).append(")").toString();
    }

    public String getSqlJoins(CritterTranslationContext ctx,
                              String resourceAlias)
    {
        return new StringBuilder()
            .append("JOIN EAM_RESOURCE_TYPE @type@ on ")
            .append(resourceAlias)
            .append(".resource_type_id = @type@.id").toString();
    }
    
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ResourceTypeCritter)) return false;
        
        // make assumptions explicit
        assert _resTypeName != null;
        
        ResourceTypeCritter critter = (ResourceTypeCritter) other;
        if (!_resTypeName.equals(critter._resTypeName)) return false;
        return true;
    }

    public int hashCode() {
        int result = _resTypeName != null ? _resTypeName.hashCode() : 0;
        return result;
    }
}
