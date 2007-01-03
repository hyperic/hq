package org.hyperic.hq.galerts.server.session;

import java.util.List;

import org.hibernate.Query;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

class GalertLogDAO
    extends HibernateDAO
{
    GalertLogDAO(DAOFactory f) {
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

    PageList findByTimeWindow(ResourceGroup g, long begin, PageControl pc) {
        String sql = "from GalertLog l " +
                     "where l.group = :group and l.timestamp > :time "; 
                     
        Integer count = (Integer)
            getSession().createQuery("select count(*) " + sql)
                        .setParameter("group", g)
                        .setLong("time", begin)
                        .uniqueResult();

        if (count.intValue() > 0) {
            Query q = getSession()
                .createQuery(sql + "order by l.timestamp " +
                             (pc.isDescending() ? "desc" : "asc"))
                .setParameter("group", g)
                .setLong("time", begin);
            
            return getPagedResults(q, count.intValue(), pc);
        }

        return new PageList();
    }

    void removeAll(ResourceGroup g) {
        String sql = "delete from GalertLog l where l.group = :group";
        
        getSession().createQuery(sql)
            .setParameter("group", g)
            .executeUpdate();
    }
    
    void removeAll(GalertDef d) {
        String sql = "delete from GalertLog l where l.alertDef = :def";
        
        getSession().createQuery(sql)
                    .setParameter("def", d)
                    .executeUpdate();
    }
}
