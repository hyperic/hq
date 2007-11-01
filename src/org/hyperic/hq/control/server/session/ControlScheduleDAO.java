package org.hyperic.hq.control.server.session;

import java.io.IOException;
import java.util.Collection;

import org.hibernate.Criteria;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.scheduler.ScheduleValue;

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

public class ControlScheduleDAO extends HibernateDAO
{
    public ControlScheduleDAO(DAOFactory f) {
        super(ControlSchedule.class, f);
    }

    public ControlSchedule findById(Integer id)
    {
        return (ControlSchedule)super.findById(id);
    }

    void save(ControlSchedule entity)
    {
        super.save(entity);
    }

    void remove(ControlSchedule entity)
    {
        super.remove(entity);
    }

    ControlSchedule create(AppdefEntityID entityId,
                           String subject,
                           String action,
                           ScheduleValue schedule,
                           long nextFire, String triggerName,
                           String jobName,
                           String jobOrderData)
    {
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


    public Collection findByFireTime(boolean asc)
    {
        return createCriteria()
            .addOrder(asc
                      ? Order.asc("nextFireTime")
                      : Order.desc("nextFireTime"))
            .list();
    }

    public Collection findByEntity(int type, int id)
    {
        return createFindByEntity(type, id).list();
    }

    public Collection findByEntityAction(int type, int id, boolean asc)
    {
        return
            createFindByEntity(type, id)
                .addOrder(asc
                          ? Order.asc("action")
                          : Order.desc("action"))
                .list();
    }

    public Collection findByEntityFireTime(int type, int id, boolean asc)
    {
        return
            createFindByEntity(type, id)
                .addOrder(asc
                          ? Order.asc("nextFireTime")
                          : Order.desc("nextFireTime"))
                .list();
    }

    private Criteria createFindByEntity(int type, int id)
    {
        return createCriteria()
            .add(Expression.eq("entityId", new Integer(id)))
            .add(Expression.eq("entityType", new Integer(type)));
    }
}
