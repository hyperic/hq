/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hibernate.PersistedObject;

public class AlertActionLogDAO extends HibernateDAO {
    public AlertActionLogDAO(DAOFactory f) {
        super(AlertActionLog.class, f);
    }

    public AlertActionLog findById(Integer id) {
        return (AlertActionLog)super.findById(id);
    }

    public void save(AlertActionLog entity)
    {
        super.save(entity);
    }

    public void savePersisted(PersistedObject entity)
    {
        save((AlertActionLog)entity);
    }
    
    void handleSubjectRemoval(AuthzSubject subject) {
        String sql = "update AlertActionLog set " +
                     "subject = null " +
                     "where subject = :subject";
        
        getSession().createQuery(sql)
                    .setParameter("subject", subject)
                    .executeUpdate();
    }

}
