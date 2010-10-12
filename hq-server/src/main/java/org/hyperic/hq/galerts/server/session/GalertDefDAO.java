/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.galerts.server.session;

import java.util.Collection;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.AlertSeverity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class GalertDefDAO
    extends HibernateDAO<GalertDef> {

    @Autowired
    public GalertDefDAO(SessionFactory sessionFactory) {
        super(GalertDef.class, sessionFactory);
    }

    void remove(GtriggerInfo t) {
        getSession().delete(t);
    }

    void save(GtriggerInfo t) {
        getSession().saveOrUpdate(t);
    }

    Collection<GalertDef> findAbsolutelyAllGalertDefs() {
        return super.findAll();
    }

    @SuppressWarnings("unchecked")
    Collection<GalertDef> findAbsolutelyAllGalertDefs(ResourceGroup g) {
        String sql = "from GalertDef d where d.group = :group";

        return (Collection<GalertDef>) getSession().createQuery(sql).setParameter("group", g)
            .list();
    }

    /**
     * Finds all the galert defs which have not been marked for deletion.
     * Typically this is what people want to use.
     */
    @SuppressWarnings("unchecked")
    public List<GalertDef> findAll() {
    	String sql = "from GalertDef d "
    		+ "where d.deleted = false "
    		+ "and d.group.resource.resourceType is not null "
    		+ "order by d.name";

        return (List<GalertDef>) getSession().createQuery(sql).list();
    }

    @SuppressWarnings("unchecked")
    Collection<GalertDef> findAll(ResourceGroup g) {
        String sql = "from GalertDef d where d.group = :group "
                     + "and d.deleted = false "
                     + "and d.group.resource.resourceType is not null "
                     + "order by d.name";

        return (Collection<GalertDef>) getSession().createQuery(sql).setParameter("group", g)
            .list();
    }

    @SuppressWarnings("unchecked")
    List<GalertDef> findAll(AuthzSubject subj, AlertSeverity minSeverity, Boolean enabled,
                            PageInfo pInfo) {
        String sql = PermissionManagerFactory.getInstance().getGroupAlertDefsHQL();

        sql += " and d.deleted = false";
        sql += " and d.group.resource.resourceType is not null ";
        if (enabled != null) {
            sql += " and d.enabled = " + (enabled.booleanValue() ? "true" : "false");
        }

        sql += getOrderByClause(pInfo);

        Query q = getSession().createQuery(sql).setInteger("priority", minSeverity.getCode());

        if (sql.indexOf("subj") > 0) {
            q.setInteger("subj", subj.getId().intValue()).setParameter("op",
                AuthzConstants.groupOpManageAlerts);
        }

        return (List<GalertDef>) pInfo.pageResults(q).list();
    }

    private String getOrderByClause(PageInfo pInfo) {
        GalertDefSortField sort = (GalertDefSortField) pInfo.getSort();
        String res = " order by " + sort.getSortString("d", "g", "e") +
                     (pInfo.isAscending() ? "" : " DESC");

        if (!sort.equals(GalertDefSortField.CTIME)) {
            res += ", " + GalertDefSortField.CTIME.getSortString("d", "g", "e") + " DESC";
        }
        return res;
    }

    int countByStrategy(ExecutionStrategyTypeInfo strat) {
        String sql = "select count(*) from GalertDef d " + "where d.strategyInfo.type = :type";

        return ((Number) getSession().createQuery(sql).setParameter("type", strat).uniqueResult())
            .intValue();
    }

    @SuppressWarnings("unchecked")
    Collection<GalertDef> getUsing(Escalation e) {
        return (Collection<GalertDef>) getSession().createQuery(
            "from GalertDef where escalation = :esc").setParameter("esc", e).list();
    }

}
