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

package org.hyperic.hq.events.server.session;

import java.util.List;

import org.hibernate.Session;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.dao.HibernateDAOFactory;


public class TriggerEventDAO extends HibernateDAO {

    public TriggerEventDAO(DAOFactory f) {
        super(TriggerEvent.class, f);
    }
    
    public Session getNewSession() {
        return HibernateDAOFactory.getInstance()
                .getSessionFactory().openSession();
    }
        
    void save(TriggerEvent event, Session session) {                
        session.saveOrUpdate(event);
    }
    
    TriggerEvent findById(Long id, Session session) {
        return (TriggerEvent)session.load(getPersistentClass(), id);            
    }
    
    List findUnexpiredByTriggerId(Integer tid, Session session) {
        String hql = "from TriggerEvent te where te.triggerId= :tid and " +
                      "te.expiration > :exp order by te.ctime";
        
        return session.createQuery(hql)
                        .setInteger("tid", tid.intValue())
                        .setLong("exp", System.currentTimeMillis())
                        .list();            
    }
    
    void deleteByTriggerId(Integer tid, Session session) {
        String hql = "delete from TriggerEvent te where te.triggerId= :tid";

        session.createQuery(hql)
                .setInteger("tid", tid.intValue())
                .executeUpdate();
    }

    void deleteExpired(Session session) {
        String hql = "delete from TriggerEvent te where te.expiration < :exp";
        
        session.createQuery(hql)
                .setLong("exp", System.currentTimeMillis())
                .executeUpdate();
    } 
    
}
