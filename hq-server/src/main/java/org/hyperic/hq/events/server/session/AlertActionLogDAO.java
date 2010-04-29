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

import java.util.Collection;

import org.hibernate.SessionFactory;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AlertActionLogDAO
    extends HibernateDAO<AlertActionLog> {
    @Autowired
    public AlertActionLogDAO(SessionFactory f) {
        super(AlertActionLog.class, f);
    }

    public void savePersisted(PersistedObject entity) {
        save((AlertActionLog) entity);
    }

    /**
     * @param alerts {@link Collection} of {@link Alert}s
     */
    void deleteAlertActions(Collection alerts) {
        if (alerts.size() == 0) {
            return;
        }
        final String hql = new StringBuilder().append(
            "delete from AlertActionLog where alert in (:alerts)").toString();
        getSession().createQuery(hql).setParameterList("alerts", alerts).executeUpdate();
    }

    void handleSubjectRemoval(AuthzSubject subject) {
        String sql = "update AlertActionLog set " + "subject = null " + "where subject = :subject";

        getSession().createQuery(sql).setParameter("subject", subject).executeUpdate();
    }

}
