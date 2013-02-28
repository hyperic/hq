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

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.util.jdbc.DBUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ControlHistoryDAO
    extends HibernateDAO<ControlHistory> {
    
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    public ControlHistoryDAO(SessionFactory f, JdbcTemplate jdbcTemplate) {
        super(ControlHistory.class, f);
        this.jdbcTemplate = jdbcTemplate;
    }

    ControlHistory create(AppdefEntityID entityId, Integer groupId, Integer batchId,
                          String subject, String action, String args, Boolean scheduled,
                          long startTime, long endTime, long dateScheduled, String status,
                          String description, String message) {
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
        h.setStatus(status);
        h.setDescription(description);
        h.setAction(action);
        h.setArgs(args);
        h.setMessage(message);
        save(h);
        return h;
    }
   
    public ControlHistory findByIdAndPopulate(Serializable id) {
        ControlHistory controlHistory = findById(id);
        //Fully initialize the object for consumers without a Hibernate session
        Hibernate.initialize(controlHistory);
        return controlHistory;
    }

    public Collection<ControlHistory> findByStartTime(long time, boolean asc) {
        return createCriteria().add(Expression.gt("startTime", new Long(time))).addOrder(
            asc ? Order.asc("startTime") : Order.desc("startTime")).list();
    }

    public Collection<ControlHistory> findByEntity(int type, int id) {
        return createFindByEntity(type, id).list();
    }

    public Collection<ControlHistory> findByEntityStartTime(int type, int id, boolean asc) {
        return createFindByEntity(type, id).addOrder(
            asc ? Order.asc("startTime") : Order.desc("startTime")).list();
    }

    public Collection<ControlHistory> findByEntityAction(int type, int id, boolean asc) {
        return createFindByEntity(type, id).addOrder(
            asc ? Order.asc("action") : Order.desc("action")).list();
    }

    public Collection<ControlHistory> findByEntityStatus(int type, int id, boolean asc) {
        return createFindByEntity(type, id).addOrder(
            asc ? Order.asc("status") : Order.desc("status")).list();
    }

    public Collection<ControlHistory> findByEntityDuration(int type, int id, boolean asc) {
        return createFindByEntity(type, id).addOrder(
            asc ? Order.asc("duration") : Order.desc("duration")).list();
    }

    public Collection<ControlHistory> findByEntityDateScheduled(int type, int id, boolean asc) {
        return createFindByEntity(type, id).addOrder(
            asc ? Order.asc("dateScheduled") : Order.desc("dateScheduled")).list();
    }

    public Collection<ControlHistory> findByGroupStartTime(int groupId, int batchId, boolean asc) {
        return createFindByGroup(groupId, batchId).addOrder(
            asc ? Order.asc("startTime") : Order.desc("startTime")).list();
    }

    public Collection<ControlHistory> findByGroupAction(int groupId, int batchId, boolean asc) {
        return createFindByGroup(groupId, batchId).addOrder(
            asc ? Order.asc("action") : Order.desc("action")).list();
    }

    public Collection<ControlHistory> findByGroupStatus(int groupId, int batchId, boolean asc) {
        return createFindByGroup(groupId, batchId).addOrder(
            asc ? Order.asc("status") : Order.desc("status")).list();
    }

    public Collection<ControlHistory> findByGroupDuration(int groupId, int batchId, boolean asc) {
        return createFindByGroup(groupId, batchId).addOrder(
            asc ? Order.asc("duration") : Order.desc("duration")).list();
    }

    public Collection<ControlHistory> findByGroupDateScheduled(int groupId, int batchId, boolean asc) {
        return createFindByGroup(groupId, batchId).addOrder(
            asc ? Order.asc("dateScheduled") : Order.desc("dateScheduled")).list();
    }
    
    public List<ControlFrequency> getControlFrequencies(int numToReturn) throws SQLException {
        String sqlStr = "SELECT entity_type, entity_id, action, COUNT(id) AS num " +
                        "FROM EAM_CONTROL_HISTORY " + "WHERE scheduled = " +
                         DBUtil.getBooleanValue(false, jdbcTemplate.getDataSource().getConnection()) +
                        " GROUP BY entity_type, entity_id, action " + "ORDER by num DESC ";

        List<ControlFrequency> frequencies = new ArrayList<ControlFrequency>();
        List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(sqlStr);

        for (int i = 0; i < numToReturn && i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            final AppdefEntityID id = new AppdefEntityID(((Number) row.get("entity_type")).intValue(),
                ((Number) row.get("entity_id")).intValue());
            frequencies.add(new ControlFrequency(id,(String)row.get("action"),((Number) row.get("num")).longValue()));
        }
        return frequencies;
    }
    
    public void removeByEntity(int type, int id) {
        final String hql = new StringBuilder().append(
                "delete from ControlHistory ch where ch.entityId = :id and ch.entityType = :type").toString();
        getSession().createQuery(hql).setInteger("id", id).setInteger("type", type).executeUpdate();
    }
    
    private Criteria createFindByEntity(int type, int id) {
        return createCriteria().add(Expression.eq("entityType", new Integer(type))).add(
            Expression.eq("entityId", new Integer(id)));
    }

    private Criteria createFindByGroup(int groupId, int batchId) {
        return createCriteria().add(Expression.eq("groupId", new Integer(groupId))).add(
            Expression.eq("batchId", new Integer(batchId)));
    }

}
