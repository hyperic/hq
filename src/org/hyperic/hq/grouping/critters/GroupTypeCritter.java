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
 * Fetches all Resources which are joined from EAM_RES_GRP_RES_MAP and
 * EAM_RESOURCE_GROUP by grouptype
 */
public class GroupTypeCritter implements Critter {
    
    private final Integer _groupType;
    private final List _props; 
    private final GroupTypeCritterType _type;

    // TODO, scottmf need to add an IntegerCritterProp
    // scottmf -> please update equals and hashCode accordingly
    public GroupTypeCritter(StringCritterProp groupType,
        GroupTypeCritterType type) {
        _groupType = new Integer(groupType.getString());
        _type = type;
        List c = new ArrayList(1);
        c.add(groupType);
        _props = Collections.unmodifiableList(c);
    }

    public List getProps() {
        return _props;
    }

    public void bindSqlParams(CritterTranslationContext ctx, Query q) {
        q.setParameter(ctx.escape("groupType"), _groupType);
    }
    
    public String getConfig() {
        Object[] args = { _groupType };
        return _type.getInstanceConfig().format(args);
    }

    public CritterType getCritterType() {
        return _type;
    }

    public String getSql(CritterTranslationContext ctx, String resourceAlias) {
        return "@grp@.grouptype = :@groupType@";
    }

    public String getSqlJoins(CritterTranslationContext ctx,
        String resourceAlias) {
        return new StringBuilder().append("join EAM_RES_GRP_RES_MAP @map@ on ")
            .append(resourceAlias).append(".id = @map@.resource_id ").append(
                "join EAM_RESOURCE_GROUP @grp@ on ").append(
                "@map@.resource_group_id = @grp@.id ").toString();
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GroupTypeCritter)) return false;
        
        // make assumptions explicit
        assert _groupType != null;
        
        GroupTypeCritter critter = (GroupTypeCritter) other;
        if (!_groupType.equals(critter._groupType)) return false;
        return true;
    }

    public int hashCode() {
        int result = _groupType != null ? _groupType.hashCode() : 0;
        return result;
    }
    
}
