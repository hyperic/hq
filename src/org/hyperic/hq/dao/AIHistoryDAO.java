package org.hyperic.hq.dao;

import java.util.Collection;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.autoinventory.AIHistory;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;

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

public class AIHistoryDAO extends HibernateDAO
{
    public AIHistoryDAO(DAOFactory f) {
        super(AIHistory.class, f);
    }

    public AIHistory findById(Integer id) {
        return (AIHistory)super.findById(id);
    }

    public void save(AIHistory entity) {
        super.save(entity);
    }

    public void remove(AIHistory entity) {
        super.remove(entity);
    }

    public AIHistory create(AppdefEntityID entityId,
                            Integer groupId,
                            Integer batchId,
                            String subject,
                            ScanConfigurationCore config,
                            String scanName,
                            String scanDesc,
                            Boolean scheduled,
                            long startTime, long endTime,
                            long dateScheduled,
                            String status,
                            String description,
                            String message) throws AutoinventoryException {
        AIHistory h = new AIHistory();

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
        if (message != null) {
            h.setMessage(message);
        }
        h.setConfigObj(config);
        h.setScanName(scanName);
        h.setScanDesc(scanDesc);
        save(h);
        return h;
    }

    public Collection findByEntity(int type, int id)
    {
        String sql="from AIHistory where entityType=? and entityId=?";
        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .list();
    }

    /**
     * @deprecated use findByEntityStatus()
     * @return
     */
    public Collection findByEntityStartTimeDesc(int type, int id)
    {
        return findByEntityStartTime(type, id, true);
    }

    /**
     * @deprecated use findByEntityStatus()
     * @return
     */
    public Collection findByEntityStartTimeAsc(int type, int id)
    {
        return findByEntityStartTime(type, id, true);
    }

    public Collection findByEntityStartTime(int type, int id, boolean asc)
    {
        String sql="from AIHistory where entityType=? and entityId=? " +
                   "order by startTime " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .list();
    }

    /**
     * @deprecated use findByEntityStatus()
     * @return
     */
    public Collection findByEntityStatusDesc(int type, int id)
    {
        return findByEntityStatus(type, id, true);
    }

    /**
     * @deprecated use findByEntityStatus()
     * @return
     */
    public Collection findByEntityStatusAsc(int type, int id)
    {
        return findByEntityStatus(type, id, true);
    }

    public Collection findByEntityStatus(int type, int id, boolean asc)
    {
        String sql="from AIHistory where entityType=? and entityId=? " +
                   "order by status " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .list();
    }

    /**
     * @deprecated use findByEntityDuration()
     * @return
     */
    public Collection findByEntityDurationDesc(int type, int id)
    {
        return findByEntityDuration(type, id, true);
    }

    /**
     * @deprecated use findByEntityDuration()
     * @return
     */
    public Collection findByEntityDurationAsc(int type, int id)
    {
        return findByEntityDuration(type, id, true);
    }

    public Collection findByEntityDuration(int type, int id, boolean asc)
    {
        String sql="from AIHistory where entityType=? and entityId=? " +
                   "order by duration " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .list();
    }

    /**
     * @deprecated use findByEntityDateScheduled()
     * @return
     */
    public Collection findByEntityDateScheduledDesc(int type, int id)
    {
        return findByEntityDateScheduled(type, id, true);
    }

    /**
     * @deprecated use findByEntityDateScheduled()
     * @return
     */
    public Collection findByEntityDateScheduledAsc(int type, int id)
    {
        return findByEntityDateScheduled(type, id, true);
    }

    public Collection findByEntityDateScheduled(int type, int id, boolean asc)
    {
        String sql="from AIHistory where entityType=? and entityId=? " +
                   "order by dateScheduled " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .list();
    }

    /**
     * @deprecated use findByGroupStartTime()
     * @param groupId
     * @param batchId
     * @return
     */
    public Collection findByGroupStartTimeDesc(int groupId, int batchId)
    {
        return findByGroupStartTime(groupId, batchId, true);
    }

    /**
     * @deprecated use findByGroupStartTime()
     * @param groupId
     * @param batchId
     * @return
     */
    public Collection findByGroupStartTimeAsc(int groupId, int batchId)
    {
        return findByGroupStartTime(groupId, batchId, true);
    }

    public Collection findByGroupStartTime(int groupId, int batchId, boolean asc)
    {
        String sql="from AIHistory where groupId=? and batchId=? " +
                   "order by startTime " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, groupId)
            .setInteger(1, batchId)
            .list();
    }

    /**
     * @deprecated use findByGroupStatus()
     * @param groupId
     * @param batchId
     * @return
     */
    public Collection findByGroupStatusDesc(int groupId, int batchId)
    {
        return findByGroupStatus(groupId, batchId, true);
    }

    /**
     * @deprecated use findByGroupStatus()
     * @param groupId
     * @param batchId
     * @return
     */
    public Collection findByGroupStatusAsc(int groupId, int batchId)
    {
        return findByGroupStatus(groupId, batchId, true);
    }

    public Collection findByGroupStatus(int groupId, int batchId, boolean asc)
    {
        String sql="from AIHistory where groupId=? and batchId=? " +
                   "order by status " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, groupId)
            .setInteger(1, batchId)
            .list();
    }

    /**
     * @deprecated use findByGroupDuration()
     * @param groupId
     * @param batchId
     * @return
     */
    public Collection findByGroupDurationDesc(int groupId, int batchId)
    {
        return findByGroupDuration(groupId, batchId, false);
    }

    /**
     * @deprecated use findByGroupDuration()
     * @param groupId
     * @param batchId
     * @return
     */
    public Collection findByGroupDurationAsc(int groupId, int batchId)
    {
        return findByGroupDuration(groupId, batchId, true);
    }

    public Collection findByGroupDuration(int groupId, int batchId, boolean asc)
    {
        String sql="from AIHistory where groupId=? and batchId=? " +
                   "order by duration " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, groupId)
            .setInteger(1, batchId)
            .list();
    }

    /**
     * @deprecated use findByGroupDateScheduled()
     * @param groupId
     * @param batchId
     * @return
     */
    public Collection findByGroupDateScheduledDesc(int groupId, int batchId)
    {
        return findByGroupDateScheduled(groupId, batchId, false);
    }

    /**
     * @deprecated use findByGroupDateScheduled()
     * @param groupId
     * @param batchId
     * @return
     */
    public Collection findByGroupDateScheduledAsc(int groupId, int batchId)
    {
        return findByGroupDateScheduled(groupId, batchId, true);
    }

    public Collection findByGroupDateScheduled(int groupId,
                                               int batchId,
                                               boolean asc)
    {
        String sql="from AIHistory where groupId=? and batchId=? " +
                   "order by dateScheduled " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, groupId)
            .setInteger(1, batchId)
            .list();
    }
}
