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
import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterDump;
import org.hyperic.hq.grouping.CritterTranslationContext;
import org.hyperic.hq.grouping.CritterType;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.prop.CritterPropType;
import org.hyperic.hq.grouping.prop.StringCritterProp;

/**
 * This type of criteria is able to match resources if their prototype matches
 * the passed name.
 * 
 * "Show me all resources of type 'Nagios.*'"
 */
public class ProtoNameCritterType extends BaseCritterType {
    private static final String PROP_NAME = "name";

    public ProtoNameCritterType() {
        initialize("org.hyperic.hq.grouping.Resources", "protoName");
        addPropDescription(PROP_NAME, CritterPropType.STRING);
    }

    public ProtoNameCritter newInstance(String name) throws GroupException {
        return new ProtoNameCritter(name, this);
    }

    public Critter newInstance(List critterProps) throws GroupException {
        validate(critterProps);

        StringCritterProp c = (StringCritterProp) critterProps.get(0);
        return new ProtoNameCritter(c.getString(), this);
    }

    public Critter compose(CritterDump dump) throws GroupException {
        return newInstance(dump.getStringProp());
    }

    public void decompose(Critter critter, CritterDump dump)
        throws GroupException {
        // verify that critter is of the right type
        if (!(critter instanceof ProtoNameCritter))
            throw new GroupException(
                "Critter is not of valid type ProtoNameCritter");

        ProtoNameCritter protoCritter = (ProtoNameCritter) critter;
        dump.setStringProp(protoCritter.getNameRegex());
    }

    public boolean isSystem() {
        return false;
    }

    class ProtoNameCritter implements Critter {
        private final String _nameRegex;
        private final List _props;
        private final ProtoNameCritterType _type;

        ProtoNameCritter(String nameRegex, ProtoNameCritterType type) {
            _nameRegex = nameRegex;

            List c = new ArrayList(1);
            c.add(new StringCritterProp(
                type.getComponentName(PROP_NAME), _nameRegex));
            _props = Collections.unmodifiableList(c);
            _type = type;
        }

        public List getProps() {
            return _props;
        }

        public String getSql(CritterTranslationContext ctx, String resourceAlias) {
            return ctx.getHQDialect().getRegExSQL("@proto@.name",
                                                  ":@protoName@", false, false);
        }

        public String getSqlJoins(CritterTranslationContext ctx,
            String resourceAlias) {
            return new StringBuilder()
                .append("join EAM_RESOURCE @proto@ on ")
                .append(resourceAlias)
                .append(".proto_id = @proto@.id").toString();
        }

        public void bindSqlParams(CritterTranslationContext ctx, Query q) {
            q.setParameter(ctx.escape("protoName"), _nameRegex);
        }

        public CritterType getCritterType() {
            return _type;
        }

        public String getNameRegex() {
            return _nameRegex;
        }

        public String getConfig() {
            Object[] args = { _nameRegex };
            return _type.getInstanceConfig().format(args);
        }

        public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ProtoNameCritter)) return false;
        
        // make assumptions explicit
        assert _nameRegex != null;
        
        ProtoNameCritter critter = (ProtoNameCritter) other;
        if (!_nameRegex.equals(critter._nameRegex)) return false;
        return true;
    }

        public int hashCode() {
            int result = _nameRegex != null ? _nameRegex.hashCode() : 0;
            return result;
        }
    }
}
