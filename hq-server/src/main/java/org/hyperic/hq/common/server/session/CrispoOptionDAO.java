/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

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
