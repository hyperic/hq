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
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterDump;
import org.hyperic.hq.grouping.CritterTranslationContext;
import org.hyperic.hq.grouping.CritterType;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.prop.CritterPropType;
import org.hyperic.hq.grouping.prop.GroupCritterProp;

/**
 * Metadata for GroupMembershipCritter which fetches all Resources
 * that belong to a group by GroupId
 */
public class GroupMembershipCritterType extends BaseCritterType {
    
    private static final String PROP_NAME = "group";

    public GroupMembershipCritterType() {
        super();
        initialize("org.hyperic.hq.grouping.Resources", "groupMembership"); 
        addPropDescription(PROP_NAME, CritterPropType.GROUP);
    }

    public Critter compose(CritterDump dump)
        throws GroupException {
        throw new GroupException("compose from dump not supported");
    }

    public void decompose(Critter critter, CritterDump dump)
        throws GroupException {
        throw new GroupException("decompose not supported");
    }

    public boolean isSystem() {
        return false;
    }

    public Critter newInstance(ResourceGroup group) {
        return new GroupMembershipCritter(group, this);
    }

    public Critter newInstance(List critterProps) throws GroupException {
        validate(critterProps);
        GroupCritterProp group = (GroupCritterProp)critterProps.get(0);
        return new GroupMembershipCritter(group.getGroup(), this);
    }
    
    /**
    * Gathers all Resources that belong to a ResourceGroup by GroupId
    */
   class GroupMembershipCritter implements Critter {
       
       private final ResourceGroup _group;
       private final List _props; 
       private final GroupMembershipCritterType _type;
       
       public GroupMembershipCritter(ResourceGroup group,
                                     GroupMembershipCritterType type) {
           _group = group;
           _type = type;
           List c = new ArrayList(1);
           c.add(new GroupCritterProp(type.getComponentName(PROP_NAME), group));
           _props = Collections.unmodifiableList(c);
       }
       
       public List getProps() {
           return _props;
       }

       public void bindSqlParams(CritterTranslationContext ctx, Query q) {
           q.setParameter(ctx.escape("groupId"), _group.getId());
       }

       public String getConfig() {
           Object[] args = {_group.getId()};
           return _type.getInstanceConfig().format(args);
       }

       public CritterType getCritterType() {
           return _type;
       }

       public String getSql(CritterTranslationContext ctx, String resourceAlias) {
           return "@grp@.id = :@groupId@";
       }

       public String getSqlJoins(CritterTranslationContext ctx,
                                 String resourceAlias)
       {
           return new StringBuilder()
               .append("join EAM_RES_GRP_RES_MAP @map@ on ")
               .append(resourceAlias).append(".id = @map@.resource_id ")
               .append("join EAM_RESOURCE_GROUP @grp@ on ")
               .append("@map@.resource_group_id = @grp@.id ").toString();
       }
       
       public boolean equals(Object other) {
           if (this == other) return true;
           if (!(other instanceof GroupMembershipCritter)) return false;
           
           // make assumptions explicit
           assert _group != null;
           
           GroupMembershipCritter critter = (GroupMembershipCritter) other;
           if (!_group.equals(critter._group)) return false;
           return true;
       }

       public int hashCode() {
           int result = _group != null ? _group.hashCode() : 0;
           return result;
       }
    }
}
