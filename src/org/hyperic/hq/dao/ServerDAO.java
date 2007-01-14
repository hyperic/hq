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

import org.hibernate.criterion.Expression;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.shared.PlatformLightValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.Virtual;
import org.hyperic.hq.authz.shared.AuthzConstants;

/**
 * CRUD methods, finders, etc. for Server
 */
public class ServerDAO extends HibernateDAO
{
    public ServerDAO(DAOFactory f) {
        super(Server.class, f);
    }

    public Server findById(Integer id)
    {
        return (Server)super.findById(id);
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

    /**
     * legacy EJB method for creating a server.
     *
     * Create a server for this Platform. The server is assumed
     * to have an associated server type already set. This operation
     * has to be performed as part of an existing transaction.
     */
    public Server createServer(Platform p, ServerValue sv)
        throws ValidationException
    {
        // validate the object
        validateNewServer(p, sv);
        // set the parent platform to be this
        // XXX cheap hack, the ejbPostCreate in ServerEJB only
        // needs to be able to detect the foreign key of
        // the parent platform. So, I'll skip the valueobject
        // creation and create one with just an ID
        PlatformLightValue pv = new PlatformLightValue();
        pv.setId(p.getId());
        sv.setPlatform(pv);
        // get the server home
        // create it
        return create(sv, p);
    }

    /**
     * Validate a server value object which is to be created on this
     * platform. This method will check IP conflicts and any other
     * special constraint required to succesfully add a server instance
     * to a platform
     */
    private void validateNewServer(Platform p, ServerValue sv)
        throws ValidationException
    {
        // ensure the server value has a server type
        String msg = null;
        if(sv.getServerType() == null) {
            msg = "Server has no ServiceType";
        } else if(sv.idHasBeenSet()){
            msg = "This server is not new, it has ID:" + sv.getId();
        }
        if(msg == null) {
            Integer id = sv.getServerType().getId();
            Collection stypes = p.getPlatformType().getServerTypes();
            for (Iterator i = stypes.iterator(); i.hasNext();) {
                ServerType sVal = (ServerType)i.next();
                if(sVal.getId().equals(id))
                    return;
            }
            msg = "Servers of type '" + sv.getServerType().getName() +
                "' cannot be created on platforms of type '" +
                p.getPlatformType().getName() +"'";
        }
        if (msg != null) {
            throw new ValidationException(msg);
        }
    }

    /**
     * @deprecated use findAll_orderName()
     * @return
     */
    public Collection findAll_orderName_asc()
    {
        return findAll_orderName(true);
    }

    /**
     * @deprecated use findAll_orderName()
     * @return
     */
    public Collection findAll_orderName_desc()
    {
        return findAll_orderName(false);
    }

    public Collection findAll_orderName(boolean asc)
    {
        String sql="from Server s join fetch s.serverType st " +
                   "where st.virtual=false " +
                   "order by s.sortName " + (asc ? "asc" : "desc");
        return getSession().createQuery(sql).list();
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
            .list();
    }

    public List findByServices(Integer[] ids)
    {
        return createCriteria()
            .createAlias("services", "s")
            .add( Expression.in("s.id", ids))
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
}
