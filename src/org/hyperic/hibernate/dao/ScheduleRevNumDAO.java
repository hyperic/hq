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

package org.hyperic.hibernate.dao;

import org.hibernate.Session;

import org.hyperic.hq.measurement.ScheduleRevNum;
import org.hyperic.hq.measurement.SrnId;

/**
 * CRUD methods, finders, etc. for ScheduleRevNum
 */
public class ScheduleRevNumDAO extends HibernateDAO
{
    public ScheduleRevNumDAO(Session session) {
        super(ScheduleRevNum.class, session);
    }

    public ScheduleRevNum findById(SrnId id) {
        return (ScheduleRevNum)super.findById(id);
    }

    public void evict(ScheduleRevNum entity) {
        super.evict(entity);
    }

    public ScheduleRevNum merge(ScheduleRevNum entity) {
        return (ScheduleRevNum)super.merge(entity);
    }

    public void save(ScheduleRevNum entity) {
        super.save(entity);
    }

    public void remove(ScheduleRevNum entity) {
        super.remove(entity);
    }

    public ScheduleRevNum create(int entType, int entId) {
        
        SrnId srnId = new SrnId(entType, entId);
        ScheduleRevNum srn = new ScheduleRevNum();

        srn.setId(srnId);
        srn.setSrn(new Integer(1));
        save(srn);
        return srn;
    }
}
