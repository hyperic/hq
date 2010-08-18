/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2010], VMware, Inc.
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

package org.hyperic.hq.appdef.server.session;

import org.hibernate.SessionFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationTypeDAO
    extends HibernateDAO<ApplicationType> {

    private ServiceTypeDAO serviceTypeDAO;

    @Autowired
    public ApplicationTypeDAO(SessionFactory f, ServiceTypeDAO serviceTypeDAO) {
        super(ApplicationType.class, f);
        this.serviceTypeDAO = serviceTypeDAO;
    }

    public ApplicationType create(String name, String desc) {
        ApplicationType type = new ApplicationType();
        type.setName(name);
        type.setDescription(desc);
        save(type);
        return type;
    }

    public ApplicationType findByName(String name) {
        String sql = "from ApplicationType where sortName=?";
        return (ApplicationType) getSession().createQuery(sql).setString(0, name.toUpperCase())
            .uniqueResult();
    }

    public boolean supportsServiceType(ApplicationType at, Integer stPK) {
        if (at.getServiceTypes() == null) {
            return false;
        }
        ServiceType serviceType = serviceTypeDAO.findById(stPK);
        return at.getServiceTypes().contains(serviceType);
    }
}
