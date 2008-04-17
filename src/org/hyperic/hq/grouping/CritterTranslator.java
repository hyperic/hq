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

import java.util.Iterator;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hyperic.hq.authz.server.session.Resource;


/**
 * The CritterTranslator is a simple class useful composing a Hibernate query
 * from a list of critters
 */
public class CritterTranslator {
    public CritterTranslator() {
    }
    
    public SQLQuery translate(Session s, CritterList cList) {
        StringBuilder sql = new StringBuilder();
        
        sql.append("select {res.*} from EAM_RESOURCE res ");
        
        for (Iterator i=cList.getCritters().iterator(); i.hasNext(); ) {
            Critter c = (Critter)i.next();
            
            sql.append(c.getSqlJoins("res"));
        }
        
        sql.append(" where ");
        for (Iterator i=cList.getCritters().iterator(); i.hasNext(); ) {
            Critter c = (Critter)i.next();
            
            sql.append(c.getSql("res"));
            if (i.hasNext()) {
                if (cList.isAll()) {
                    sql.append(" and ");
                } else {
                    sql.append(" or ");
                }
            }
        }

        SQLQuery res = s.createSQLQuery(sql.toString());
        res.addEntity("res", Resource.class);
        for (Iterator i = cList.getCritters().iterator(); i.hasNext(); ) {
            Critter c = (Critter)i.next();

            c.bindSqlParams(res);
        }
        
        return res;
    }
}
