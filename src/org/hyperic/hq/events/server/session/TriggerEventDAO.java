/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

import java.util.ArrayList;
import java.util.Iterator;
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
        
    void save(TriggerEvent event) {                
        super.save(event);
    }
    
    TriggerEvent findById(Long id) {
        return (TriggerEvent) super.findById(id);            
    }
    
    TriggerEvent get(Long id) {
        return (TriggerEvent) super.get(id);
    }
    
    List findUnexpiredByTriggerId(Integer tid, Session session) {
        String hql = "from TriggerEvent te where te.triggerId= :tid and " +
                      "te.expiration > :exp order by te.ctime";
        
        return session.createQuery(hql)
                        .setInteger("tid", tid.intValue())
                        .setLong("exp", System.currentTimeMillis())
                        .list();            
    }
    
    List findExpiredByTriggerId(Integer tid) {
    	String hql = "from TriggerEvent te where te.triggerId= :tid and " +
    				 "te.expiration < :exp order by te.ctime";

    	return createQuery(hql)
    				  .setInteger("tid", tid.intValue())
    				  .setLong("exp", System.currentTimeMillis())
    				  .list();            
    }
    
    List findAllByTriggerId(Integer tid) {
    	String hql = "select te.id from TriggerEvent te where " +
    					"te.triggerId= :tid";

        List list = createQuery(hql)
        				.setInteger("tid", tid.intValue())
        				.list();
        List rtn = new ArrayList(list.size());
        for (Iterator it = list.iterator(); it.hasNext(); ) {
        	Long id = (Long) it.next();
        	rtn.add(findById(id));
        }

        return rtn;
    }
    
    int countUnexpiredByTriggerId(Integer tid, Session session) {
        String hql = "select count(te) from TriggerEvent te " +
                     "where te.triggerId= :tid and te.expiration > :exp";
        
        return ((Number) session.createQuery(hql)
                        .setInteger("tid", tid.intValue())
                        .setLong("exp", System.currentTimeMillis())
                        .uniqueResult()).intValue();            
    }
    
    void deleteById(Long teid) {
    	remove(findById(teid));
    }
    
    void delete(TriggerEvent te) {
    	remove(te);
    }
    
    void deleteByAlertDefinition(AlertDefinition def) {
        String hql = "delete from TriggerEvent te where te.triggerId in " +
                     "(select r.id from RegisteredTrigger r " +
                     "where r.alertDefinition = :def)";

        createQuery(hql)
                .setParameter("def", def)
                .executeUpdate();
    }
}
