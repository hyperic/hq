/*
 * NOTE: This copyright doesnot cover user programs that use HQ program services
 * by normal system calls through the application program interfaces provided as
 * part of the Hyperic Plug-in Development Kit or the Hyperic Client Development
 * Kit - this is merely considered normal use of the program, and doesnot fall
 * under the heading of "derived work". Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ. HQ is free software; you can redistribute it and/or
 * modify it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; if not, write
 * to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA.
 */
package org.hyperic.hq.events.server.session;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.dialect.Dialect;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.timer.StopWatch;


public class TriggerDAO
    extends HibernateDAO implements TriggerDAOInterface
{
    private final Log log = LogFactory.getLog(TriggerDAO.class);

    public TriggerDAO(DAOFactory f) {
        super(RegisteredTrigger.class, f);
    }

    public RegisteredTrigger create(RegisteredTriggerValue createInfo) {
        RegisteredTrigger res = new RegisteredTrigger(createInfo);
        save(res);

        // Set the new ID just in case someone wants to use it
        createInfo.setId(res.getId());

        return res;
    }

    public RegisteredTrigger findById(Integer id) {
        return (RegisteredTrigger) super.findById(id);
    }

    public RegisteredTrigger get(Integer id) {
        return (RegisteredTrigger) super.get(id);
    }

    /**
     * Find all the registered triggers associated with the alert definition.
     *
     * @param id The alert definition id.
     * @return The list of associated registered triggers.
     */
    public List findByAlertDefinitionId(Integer id) {
        String sql = "from RegisteredTrigger rt where rt.alertDefinition.id = :defId";

        return getSession().createQuery(sql).setParameter("defId", id).list();
    }


    public Set findAllEnabledTriggers() {
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();

        // For performance optimization, we want to fetch each trigger's alert
        // def as well as the alert def's alert definition state and conditions
        // in a single query (as they will be used to create 
        // AlertConditionEvaluators when creating trigger impls). This query 
        // guarantees that when we do trigger.getAlertDefinition().getConditions(), 
        // the database is not hit again
        String hql = new StringBuilder(256)
            .append("from AlertDefinition ad ")
            .append("join fetch ad.alertDefinitionState ")
            .append("join fetch ad.conditionsBag c ")
            .append("join fetch c.trigger ")
            .append("where ad.enabled = '1'")
            .toString();
        
        if (debug) watch.markTimeBegin("createQuery.list");
        List alertDefs = getSession().createQuery(hql).list();
        if (debug) watch.markTimeEnd("createQuery.list");        
        
        Set triggers = new LinkedHashSet();

        if (debug) watch.markTimeBegin("addTriggers");

        for(Iterator iterator = alertDefs.iterator();iterator.hasNext();) {
            AlertDefinition defs = (AlertDefinition)iterator.next();
            for(Iterator it = defs.getConditionsBag().iterator(); it.hasNext();) {
                AlertCondition condition = (AlertCondition)it.next();
                triggers.add(condition.getTrigger());
            }
        }

        if (debug) {
            watch.markTimeEnd("addTriggers");
            log.debug("findAllEnabledTriggers: " + watch);
        }

        return triggers;
    }

}
