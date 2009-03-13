import org.hyperic.dao.DAOFactory
import org.hyperic.hq.events.server.session.EventLog
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.measurement.shared.ResourceLogEvent

/**
 * This DAO adds custom queries required for rendering the dashboard in
 * a performant way.
 * TODO: Investigate merging these back into the HQ source.
 */
class OpCenterDAO {

    public static List getUnfixedAlerts(Integer group) {

        def sess = DAOFactory.getDAOFactory().currentSession

        def groupClause = ""
        if (group != null && group > 0) {
            groupClause = """
            exists (select 1 from EAM_RES_GRP_RES_MAP 
                    where resource_id = d.resource_id and
                          resource_group_id=${group}) and
        """
        }

        def hql = """
            select a.alert_definition_id, s.id, count(a.id), max(a.id)
                from EAM_ALERT_DEFINITION d,
                     EAM_ALERT a left outer join EAM_ESCALATION_STATE s on s.alert_id = a.id
                where ${groupClause} d.id = a.alert_definition_id and
                      d.deleted = '0' and
                      a.fixed = '0'
                group by a.alert_definition_id, s.id
        """
        
        sess.createSQLQuery(hql).list()
    }

    public static EventLog getLastLog(Resource r) {

        def sess = DAOFactory.getDAOFactory().currentSession

        def hql = """
            select l from EventLog l where l.resource = :resource and type = :type order by timestamp desc
        """

        return (EventLog)sess.createQuery(hql)
                .setParameter("resource", r)
                .setParameter("type", ResourceLogEvent.class.name)
                .setMaxResults(1).uniqueResult()
    }
}