/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.product.server.session;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.hyperic.hq.plugin.domain.Plugin;
import org.springframework.stereotype.Repository;



@Repository
public class PluginDAO {

    @PersistenceContext
    private EntityManager entityManager;

    
    public PluginDAO() {
       
    }

    public Plugin create(String name, String path, String md5) {
        Plugin p = new Plugin();
        p.setName(name);
        p.setPath(path);
        p.setMD5(md5);
        entityManager.persist(p);
        return p;
    }

    public Plugin findByName(String name) {
        if (name == null) return null;       
        Query q = entityManager.createQuery("SELECT p FROM Plugin p WHERE p.name = :name",Plugin.class);
        q.setParameter("name", name);
        
        List<Plugin> resultList = q.getResultList();
        if (resultList.size() > 0)
        {
            final Plugin plugin = (Plugin) resultList.get(0);
            if (plugin != null) {
                plugin.getId();
            }
            return plugin;
        } 
        return null;
    }
    
    public EntityManager getEntityManager() {
        return entityManager;
    }
}
