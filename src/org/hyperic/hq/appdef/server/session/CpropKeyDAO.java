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

import java.util.List;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.HibernateDAO;

public class CpropKeyDAO extends HibernateDAO
{
    public CpropKeyDAO(DAOFactory f) {
        super(CpropKey.class, f);
    }

    public CpropKey findById(Integer id) {
        return (CpropKey)super.findById(id);
    }

    void save(CpropKey entity) {
        super.save(entity);
    }

    void remove(CpropKey entity) {
        super.remove(entity);
    }

    CpropKey create(int appdefType, int appdefTypeId, String key,
                    String description)
    {
        CpropKey cpropkey = new CpropKey(appdefType, appdefTypeId,
                                         key, description);
        save(cpropkey);
        return cpropkey;
    }

    List findByAppdefType(int appdefType, int appdefId)
    {
        String sql = "from CpropKey k where k.appdefType=? and " +
                     "k.appdefTypeId = ?";
        return getSession().createQuery(sql)
            .setInteger(0, appdefType)
            .setInteger(1, appdefId)
            .list();
    }

    CpropKey findByKey(int appdefType, int appdefTypeId, String key)
    {
        String sql = "from CpropKey k where k.appdefType=? and " +
                     "k.appdefTypeId=? and k.key=?";
        return (CpropKey)getSession().createQuery(sql)
            .setInteger(0, appdefType)
            .setInteger(1, appdefTypeId)
            .setString(2, key)
            .setCacheable(true)
            .setCacheRegion("CpropKey.findByKey")
            .uniqueResult();
    }
}
