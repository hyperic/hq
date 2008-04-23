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
import org.hyperic.hq.grouping.CritterTranslationContext;
import org.hyperic.hq.grouping.CritterType;
import org.hyperic.hq.grouping.prop.StringCritterProp;

class ProtoNameCritter
    implements Critter
{
    private final String               _nameRegex;
    private final List                 _props;
    private final ProtoNameCritterType _type;
    
    ProtoNameCritter(String nameRegex, ProtoNameCritterType type) {
        _nameRegex = nameRegex;
        
        List c = new ArrayList(1);
        c.add(new StringCritterProp(_nameRegex));
        _props = Collections.unmodifiableList(c);
        _type  = type;
    }
    
    public List getProps() {
        return _props;
    }
    
    public String getSql(CritterTranslationContext ctx, String resourceAlias) {
        return ctx.getHQDialect().getRegExSQL("@proto@.name", ":@protoName@", 
                                            false, false); 
    }
    
    public String getSqlJoins(CritterTranslationContext ctx, 
                              String resourceAlias) 
    {
        return "join EAM_RESOURCE @proto@ on " + 
            resourceAlias + ".proto_id = @proto@.id"; 
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
        Object[] args = {_nameRegex};
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
