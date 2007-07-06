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

import java.util.Collection;
import java.util.List;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.dao.HibernateDAO;

class GalertDefDAO
    extends HibernateDAO
{
    GalertDefDAO(DAOFactory f) {
        super(GalertDef.class, f);
    }

    GalertDef findById(Integer id) {
        return (GalertDef)super.findById(id);
    }

    void save(GalertDef def) {
        super.save(def);
    }

    void remove(GalertDef def) {
        super.remove(def);
    }
    
    void remove(GtriggerInfo t) {
        super.remove(t);
    }

    void save(GtriggerInfo t) {
        super.save(t);
    }

    Collection findAbsolutelyAllGalertDefs() {
        return super.findAll(); 
    }
    
    Collection findAbsolutelyAllGalertDefs(ResourceGroup g) {
        String sql = "from GalertDef d where d.group = :group"; 

        return getSession().createQuery(sql)
            .setParameter("group", g)
            .list();
    }

    /**
     * Finds all the galert defs which have not been marked for deletion.
     * Typically this is what people want to use.
     */
    public Collection findAll() {
        return getSession().createQuery("from GalertDef d " + 
                                        "where d.deleted = false " +
                                        "order by name").list();
    }

    Collection findAll(ResourceGroup g) {
        String sql = "from GalertDef d where d.group = :group " + 
                     "and d.deleted = false order by name";
        
        return getSession().createQuery(sql)
            .setParameter("group", g)
            .list();
    }
    
    int countByStrategy(ExecutionStrategyTypeInfo strat) {
        String sql = "select count(*) from GalertDef d " +
            "where d.strategyInfo.type = :type";
        
        return ((Integer)getSession().createQuery(sql)
            .setParameter("type", strat)
            .uniqueResult()).intValue();
    }
}
