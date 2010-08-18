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

package org.hyperic.hq.dao;

import java.io.IOException;
import java.util.Collection;

import org.hibernate.SessionFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.autoinventory.AISchedule;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AIScheduleDAO
    extends HibernateDAO<AISchedule> {
    @Autowired
    public AIScheduleDAO(SessionFactory f) {
        super(AISchedule.class, f);
    }

    public AISchedule create(AppdefEntityID entityId, String subject, String scanName,
                             String scanDesc, ScheduleValue schedule, long nextFire,
                             String triggerName, String jobName) throws ApplicationException

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
            throw new ApplicationException(e.getMessage());
        }
    }

    public AISchedule findByScanName(String name) {
        String sql = "from AISchedule where scanName=?";
        return (AISchedule) getSession().createQuery(sql).setString(0, name).uniqueResult();
    }

    /**
     * @deprecated use findByEntityFireTime()
     * @param type
     * @param id
     * @return
     */
    public Collection findByEntityFireTimeDesc(int type, int id) {
        return findByEntityFireTime(type, id, false);
    }

    /**
     * @deprecated use findByEntityFireTime()
     * @param type
     * @param id
     * @return
     */
    public Collection findByEntityFireTimeAsc(int type, int id) {
        return findByEntityFireTime(type, id, true);
    }

    public Collection<AISchedule> findByEntityFireTime(int type, int id, boolean asc) {
        String sql = "from AISchedule where entityId=? and entityType=? " +
                     "order by nextFireTime " + (asc ? "asc" : "desc");
        return getSession().createQuery(sql).setInteger(0, id).setInteger(1, type).list();
    }

    public Collection<AISchedule> findByEntityScanName(int type, int id, boolean asc) {
        String sql = "from AISchedule where entityId=? and entityType=? " + "order by scanName " +
                     (asc ? "asc" : "desc");
        return getSession().createQuery(sql).setInteger(0, id).setInteger(1, type).list();
    }
}
