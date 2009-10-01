package org.hyperic.hq.common.server.session;

import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.dao.DAOFactory;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public class CrispoOptionDAO extends HibernateDAO {

    @Autowired
    public CrispoOptionDAO(SessionFactory f) {
        super(CrispoOption.class, f);
    }

    void remove(CrispoOption o) {
        super.remove(o);
    }

    void save(CrispoOption o) {
        super.save(o);
    }

    /**
     * Return a list of CrispoOption's that have a key that contains the
     * given String key
     * @param key The key to search for
     * @return A List of CrispoOptions that have a key that contains the
     * given search key.
     */
    List findOptionsByKey(String key) {
        return createCriteria().add(Restrictions.like("key",
                                                      "%" + key + "%")).list();
    }

    List findOptionsByValue(String val) {
        String hql = "from CrispoOption o join o.array a where " +
        		     "o.optionValue = :val or a = :val";
        return createQuery(hql)
            .setString("val", val)
            .list();
    }
}
