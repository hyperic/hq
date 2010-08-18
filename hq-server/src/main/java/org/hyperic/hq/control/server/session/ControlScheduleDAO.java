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

package org.hyperic.hq.control.server.session;

import java.io.IOException;
import java.util.Collection;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ControlScheduleDAO
    extends HibernateDAO<ControlSchedule> {

    @Autowired
    public ControlScheduleDAO(SessionFactory f) {
        super(ControlSchedule.class, f);
    }

    ControlSchedule create(AppdefEntityID entityId, String subject, String action,
                           ScheduleValue schedule, long nextFire, String triggerName,
                           String jobName, String jobOrderData) {
        ControlSchedule s = new ControlSchedule();

        try {
            s.setEntityId(entityId.getId());
            s.setEntityType(new Integer(entityId.getType()));
            s.setSubject(subject);
            s.setScheduleValue(schedule);
            s.setNextFireTime(nextFire);
            s.setTriggerName(triggerName);
            s.setJobName(jobName);
            s.setJobOrderData(jobOrderData);
            s.setAction(action);
            save(s);
            return s;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<ControlSchedule> findByFireTime(boolean asc) {
        return createCriteria().addOrder(
            asc ? Order.asc("nextFireTime") : Order.desc("nextFireTime")).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<ControlSchedule> findByEntity(int type, int id) {
        return createFindByEntity(type, id).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<ControlSchedule> findByEntityAction(int type, int id, boolean asc) {
        return createFindByEntity(type, id).addOrder(
            asc ? Order.asc("action") : Order.desc("action")).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<ControlSchedule> findByEntityFireTime(int type, int id, boolean asc) {
        return createFindByEntity(type, id).addOrder(
            asc ? Order.asc("nextFireTime") : Order.desc("nextFireTime")).list();
    }

    private Criteria createFindByEntity(int type, int id) {
        return createCriteria().add(Expression.eq("entityId", new Integer(id))).add(
            Expression.eq("entityType", new Integer(type)));
    }

   
}
