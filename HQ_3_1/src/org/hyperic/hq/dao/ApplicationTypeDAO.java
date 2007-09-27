package org.hyperic.hq.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.server.session.ApplicationType;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.ApplicationTypeValue;

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

public class ApplicationTypeDAO extends HibernateDAO
{
    private static final Log log = LogFactory.getLog(ApplicationTypeDAO.class);

    public ApplicationTypeDAO(DAOFactory f) {
        super(ApplicationType.class, f);
    }

    public ApplicationType findById(Integer id)
    {
        return (ApplicationType)super.findById(id);
    }

    public void save(ApplicationType entity)
    {
        super.save(entity);
    }

    public void remove(ApplicationType entity)
    {
        super.remove(entity);
    }

    public ApplicationType create(ApplicationTypeValue appType)
    {
        ApplicationType type = new ApplicationType();
        type.setName(appType.getName());
        type.setDescription(appType.getDescription());
        save(type);
        return type;
    }

    public ApplicationType findByName(String name)
    {
        String sql="from ApplicationType where sortName=?";
        return (ApplicationType)getSession().createQuery(sql)
            .setString(0, name.toUpperCase())
            .uniqueResult();
    }

    public boolean supportsServiceType(ApplicationType at, Integer stPK)
    {
        if (at.getServiceTypes() == null) {
            return false;
        }
        ServiceType serviceType = DAOFactory.getDAOFactory()
            .getServiceTypeDAO().findById(stPK);
        return at.getServiceTypes().contains(serviceType);
    }
}
