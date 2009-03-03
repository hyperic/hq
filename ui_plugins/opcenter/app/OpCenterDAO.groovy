import org.hyperic.dao.DAOFactory
import org.hyperic.util.jdbc.DBUtil

class OpCenterDAO {

    public static List getUnfixedAlerts(Integer group) {

        def sess = DAOFactory.getDAOFactory().currentSession
        // TODO: can we get this through the dialect or let hibernate handle it?
        def boolFalse = DBUtil.getBooleanValue(false, sess.connection())

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
                      d.deleted = ${boolFalse} and
                      a.fixed = ${boolFalse}
                group by a.alert_definition_id, s.id
        """
        
        sess.createSQLQuery(hql).list()
    }
}