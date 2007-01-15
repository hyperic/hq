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

package org.hyperic.hq.galerts.server.session;

import java.util.List;

import org.hibernate.Query;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

class GalertLogDAO
    extends HibernateDAO
{
    GalertLogDAO(DAOFactory f) {
        super(GalertLog.class, f);
    }

    GalertLog findById(Integer id) {
        return (GalertLog)super.findById(id);
    }

    void save(GalertLog log) {
        super.save(log);
    }

    void remove(GalertLog log) {
        super.remove(log);
    }
    
    List findAll(ResourceGroup g) {
        String sql = "from GalertLog l where l.alertDef.group = :group " + 
                     "order by l.timestamp";
        
        return getSession().createQuery(sql)
            .setParameter("group", g)
            .list();
    }

    PageList findByTimeWindow(ResourceGroup g, long begin, PageControl pc) {
        String sql = "from GalertLog l " +
                     "where l.alertDef.group = :group and l.timestamp > :time "; 
                     
        Integer count = (Integer)
            getSession().createQuery("select count(*) " + sql)
                        .setParameter("group", g)
                        .setLong("time", begin)
                        .uniqueResult();

        if (count.intValue() > 0) {
            Query q = getSession()
                .createQuery(sql + "order by l.timestamp " +
                             (pc.isDescending() ? "desc" : "asc"))
                .setParameter("group", g)
                .setLong("time", begin);
            
            return getPagedResult(q, count, pc);
        }

        return new PageList();
    }

    void removeAll(ResourceGroup g) {
        String sql = "delete from GalertLog l where l.alertDef.group = :group";
        
        getSession().createQuery(sql)
            .setParameter("group", g)
            .executeUpdate();
    }
    
    void removeAll(GalertDef d) {
        String sql = "delete from GalertLog l where l.alertDef = :def";
        
        getSession().createQuery(sql)
                    .setParameter("def", d)
                    .executeUpdate();
    }
}
