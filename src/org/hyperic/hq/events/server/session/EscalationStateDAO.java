/**
 *
 */
package org.hyperic.hq.events.server.session;

import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.dao.DAOFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.io.Serializable;

public class EscalationStateDAO extends HibernateDAO
{
    private static Log log = LogFactory.getLog(EscalationStateDAO.class);

    public EscalationStateDAO(DAOFactory f)
    {
        super(EscalationState.class, f);
    }

    public void remove(EscalationState entity)
    {
        super.remove(entity);
    }

    public void save(EscalationState entity)
    {
        super.save(entity);
    }

    public EscalationState merge(EscalationState entity)
    {
        return (EscalationState)super.merge(entity);
    }

    public EscalationState findById(Integer id)
    {
        return (EscalationState)super.findById(id);
    }

    public EscalationState get(Integer id)
    {
        return (EscalationState)super.get(id);
    }

    public void savePersisted(PersistedObject entity)
    {
        save((EscalationState)entity);
    }

    public void removePersisted(PersistedObject entity)
    {
        remove((EscalationState)entity);
    }

    public EscalationState getEscalationState(Escalation e, Integer alertDefId,
                                              int alertType)
    {
        String sql = "from EscalationState where escalation=? and " +
                     "alertDefinitionId=? and alertType=?";
        EscalationState state =
            (EscalationState)getSession().createQuery(sql)
                .setEntity(0, e)
                .setInteger(1, alertDefId.intValue())
                .setInteger(2, alertType)
                .uniqueResult();
        return state;
    }

    public int removeByEscalation(Escalation e)
    {
        String sql = "delete EscalationState where escalation=?";
        return getSession().createQuery(sql)
            .setEntity(0, e)
            .executeUpdate();
    }

    public List findScheduledEscalationState()
    {
        long now = System.currentTimeMillis();
        String sql="from EscalationState " +
                   "where" +
                   "  active = true and " +
                   "  scheduleRunTime > 0 and " +
                   "  scheduleRunTime <= ?";
        
        return getSession().createQuery(sql)
            .setLong(0, now)
            .list();
    }
}
