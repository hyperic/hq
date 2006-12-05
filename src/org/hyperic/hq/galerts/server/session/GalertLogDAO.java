package org.hyperic.hq.galerts.server.session;

import java.util.List;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.dao.HibernateDAO;

public class GalertLogDAO
    extends HibernateDAO
{
    public GalertLogDAO(DAOFactory f) {
        super(GalertLog.class, f);
    }

    GalertLog findById(Integer id) {
        return (GalertLog)super.findById(id);
    }

    void save(GalertLog log) {
        super.save(log);
    }

    void remove(GalertLog log) {
        super.remove(log);
    }
    
    List findAll(ResourceGroup g) {
        String sql = "from GalertLog l where l.group = :group " + 
                     "order by l.timestamp";
        
        return getSession().createQuery(sql)
            .setParameter("group", g)
            .list();
    }

    void removeAll(ResourceGroup g) {
        String sql = "delete from GalertLog l where l.group = :group";
        
        getSession().createQuery(sql)
            .setParameter("group", g)
            .executeUpdate();
    }
}
