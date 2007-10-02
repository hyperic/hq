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

package org.hyperic.hq.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Expression;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.Virtual;
import org.hyperic.hq.authz.shared.AuthzConstants;

public class ServerDAO extends HibernateDAO
{
    public ServerDAO(DAOFactory f) {
        super(Server.class, f);
    }

    public Server findById(Integer id)
    {
        return (Server)super.findById(id);
    }

    public Server get(Integer id)
    {
        return (Server)super.get(id);
    }

    public void save(Server entity)
    {
        super.save(entity);
    }

    public void remove(Server entity)
    {
        super.remove(entity);
    }

    public Server create(ServerValue sv, Platform p)
    {
        ConfigResponseDB configResponse = DAOFactory.getDAOFactory()
            .getConfigResponseDAO().create();

        Server s = new Server();
        s.setName(sv.getName());
        s.setDescription(sv.getDescription());
        s.setInstallPath(sv.getInstallPath());
        String aiid = sv.getAutoinventoryIdentifier();
        if (aiid != null) {
            s.setAutoinventoryIdentifier(sv.getAutoinventoryIdentifier());
        } else {
            // Server was created by hand, use a generated AIID. (This matches
            // the behaviour in 2.7 and prior)
            aiid = sv.getInstallPath() + "_" + System.currentTimeMillis() +
                "_" + sv.getName();
            s.setAutoinventoryIdentifier(aiid);
        }

        s.setServicesAutomanaged(sv.getServicesAutomanaged());
        s.setRuntimeAutodiscovery(sv.getRuntimeAutodiscovery());
        s.setWasAutodiscovered(sv.getWasAutodiscovered());
        s.setAutodiscoveryZombie(false);
        s.setOwner(sv.getOwner());
        s.setLocation(sv.getLocation());
        s.setModifiedBy(sv.getModifiedBy());
        s.setConfigResponse(configResponse);
        s.setPlatform(p);

        Integer stid = sv.getServerType().getId();
        ServerType st = 
            DAOFactory.getDAOFactory().getServerTypeDAO().findById(stid);
        s.setServerType(st);
        save(s);
        return s;
    }

    public Collection findAll_orderName(boolean asc)
    {
        String sql="from Server s join fetch s.serverType st " +
                   "where st.virtual=false " +
                   "order by s.sortName " + (asc ? "asc" : "desc");
        return getSession()
            .createQuery(sql)
            .setCacheable(true)
            .setCacheRegion("Server.findAll_orderName")
            .list();
    }

    public Collection findByType(Integer sTypeId)
    {
        String sql="from Server where serverType.id=?";
        return getSession().createQuery(sql)
            .setInteger(0, sTypeId.intValue())
            .list();
    }

    public List findByPlatform_orderName(Integer id)
    {
        String sql="from Server where platform.id=? " +
                   "order by sortName";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }

    public List findByPlatform_orderName(Integer id, Boolean virtual)
    {
        String sql="from Server where platform.id=? and " +
                   "serverType.virtual=? " +
                   "order by sortName";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .setBoolean(1, virtual.booleanValue())
            .setCacheable(true)
            .setCacheRegion("Server.findByPlatform_orderName")
            .list();
    }

    public List findByPlatformAndType_orderName(Integer id, Integer tid)
    {
        String sql="from Server where platform.id=? and " +
                   "serverType.id=? " +
                   "order by sortName";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .setInteger(1, tid.intValue())
            .list();
    }

    public List findByPlatformAndType_orderName(Integer id, Integer tid,
                                                Boolean isVirtual)
    {
        String sql="select s from Server s join s.serverType st " +
                   "where s.platform.id=? and " +
                   "st.id=? and " +
                   "st.virtual=? " +
                   "order by s.sortName";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .setInteger(1, tid.intValue())
            .setBoolean(2, isVirtual.booleanValue())
            .setCacheable(true)
            .setCacheRegion("Server.findByPlatformAndType_orderName")
            .list();
    }

    public List findByServices(Integer[] ids)
    {
        return createCriteria()
            .createAlias("services", "s")
            .add( Expression.in("s.id", ids))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
            .list();
    }

    public List findByName(String name)
    {
        String sql="from Server where sortName=?";
        return getSession().createQuery(sql)
            .setString(0, name.toUpperCase())
            .list();
    }

    public Resource findVirtualByInstanceId(Integer id) {
        VirtualDAO dao = DAOFactory.getDAOFactory().getVirtualDAO();
        return dao.findVirtualByInstanceId(id, AuthzConstants.serverResType);
    }

    public Collection findVirtualByProcessId(Integer id) {
        VirtualDAO dao = DAOFactory.getDAOFactory().getVirtualDAO();
        Collection resources =
            dao.findVirtualByProcessId(id, AuthzConstants.serverResType);
        List servers = new ArrayList();
        for (Iterator it = resources.iterator(); it.hasNext(); ) {
            Virtual virt = (Virtual) it.next();
            servers.add(findById(virt.getId()));
        }
        return servers;
    }

    public Collection findVirtualByPysicalId(Integer id) {
        VirtualDAO dao = DAOFactory.getDAOFactory().getVirtualDAO();
        Collection resources =
            dao.findVirtualByPysicalId(id, AuthzConstants.serverResType);
        List servers = new ArrayList();
        for (Iterator it = resources.iterator(); it.hasNext(); ) {
            Virtual virt = (Virtual) it.next();
            servers.add(findById(virt.getId()));
        }
        return servers;
    }
    
    public List getServerTypeCounts() {
        String sql = "select t.name, count(*) from ServerType t, " + 
                     "Server s where s.serverType = t " + 
                     "group by t.name order by t.name";
        
        return getSession().createQuery(sql).list();
    }
    
}
