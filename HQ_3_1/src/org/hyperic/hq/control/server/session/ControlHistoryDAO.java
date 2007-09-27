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
package org.hyperic.hq.control.server.session;

import java.util.Collection;

import org.hibernate.Criteria;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.dao.HibernateDAO;

public class ControlHistoryDAO extends HibernateDAO
{
    public ControlHistoryDAO(DAOFactory f) {
        super(ControlHistory.class, f);
    }

    public ControlHistory findById(Integer id)
    {
        return (ControlHistory)super.findById(id);
    }

    void save(ControlHistory entity) {
        super.save(entity);
    }

    void remove(ControlHistory entity) {
        super.remove(entity);
    }

    ControlHistory create(AppdefEntityID entityId,
                          Integer groupId,
                          Integer batchId,
                          String subject,
                          String action,
                          String args,
                          Boolean scheduled,
                          long startTime, long endTime,
                          long dateScheduled,
                          String status,
                          String description,
                          String message)
    {
        ControlHistory h = new ControlHistory();
        h.setGroupId(groupId);
        h.setBatchId(batchId);
        h.setEntityId(entityId.getId());
        h.setEntityType(new Integer(entityId.getType()));
        h.setSubject(subject);
        h.setScheduled(scheduled);
        h.setStartTime(startTime);
        h.setEndTime(endTime);
        h.setDateScheduled(dateScheduled);
        h.setDuration(endTime - startTime);
        h.setStatus(status);
        h.setDescription(description);
        h.setAction(action);
        h.setArgs(args);
        save(h);
        return h;
    }

    public Collection findByStartTime(long time, boolean asc)
    {
        return createCriteria()
            .add(Expression.gt("startTime", new Long(time)))
            .addOrder(asc ? Order.asc("startTime") : Order.desc("startTime"))
            .list();
    }

    public Collection findByEntity(int type, int id)
    {
        return createFindByEntity(type, id).list();
    }

    public Collection findByEntityStartTime(int type, int id, boolean asc)
    {
        return createFindByEntity(type, id)
            .addOrder(asc ? Order.asc("startTime") : Order.desc("startTime"))
            .list();
    }

    public Collection findByEntityAction(int type, int id, boolean asc)
    {
        return createFindByEntity(type, id)
            .addOrder(asc ? Order.asc("action") : Order.desc("action"))
            .list();
    }

    public Collection findByEntityStatus(int type, int id, boolean asc)
    {
        return createFindByEntity(type, id)
            .addOrder(asc ? Order.asc("status") : Order.desc("status"))
            .list();
    }

    public Collection findByEntityDuration(int type, int id, boolean asc)
    {
        return createFindByEntity(type, id)
            .addOrder(asc ? Order.asc("duration") : Order.desc("duration"))
            .list();
    }

    public Collection findByEntityDateScheduled(int type, int id, boolean asc)
    {
        return createFindByEntity(type, id)
            .addOrder(asc
                      ? Order.asc("dateScheduled")
                      : Order.desc("dateScheduled"))
            .list();
    }

    public Collection findByGroupStartTime(int groupId,int batchId, boolean asc)
    {
        return createFindByGroup(groupId, batchId)
            .addOrder(asc
                      ? Order.asc("startTime")
                      : Order.desc("startTime"))
            .list();
    }

    public Collection findByGroupAction(int groupId, int batchId, boolean asc)
    {
        return createFindByGroup(groupId, batchId)
            .addOrder(asc
                      ? Order.asc("action")
                      : Order.desc("action"))
            .list();
    }

    public Collection findByGroupStatus(int groupId, int batchId, boolean asc)
    {
        return createFindByGroup(groupId, batchId)
            .addOrder(asc
                      ? Order.asc("status")
                      : Order.desc("status"))
            .list();
    }

    public Collection findByGroupDuration(int groupId, int batchId, boolean asc)
    {
        return createFindByGroup(groupId, batchId)
            .addOrder(asc
                      ? Order.asc("duration")
                      : Order.desc("duration"))
            .list();
    }

    public Collection findByGroupDateScheduled(int groupId, int batchId,
                                               boolean asc)
    {
        return createFindByGroup(groupId, batchId)
            .addOrder(asc
                      ? Order.asc("dateScheduled")
                      : Order.desc("dateScheduled"))
            .list();
    }

    private Criteria createFindByEntity(int type, int id)
    {
        return createCriteria()
            .add(Expression.eq("entityType", new Integer(type)))
            .add(Expression.eq("entityId", new Integer(id)));
    }

    private Criteria createFindByGroup(int groupId, int batchId)
    {
        return createCriteria()
            .add(Expression.eq("groupId", new Integer(groupId)))
            .add(Expression.eq("batchId", new Integer(batchId)));
    }
}
