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

package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.CpropKey;
import org.hyperic.hq.appdef.shared.CPropKeyPK;

import java.util.Collection;

/**
 * CRUD methods, finders, etc. for CpropKey
 */
public class CpropKeyDAO extends HibernateDAO implements ICPropKeyDAO
{
    public CpropKeyDAO(Session session)
    {
        super(CpropKey.class, session);
    }

    protected CpropKey findById(Integer id)
    {
        return (CpropKey)super.findById(id);
    }

    public void evict(CpropKey entity)
    {
        super.evict(entity);
    }

    public CpropKey merge(CpropKey entity)
    {
        return (CpropKey)super.merge(entity);
    }

    public void save(CpropKey entity)
    {
        super.save(entity);
    }

    public void remove(CpropKey entity)
    {
        super.remove(entity);
    }

    public CpropKey create(int appdefType, int appdefTypeId, String key, String description)
    {
        CpropKey cpropkey = new CpropKey();
        cpropkey.setAppdefType(new Integer(appdefType));
        cpropkey.setAppdefTypeId(new Integer(appdefTypeId));
        cpropkey.setKey(key);
        cpropkey.setDescription(description);
        save(cpropkey);
        return cpropkey;
    }

    public Collection findByAppdefType(int appdefType, int appdefId)
    {
        String sql = "from CpropKey k where k.appdefType = ? and k.appdefTypeId = ?";
        return getSession().createQuery(sql)
            .setInteger(0, appdefType)
            .setInteger(1, appdefId)
            .list();
    }

    public CpropKey findByKey(int appdefType, int appdefTypeId, String key)
    {
        String sql = "from CpropKey k where k.appdefType=? and k.appdefTypeId=? and k.key=?";
        return (CpropKey)getSession().createQuery(sql)
            .setInteger(0, appdefType)
            .setInteger(1, appdefTypeId)
            .setString(2, key)
            .uniqueResult();
    }

    /**
     * for legacy EJB Entity Bean compatibility.
     * @param pk
     * @return
     */
    public CpropKey findByPrimaryKey(CPropKeyPK pk)
    {
        return findById(pk.getId());
    }
}
