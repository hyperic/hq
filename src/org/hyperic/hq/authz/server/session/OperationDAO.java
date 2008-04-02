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

package org.hyperic.hq.authz.server.session;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.HibernateDAO;

public class OperationDAO extends HibernateDAO {
    public OperationDAO(DAOFactory f) {
        super(Operation.class, f);
    }


    Operation findById(Integer id) {
        return (Operation) super.findById(id);
    }

    void save(Operation entity) {
        super.save(entity);
    }

    void remove(Operation entity) {
        super.remove(entity);
    }

    public Operation findByTypeAndName(ResourceType type, String name) {
        String sql = "from Operation where resourceType.id=? and name=?";

        return (Operation)getSession().createQuery(sql)
            .setInteger(0, type.getId().intValue())
            .setString(1, name)
            .setCacheable(true)
            .setCacheRegion("Operation.findByTypeAndName")
            .uniqueResult();
    }
}
