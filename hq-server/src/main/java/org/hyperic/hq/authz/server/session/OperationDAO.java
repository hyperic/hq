/*
us * NOTE: This copyright does *not* cover user programs that use HQ
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

package org.hyperic.hq.authz.server.session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class OperationDAO
    extends HibernateDAO<Operation> {

    private JdbcTemplate jdbcTemplate;

    private static final String OPERABLE_SQL =
    /* ex. "SELECT DISTINCT(server_type_id) FROM eam_server " + */
    " s, EAM_CONFIG_RESPONSE c, EAM_RESOURCE r, EAM_OPERATION o, "
        + "EAM_RESOURCE_TYPE t, EAM_ROLE_OPERATION_MAP ro, " + "EAM_ROLE_RESOURCE_GROUP_MAP g, "
        + "EAM_RES_GRP_RES_MAP rg "
        + "WHERE t.name = ? AND o.resource_type_id = t.id AND o.name = ? AND "
        + "operation_id = o.id AND ro.role_id = g.role_id AND "
        + "g.resource_group_id = rg.resource_group_id AND "
        + "rg.resource_id = r.id AND r.resource_type_id = t.id AND "
        + "r.instance_id = s.id AND s.config_response_id = c.id AND "
        + "c.control_response is not null AND " + "(r.subject_id = ? OR EXISTS "
        + "(SELECT * FROM EAM_SUBJECT_ROLE_MAP sr "
        + "WHERE sr.role_id = g.role_id AND subject_id = ?))";

    @Autowired
    public OperationDAO(SessionFactory f, JdbcTemplate jdbcTemplate) {
        super(Operation.class, f);
        this.jdbcTemplate = jdbcTemplate;
    }

    public Operation getByName(String name) {
        String sql = "from Operation where name = :name";
        return (Operation) getSession().createQuery(sql).setParameter("name", name).uniqueResult();
    }

    public Operation findByTypeAndName(ResourceType type, String name) {
        String sql = "from Operation where resourceType=? and name=?";

        return (Operation) getSession().createQuery(sql).setParameter(0, type).setString(1, name)
            .setCacheable(true).setCacheRegion("Operation.findByTypeAndName").uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public Collection<Integer> findViewableOperationIds(Collection<ResourceType> resourceTypes) {
        // adding this due to bug in hibernate-https://hibernate.atlassian.net/browse/HHH-2045
        // our bug-https://jira.hyperic.com/browse/HQ-4479
        if ((resourceTypes == null) || resourceTypes.isEmpty()) {
            return Collections.emptyList();
        }
        String sql = "from Operation where resourceType in (:types) and name like '+view+%'";
        sql = sql.replace("+view+", AuthzConstants.VIEW_PREFIX);
        final List<Operation> list = getSession().createQuery(sql).setParameterList("types", resourceTypes).list();
        final List<Integer> rtn = new ArrayList<Integer>(list.size());
        for (Operation o : list) {
            rtn.add(o.getId());
        }
        return rtn;
    }

    @SuppressWarnings("unchecked")
    public Collection<Operation> findByResourceType(Collection<ResourceType> resourceTypes) {
        // adding this due to bug in hibernate-https://hibernate.atlassian.net/browse/HHH-2045
        if ((resourceTypes == null) || resourceTypes.isEmpty()) {
            return Collections.emptyList();
        }
        String sql = "from Operation where resourceType in (:types)";
        return getSession().createQuery(sql).setParameterList("types", resourceTypes).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<Role> findByRole(Integer roleId) {
        String sql = "select o from Role r join r.operations o where r.id = ?";
        return getSession().createQuery(sql).setParameter(0, roleId).list();
    }

    public List<Integer> findOperableResourceIds(final AuthzSubject subj,
                                                 final String resourceTable,
                                                 final String resourceColumn, final String resType,
                                                 final String operation, final String addCond) {

        final StringBuffer sql = new StringBuffer("SELECT DISTINCT(s.").append(resourceColumn)
            .append(") FROM ").append(resourceTable).append(OPERABLE_SQL);

        if (addCond != null) {
            sql.append(" AND s.").append(addCond);
        }

        List<Integer> resTypeIds = this.jdbcTemplate.query(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement stmt = con.prepareStatement(sql.toString());
                int i = 1;
                stmt.setString(i++, resType);
                stmt.setString(i++, operation);
                stmt.setInt(i++, subj.getId().intValue());
                stmt.setInt(i++, subj.getId().intValue());
                return stmt;
            }
        }, new RowMapper<Integer>() {
            public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getInt(1);
            }
        });

        return resTypeIds;

    }

    public boolean userHasOperation(AuthzSubject subj, Operation op) {
        String hql = new StringBuilder(128).append("select 1 from Role r ").append(
            "join r.operations op ").append("join r.subjects s ").append(
            "where s = :subject and op = :operation").toString();
        return getSession().createQuery(hql)
        	.setParameter("subject", subj)
        	.setParameter("operation", op)
        	.list().size() > 0;
    }
}
