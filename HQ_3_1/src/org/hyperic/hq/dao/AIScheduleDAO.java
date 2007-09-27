package org.hyperic.hq.dao;

import java.io.IOException;
import java.util.Collection;

import javax.ejb.CreateException;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.autoinventory.AISchedule;
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

public class AIScheduleDAO extends HibernateDAO
{
    public AIScheduleDAO(DAOFactory f) {
        super(AISchedule.class, f);
    }

    public AISchedule findById(Integer id)
    {
        return (AISchedule)super.findById(id);
    }

    public void save(AISchedule entity)
    {
        super.save(entity);
    }

    public void remove(AISchedule entity)
    {
        super.remove(entity);
    }

    public AISchedule create(AppdefEntityID entityId,
                             String subject,
                             String scanName,
                             String scanDesc,
                             ScheduleValue schedule,
                             long nextFire, String triggerName,
                             String jobName)
        throws CreateException
    {
        try {
            AISchedule s = new AISchedule();
            s.setEntityId(entityId.getId());
            s.setEntityType(new Integer(entityId.getType()));
            s.setSubject(subject);
            s.setScheduleValue(schedule);
            s.setNextFireTime(nextFire);
            s.setTriggerName(triggerName);
            s.setJobName(jobName);
            s.setJobOrderData(null);
            s.setScanName(scanName);
            s.setScanDesc(scanDesc);
            save(s);
            return s;
        } catch (IOException e) {
            throw new CreateException(e.getMessage());
        }
    }

    public AISchedule findByScanName(String name)
    {
        String sql="from AISchedule where scanName=?";
        return (AISchedule)getSession().createQuery(sql)
            .setString(0, name)
            .uniqueResult();
    }

    /**
     * @deprecated use findByEntityFireTime()
     * @param type
     * @param id
     * @return
     */
    public Collection findByEntityFireTimeDesc(int type, int id)
    {
        return findByEntityFireTime(type, id, false);
    }

    /**
     * @deprecated use findByEntityFireTime()
     * @param type
     * @param id
     * @return
     */
    public Collection findByEntityFireTimeAsc(int type, int id)
    {
        return findByEntityFireTime(type, id, true);
    }

    public Collection findByEntityFireTime(int type, int id, boolean asc) {
        String sql="from AISchedule where entityType=? and entityId=? " +
                   "order by nextFireTime " + (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .list();
    }

    public Collection findByEntityScanName(int type, int id, boolean asc) {
        String sql="from AISchedule where entityType=? and entityId=? " +
                   "order by scanName " + (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .list();
    }
}
