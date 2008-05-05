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
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterDump;
import org.hyperic.hq.grouping.CritterTranslationContext;
import org.hyperic.hq.grouping.CritterType;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.prop.CritterPropType;
import org.hyperic.hq.grouping.prop.SubjectCritterProp;

/**
 * Filters in resources owned by subject.
 */
public class OwnedCritterType
    extends BaseCritterType
{
    private static final String OWNER_PROP = "owner";
    
    public OwnedCritterType() {
        initialize("org.hyperic.hq.grouping.Resources", "owned");
        addPropDescription(OWNER_PROP, CritterPropType.SUBJECT);
    }
    
    public Critter newInstance(AuthzSubject subj) {
        return new OwnedCritter(subj, this);
    }

    public Critter newInstance(Map critterProps)
        throws GroupException
    {
        validate(critterProps);
        
        SubjectCritterProp owner = (SubjectCritterProp) 
            critterProps.get(OWNER_PROP);
        
        return new OwnedCritter(owner.getSubject(), this);
    }
    
    public Critter compose(CritterDump dump) throws GroupException { 
       throw new GroupException("compose from dump not supported");
    }

    public void decompose(Critter critter, CritterDump dump) {
    }
    
    public boolean isSystem() {
        return false;
    }
    
    class OwnedCritter implements Critter {
        private final AuthzSubject _owner;
        private final List _props;
        private final OwnedCritterType _type;

        public OwnedCritter(AuthzSubject owner, OwnedCritterType type) {
            super();
            _owner = owner;

            List c = new ArrayList(1);
            c.add(new SubjectCritterProp(OWNER_PROP, _owner));
            _props = Collections.unmodifiableList(c);
            
            _type = type;
        }

        public void bindSqlParams(CritterTranslationContext ctx, Query q) {
            q.setParameter(ctx.escape("subject"), _owner.getId());
        }

        public String getConfig() {
            Object[] args = { _owner.getId() };
            return _type.getInstanceConfig().format(args);
        }

        public CritterType getCritterType() {
            return _type;
        }

        public List getProps() {
            return _props;
        }

        public String getSql(CritterTranslationContext ctx, String resourceAlias) {
            return resourceAlias + ".subject_id = :@subject@";
        }

        public String getSqlJoins(CritterTranslationContext ctx,
                                  String resourceAlias) {
            return "";
        }

    }

}
