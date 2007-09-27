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

package org.hyperic.hq.appdef.server.session;

import org.hibernate.criterion.Expression;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;

public class ResourceAuxLogDAO extends HibernateDAO {
    public ResourceAuxLogDAO(DAOFactory f) {
        super(ResourceAuxLogPojo.class, f);
    }

    ResourceAuxLogPojo findById(Integer id) {
        return (ResourceAuxLogPojo)super.findById(id);
    }

    void save(ResourceAuxLogPojo log) {
        super.save(log);
    }

    void remove(ResourceAuxLogPojo log) {
        super.remove(log);
    }
    
    ResourceAuxLogPojo find(GalertAuxLog log) {
        return (ResourceAuxLogPojo) createCriteria()
            .add(Expression.eq("auxLog", log))
            .uniqueResult();
    }
    
    void removeAll(GalertDef def) {
        String sql = "delete from ResourceAuxLogPojo p where p.alertDef = :def";
            
        getSession().createQuery(sql)
                    .setParameter("def", def)
                    .executeUpdate();
    }
}
