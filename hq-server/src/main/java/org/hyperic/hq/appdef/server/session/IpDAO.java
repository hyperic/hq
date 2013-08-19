/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2013], VMware, Inc. 
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
package org.hyperic.hq.appdef.server.session;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.type.IntegerType;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class IpDAO extends HibernateDAO<Ip> {

    @Autowired
    public IpDAO(SessionFactory f) {
        super(Ip.class, f);
    }
    
    @SuppressWarnings("unchecked")
    List<Ip> getIps(Collection<Integer> platformIds) {
        if (platformIds == null || platformIds.isEmpty()) {
            return Collections.emptyList();
        }
        String hql = "from Ip i where i.platform.id in (:platformIds)";
        return getSession()
            .createQuery(hql)
            .setParameterList("platformIds", platformIds, new IntegerType())
            .list();
    }

}
