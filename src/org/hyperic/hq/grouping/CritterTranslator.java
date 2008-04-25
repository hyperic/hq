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

package org.hyperic.hq.grouping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * The CritterTranslator is a simple class useful composing a Hibernate query
 * from a list of critters
 */
public class CritterTranslator {
    private final Log _log = LogFactory.getLog(CritterTranslator.class);
    
    public CritterTranslator() {
    }

    public PageList translate(CritterTranslationContext ctx, CritterList cList,
                              PageControl pc)
    {
        PageList rtn = new PageList();
        rtn.ensureCapacity(pc.getPagesize());
        Query query = translate(ctx, cList, false)
                        .setFirstResult(pc.getPageEntityIndex());
        if (PageControl.SIZE_UNLIMITED != pc.getPagesize()) {
            query.setMaxResults(pc.getPagesize());
        }
        rtn.addAll(query.list());
        rtn.setTotalSize(
            ((Number)translate(ctx, cList, true).uniqueResult()).intValue());
        return rtn;
    }

    public SQLQuery translate(CritterTranslationContext ctx, CritterList cList)
    {
        return translate(ctx,cList,false);
    }
    
    public SQLQuery translate(CritterTranslationContext ctx, CritterList cList,
                              boolean issueCount)
    {
        StringBuilder sql = new StringBuilder();
        Map txContexts = new HashMap(cList.getCritters().size());
        
        if (issueCount) {
            sql.append("select count(1) from EAM_RESOURCE res ");
        } else {
            sql.append("select {res.*} from EAM_RESOURCE res ");
        }
        
        int prefixCnt = 0;
        for (Iterator i=cList.getCritters().iterator(); i.hasNext(); ) {
            Critter c = (Critter)i.next();
            String prefix = "x" + (prefixCnt++);
            CritterTranslationContext critterCtx = 
                new CritterTranslationContext(ctx.getSession(), 
                                              ctx.getHQDialect(), prefix);
            txContexts.put(c, critterCtx);
            sql.append(critterCtx.escapeSql(c.getSqlJoins(critterCtx, "res")));
            sql.append(" ");
        }

        sql.append(" where ");

        List systemCritters = new ArrayList();
        List regularCritters = new ArrayList();
        
        for (Iterator i=cList.getCritters().iterator(); i.hasNext(); ) {
            Critter c = (Critter)i.next();
            
            if (c.getCritterType().isSystem())
                systemCritters.add(c);
            else
                regularCritters.add(c);
        }

        if (!regularCritters.isEmpty()) {
            sql.append(" (");
        
            for (Iterator i=regularCritters.iterator(); i.hasNext(); ) {
                Critter c = (Critter)i.next();
            
                CritterTranslationContext critterCtx = 
                    (CritterTranslationContext)txContexts.get(c);
                sql.append(critterCtx.escapeSql(c.getSql(critterCtx, "res"))); 
            
                if (i.hasNext()) {
                    if (cList.isAll()) {
                        sql.append(" and ");
                    } else {
                        sql.append(" or ");
                    }
                }
            }
            sql.append(") ");
        }
        
        if (!systemCritters.isEmpty()) {
            if (!regularCritters.isEmpty()) {
                sql.append(" and ");
            }
            sql.append(" (");
            for (Iterator i=systemCritters.iterator(); i.hasNext(); ) {
                Critter c = (Critter)i.next();
            
                CritterTranslationContext critterCtx = 
                    (CritterTranslationContext)txContexts.get(c);
                sql.append(critterCtx.escapeSql(c.getSql(critterCtx, "res"))); 
                
                if (i.hasNext()) {
                    sql.append(" and ");
                }
            }
            sql.append(") ");
        }

        if (!issueCount) {
            sql.append(" order by res.name");
        }

        if (_log.isDebugEnabled())
            _log.debug("Created SQL: [" + sql + "]");
        
        SQLQuery res = ctx.getSession().createSQLQuery(sql.toString());
        
        if (_log.isDebugEnabled())
            _log.debug("Translated into: [" + res.getQueryString() + "]");
        
        if (!issueCount) {
            res.addEntity("res", Resource.class);
        }
        for (Iterator i = cList.getCritters().iterator(); i.hasNext(); ) {
            Critter c = (Critter)i.next();

            c.bindSqlParams((CritterTranslationContext)txContexts.get(c), res);
        }
        
        return res;
    }
}
