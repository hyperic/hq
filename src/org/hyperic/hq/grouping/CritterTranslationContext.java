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

package org.hyperic.hq.grouping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hyperic.hibernate.Util;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.util.StringUtil;

/**
 * This context is used to provide additional information to {@link Critter}s 
 * which they may use to generate more specific SQL (i.e. based on the 
 * executing user, or more specific SQL from the dialect)
 * 
 * The context also provides utility methods for getting unique strings for
 * SQL aliases or Hibernate bindings -- used to prevent collisions between
 * criteria which use the same variable names.
 */
public class CritterTranslationContext {
    private final Session   _session;
    private final HQDialect _dialect;
    private final String    _prefix;
    
    public CritterTranslationContext() {
        _session = Util.getSessionFactory().getCurrentSession();
        _dialect = Util.getHQDialect();
        _prefix  = "x";
    }

    public CritterTranslationContext(Session s, HQDialect d) {
        _session = s;
        _dialect = d;
        _prefix  = "x";
    }
    
    public CritterTranslationContext(Session s, HQDialect d, String prefix) {
        _session = s;
        _dialect = d;
        _prefix  = prefix;
    }
    
    public Session getSession() {
        return _session;
    }
    
    public HQDialect getDialect() {
        return _dialect;
    }

    public String escapeSql(String sql) {
        String[] split = sql.split("@");
        List toJoin = new ArrayList();

        for (int i=0; i<split.length; i++) {
            if ((i % 2) == 1) {
                toJoin.add(escape(split[i]));
            } else {
                toJoin.add(split[i]);
            }
        }
        return StringUtil.implode(toJoin, "");
    }
    
    public String escape(String var) {
        return _prefix + var;
    }
}
