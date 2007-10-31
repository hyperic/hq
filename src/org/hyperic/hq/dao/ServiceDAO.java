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

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.Virtual;
import org.hyperic.hq.authz.shared.AuthzConstants;

public class ServiceDAO extends HibernateDAO
{
    public ServiceDAO(DAOFactory f) {
        super(Service.class, f);
    }

    public Service findById(Integer id) {
        return (Service)super.findById(id);
    }

    public Service get(Integer id) {
        return (Service)super.get(id);
    }
    
    public void save(Service entity) {
        super.save(entity);
    }

    public void remove(Service entity) {
        super.remove(entity);
    }

    public Service create(ServiceValue sv, Server parent) {
        ConfigResponseDB configResponse =
            DAOFactory.getDAOFactory().getConfigResponseDAO().create();
        
        Service s = new Service();
        s.setName(sv.getName());
        s.setAutodiscoveryZombie(false);
        s.setServiceRt(false);
        s.setEndUserRt(false);
        s.setDescription(sv.getDescription());
        s.setModifiedBy(sv.getModifiedBy());
        s.setLocation(sv.getLocation());
        s.setOwner(sv.getOwner());
        s.setParentId(sv.getParentId());

        if (sv.getServiceType() != null) {
            Integer stId = sv.getServiceType().getId();
            ServiceType st = 
                DAOFactory.getDAOFactory().getServiceTypeDAO().findById(stId);
            s.setServiceType(st);
        }

        s.setServer(parent);
        s.setConfigResponse(configResponse);
        save(s);
        return s;
    }

    public Service createService(Server s, ServiceValue sv)
        throws ValidationException
    {
        // validate the service
        s.validateNewService(sv);
        // get the Service home
        return create(sv, s);
    }

    public Collection findByParent(Integer parentId)
    {
        String sql="from Service where parentService.id=?";
        return getSession().createQuery(sql)
            .setInteger(0, parentId.intValue())
            .list();
    }

    public Collection findByParentAndType(Integer parentId, Integer typeId)
    {
        String sql="from Service where parentService.id=? and serviceType.id=?";
        return getSession().createQuery(sql)
            .setInteger(0, parentId.intValue())
            .setInteger(1, typeId.intValue())
            .list();
    }

    public Collection findAll_orderName(boolean asc)
    {
        return getSession()
            .createQuery("from Service order by sortName " +
                         (asc ? "asc" : "desc"))
            .setCacheable(true)
            .setCacheRegion("Service.findAll_orderName")
            .list();
    }

    public Collection findAll_orderCtime(boolean asc)
    {
        return getSession()
            .createQuery("from Service order by creationTime " +
                         (asc ? "asc" : "desc"))
            .list();
    }

    public Collection findByType(Integer st, boolean asc)
    {
        String sql = "from Service where serviceType.id=? order by sortName " +
                     (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, st.intValue())
            .list();
    }

    public List findByName(String name)
    {
        String sql="from Service where sortName=?";
        return getSession().createQuery(sql)
            .setString(0, name.toUpperCase())
            .list();
    }

    public Collection findByPlatform_orderName(Integer id, boolean asc)
    {
        String sql="select sv from Service sv " +
                   " join fetch sv.server s " +
                   " join fetch s.platform p "+
                   "where p.id=?" +
                   "order by s.sortName " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }

    public Collection findByPlatform_orderType(Integer id, boolean asc)
    {
        String sql="select sv from Service sv " +
                   " join fetch sv.server s " +
                   " join fetch s.serverType st " +
                   " join fetch s.platform p "+
                   "where p.id=?" +
                   "order by st.sortName "+
                   (asc ? "asc" : "desc") +
                   ", s.sortName";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }

    public Collection findPlatformServices_orderName(Integer platId,
                                                     boolean asc)
    {
        String sql="select sv from Service sv " +
                   " join fetch sv.server s " +
                   " join fetch s.serverType st " +
                   " join fetch s.platform p " +
                   "where p.id=? " +
                   " and st.virtual=? " +
                   "order by sv.sortName " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, platId.intValue())
            .setBoolean(1, true)
            .setCacheRegion("Service.findPlatformServices")
            .setCacheable(true)
            .list();
    }

    public List findByServer_orderName(Integer id)
    {
        String sql="from Service where server.id=? order by sortName";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }

    public List findByServer_orderType(Integer id)
    {
        String sql="select s from Service s " +
                   " join fetch s.serviceType st " +
                   "where s.server.id=? " +
                   "order by st.sortName";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }

    public List findByServerAndType_orderName(Integer id, Integer tid)
    {
        String sql="from Service where server.id=? and serviceType.id=? " +
                   "order by sortName";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .setInteger(1, tid.intValue())
            .setCacheable(true)
            .setCacheRegion("Service.findByServerAndType")
            .list();
    }

    public Service findByApplication(Integer appId)
    {
        String sql="select s from Service s " +
                   " join fetch s.appServices a " +
                   "where a.application.id=? ";
        return (Service)getSession().createQuery(sql)
            .setInteger(0, appId.intValue())
            .uniqueResult();
    }

    public Collection findByCluster(Integer clusterId)
    {
        String sql="select s from Service s " +
                   " join fetch s.serviceCluster c " +
                   "where c.id=?";
        return getSession().createQuery(sql)
            .setInteger(0, clusterId.intValue())
            .list();
    }

    public Collection findAllClusterUnassigned_orderName(boolean asc)
    {
        String sql="from Service where serviceCluster is null " +
                   "order by sortName " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql).list();
    }

    public Collection findAllClusterAppUnassigned_orderName(boolean asc)
    {
        String sql="from Service where serviceCluster is null and " +
                   "appServices.size=0 " +
                   "order by sortName " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql).list();
    }
    
    public Resource findVirtualByInstanceId(Integer id) {
        VirtualDAO dao = DAOFactory.getDAOFactory().getVirtualDAO();
        return dao.findVirtualByInstanceId(id, AuthzConstants.serviceResType);
    }

    public Collection findVirtualByProcessId(Integer id) {
        VirtualDAO dao = DAOFactory.getDAOFactory().getVirtualDAO();
        Collection resources =
            dao.findVirtualByProcessId(id, AuthzConstants.serviceResType);
        List services = new ArrayList();
        for (Iterator it = resources.iterator(); it.hasNext(); ) {
            Virtual virt = (Virtual) it.next();
            services.add(findById(virt.getId()));
        }
        return services;
    }

    public Collection findVirtualByPysicalId(Integer id) {
        VirtualDAO dao = DAOFactory.getDAOFactory().getVirtualDAO();
        Collection resources =
            dao.findVirtualByPysicalId(id, AuthzConstants.serviceResType);
        List services = new ArrayList();
        for (Iterator it = resources.iterator(); it.hasNext(); ) {
            Virtual virt = (Virtual) it.next();
            services.add(findById(virt.getId()));
        }
        return services;
    }
    
    public List getServiceTypeCounts() {
        String sql = "select t.name, count(*) from ServiceType t, " + 
                     "Service s where s.serviceType = t " + 
                     "group by t.name order by t.name";
        
        return getSession().createQuery(sql).list();
    }
}
