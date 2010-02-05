package org.hyperic.hq.common.server.session;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class CrispoOptionDAO
    extends HibernateDAO<CrispoOption> {

    @Autowired
    public CrispoOptionDAO(SessionFactory f) {
        super(CrispoOption.class, f);
    }

    /**
     * Return a list of CrispoOption's that have a key that contains the given
     * String key
     * @param key The key to search for
     * @return A List of CrispoOptions that have a key that contains the given
     *         search key.
     */
    @SuppressWarnings("unchecked")
    List<CrispoOption> findOptionsByKey(String key) {
        return createCriteria().add(Restrictions.like("key", "%" + key + "%")).list();
    }

    @SuppressWarnings("unchecked")
    List<CrispoOption> findOptionsByValue(String val) {
        String hql = "from CrispoOption o join o.array a where "
                     + "o.optionValue = :val or a = :val";
        return createQuery(hql).setString("val", val).list();
    }
}
