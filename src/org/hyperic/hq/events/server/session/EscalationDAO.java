package org.hyperic.hq.events.server.session;

import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PersistedObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.NonUniqueObjectException;

import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import java.io.Serializable;

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

/**
 * CRUD for Escalation, finders
 */
public class EscalationDAO extends HibernateDAO
{
    private static Log log = LogFactory.getLog(EscalationDAO.class);

    public EscalationDAO(DAOFactory df) {
        super(Escalation.class, df);
    }

    public void save(Escalation entity) throws PermissionException
    {
        checkUpdatePermission(entity);
        saveActions(entity.getActions().iterator());
        super.save(entity);
    }

    private void checkUpdatePermission(Escalation entity)
        throws PermissionException
    {
        Integer subjectId = entity.getSubjectId();
        if (subjectId != null) {
            if (entity.getId() == null) {
                SessionBase.canCreateEscalation(subjectId);
            } else {
                SessionBase.canModifyEscalation(subjectId);
            }
        }
    }

    public Escalation merge(Escalation entity) throws PermissionException
    {
        checkUpdatePermission(entity);
        mergeActions(entity.getActions().iterator());
        return (Escalation)super.merge(entity);
    }

    public Escalation get(Integer id) {
        return (Escalation)super.get(id);
    }

    public void remove(Escalation entity) throws PermissionException
    {
        Integer subjectId = entity.getSubjectId();
        if (subjectId != null) {
            SessionBase.canRemoveEscalation(subjectId);
        }
        DAOFactory.getDAOFactory().getEscalationStateDAO()
            .removeByEscalation(entity);
        removeActions(entity.getActions().iterator());
        super.remove(entity);
    }

    public Collection findAll(Integer subjectId) throws PermissionException
    {
        SessionBase.canViewEscalation(subjectId);
        return super.findAll();
    }

    public void clearActions(Escalation entity) {
        removeActions(entity.getActions().iterator());
        entity.getActions().clear();
    }

    public Escalation findById(Integer id) {
        return (Escalation)super.findById(id);
    }

    public Escalation findByName(String name) {
        String sql = "from Escalation where name=?";
        return (Escalation)getSession().createQuery(sql)
            .setString(0, name)
            .uniqueResult();
    }

    private void removeActions(Iterator i) {
        ActionDAO dao = DAOFactory.getDAOFactory().getActionDAO();
        // have to remove actions manually as we can't setup
        // cascade relationship via the hbm model, :(
        while(i.hasNext()) {
            EscalationAction ea = (EscalationAction)i.next();
            Integer id = ea.getAction().getId();
            if (id != null && dao.get(ea.getAction().getId()) != null) {
                dao.remove(ea.getAction());
            }
        }
    }

    private void saveActions(Iterator i) {
        ActionDAO dao = DAOFactory.getDAOFactory().getActionDAO();
        while(i.hasNext()) {
            EscalationAction ea = (EscalationAction)i.next();
            dao.save(ea.getAction());
        }
    }

    private void mergeActions(Iterator i) {
        ActionDAO dao = DAOFactory.getDAOFactory().getActionDAO();
        while(i.hasNext()) {
            EscalationAction ea = (EscalationAction)i.next();
            dao.merge(ea.getAction());
        }
    }

    public PersistedObject findPersistedById(Integer subjectId, Serializable id)
        throws PermissionException
    {
        SessionBase.canViewEscalation(subjectId);
        return findById((Integer)id);
    }

    public PersistedObject findPersisted(PersistedObject entity)
        throws PermissionException
    {
        Integer subjectId = entity.getSubjectId();
        if (subjectId != null) {
            SessionBase.canViewEscalation(subjectId);
        }
        List result = findByExample(entity);
        if (result.size() == 0) {
            return null;
        } else if (result.size() > 1) {
            throw new NonUniqueObjectException("found " + result.size() +
                    " escalations.", entity.getId(),
                    entity.getClass().getName());
        }
        return (Escalation)result.get(0);
    }

    public void savePersisted(PersistedObject entity) throws PermissionException
    {
        save((Escalation)entity);
    }

    public void removePersisted(PersistedObject entity)
        throws PermissionException
    {
        remove((Escalation)entity);
    }

    public void clearActiveEscalation(Integer escalationId, Integer alertDefId)
    {
        DAOFactory.getDAOFactory().getEscalationStateDAO()
            .clearActiveEscalation(findById(escalationId), alertDefId);
    }

    public Escalation findByAlertDefinitionId(Integer subjectId, Integer id)
        throws PermissionException
    {
        SessionBase.canViewEscalation(subjectId);
        String sql = "select e from AlertDefinition d " +
                "join Escalation e " +
                "where d.id = ?";
        return (Escalation)getSession().createQuery(sql)
                .setInteger(0, id.intValue())
                .uniqueResult();
    }

    public void deleteById(Integer subjectId, Integer[] ids)
        throws PermissionException
    {
        SessionBase.canRemoveEscalation(subjectId);
        for (int i = 0; i < ids.length; i++) {
            Escalation e = get(ids[i]);
            if (e != null) {
                remove(e);
            }
        }
    }

    public void deleteByName(Integer subjectId, String name)
        throws PermissionException
    {
        SessionBase.canRemoveEscalation(subjectId);
        String sql = "from Escalation where name = ?";
        Escalation e = (Escalation)getSession().createQuery(sql)
            .setString(0, name).uniqueResult();
        if (e != null) {
            remove(e);
        }
    }
}
