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

    public EscalationState getEscalationState(Escalation e, Integer alertDefId)
    {
        String sql = "from EscalationState where escalation=? and " +
                     "alertDefinitionId=?";
        EscalationState state =
            (EscalationState)getSession().createQuery(sql)
                .setEntity(0, e)
                .setInteger(1, alertDefId.intValue())
                .uniqueResult();
        if (state == null) {
            state = EscalationState.newInstance(e, alertDefId);
            save(state);
        }
        return state;
    }

    public int removeByEscalation(Escalation e)
    {
        String sql = "delete EscalationState where escalation=?";
        return getSession().createQuery(sql)
            .setEntity(0, e)
            .executeUpdate();
    }

    public int clearActiveEscalation()
    {
        String sql="update EscalationState " +
                   "set active=false " +
                   "where" +
                   "  active = true ";

        int count = getSession().createQuery(sql).executeUpdate();

        if (count > 0 && log.isInfoEnabled()) {
            log.info("Cleared " + count + " active " +
                     (count == 1 ? "escalation." : "escalations."));
        }
        return count;
    }

    public void clearActiveEscalation(Escalation e, Integer alertDefId)
    {
        EscalationState state = getEscalationState(e, alertDefId);
        state.setActive(false);
        save(state);
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
