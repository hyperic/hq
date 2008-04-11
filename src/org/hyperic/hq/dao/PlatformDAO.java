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
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.Virtual;
import org.hyperic.hq.authz.shared.AuthzConstants;

public class PlatformDAO extends HibernateDAO {

    public PlatformDAO(DAOFactory f) {
        super(Platform.class, f);
    }

    public Platform findById(Integer id) {
        return (Platform)super.findById(id);
    }

    public Platform get(Integer id) {
        return (Platform)super.get(id);
    }

    public void save(Platform entity) {
        super.save(entity);
        getSession().flush();
    }

    public void remove(Platform entity) {
        super.remove(entity);
    }
    
    public void remove(Ip ip) {
        super.remove(ip);
    }
 
    /**
     * A method to update a platform based on a PlatformValue object
     * Ideally, this should be done via the xdoclet generated setPlatformValue
     * method, however, since this method is generated incorrectly, and doesnt
     * support CMR's reliably, I'm rolling my own here.
     * IMPORTANT: due to a bug in the value objects, this method expects any
     * IP's you wish to save (even existing ones) to be inside the "addedIpValues"
     * collection. This means you should removeAllIpValues(), then add them
     * individually. This is a workaround until the xdoclet stuff is made to work.
     *
     * Legacy code from EJB entity bean.  All this logic should move close
     * to the modification source.  Should pass the pojo directly instead
     * of using Platform Value object.
     *
     * @param existing - a platform value object.
     */
    public void updatePlatform(PlatformValue existing)
    {
        // reassociate platform
        Platform platform = findById(existing.getId());
        
        // retrieve current list of ips
        Collection curips = platform.getIps();
        if (curips == null) {
            curips = new ArrayList();
            platform.setIps(curips);
        }

        // first remove any which were in the removedIp collection
        for(Iterator i=existing.getRemovedIpValues().iterator(); i.hasNext();) {
            IpValue aIp = (IpValue)i.next();
            if(aIp.idHasBeenSet()) {
                removeAIp(curips, aIp);
            }
        }
        Collection ips = existing.getAddedIpValues();
        // now get any ips which were in the ipValues array
        for(int i = 0; i < existing.getIpValues().length; i++) {
            IpValue aIp = existing.getIpValues()[i];
            if(!(ips.contains(aIp))) {
                ips.add(aIp);
            }
        }
        for(Iterator i = ips.iterator(); i.hasNext();) {
            IpValue aIp = (IpValue)i.next();
            if(aIp.idHasBeenSet()) {
                // update the ejb
                updateAIp(curips, aIp);
            } else {
                // looks like its a new one
                Ip nip = new Ip();
                nip.setIpValue(aIp);
                nip.setPlatform(platform);
                curips.add(nip);
            }
        }
        // finally update the platform ejb
        platform.setPlatformValue(existing);

        // if there is a agent
        if (existing.getAgent() != null)
        {
            // get the agent token and set the agent tp the platform
            AgentDAO agentLHome = DAOFactory.getDAOFactory().getAgentDAO();
            Agent agentLocal = agentLHome.findById(
                existing.getAgent().getId());
            platform.setAgent(agentLocal);
        }
        save(platform);
        // it is a good idea to
        // flush the Session here
        getSession().flush();
    }

    private void removeAIp(Collection coll, IpValue ipv)
    {
        for(Iterator i = coll.iterator(); i.hasNext();) {
            Ip ip = (Ip)i.next();
            if (ip.getId().equals(ipv.getId())) {
                i.remove();
                remove(ip);
                return;
            }
        }
    }

    private void updateAIp(Collection coll, IpValue ipv)
    {
        for(Iterator i = coll.iterator(); i.hasNext();) {
            Ip ip = (Ip)i.next();
            if (ip.getId().equals(ipv.getId())) {
                ip.setIpValue(ipv);
                return;
            }
        }
    }

    public Platform findByFQDN(String fqdn)
    {
        String sql = "from Platform where lower(fqdn)=?";
        return (Platform)getSession()
            .createQuery(sql)
            .setString(0, fqdn.toLowerCase())
            .uniqueResult();
    }

    public Collection findByNameOrFQDN(String name, String fqdn)
    {
        String sql = "from Platform where sortName=? or lower(fqdn)=?";
        return getSession()
            .createQuery(sql)
            .setString(0, name.toUpperCase())
            .setString(1, fqdn.toLowerCase())
            .list();
    }

    public Collection findAll_orderName(boolean asc)
    {
        return createCriteria()
            .addOrder(asc ? Order.asc("sortName"): Order.desc("sortName"))
            .setCacheable(true)
            .setCacheRegion("Platform.findAll_orderName")
            .list();
    }

    public Collection findAll_orderCTime(boolean asc) {
        return createCriteria()
            .addOrder(asc ? Order.asc("creationTime"):
                            Order.desc("creationTime"))
            .list();
    }

    public Collection findByCTime(long ctime) {
        return createCriteria()
            .add(Restrictions.gt("creationTime", new Long(ctime)))
            .addOrder(Order.desc("creationTime"))
            .list();
    }

    public Platform findByName(String name) {
        String sql = "from Platform where name=?";
        return (Platform)getSession()
            .createQuery(sql)
            .setString(0, name)
            .uniqueResult();
    }

    public Platform findBySortName(String name) {
        String sql = "from Platform where sortName=?";
        return (Platform)getSession()
            .createQuery(sql)
            .setString(0, name.toUpperCase())
            .uniqueResult();
    }
    
    public Collection findByType(Integer pid)
    {
        String sql = "select distinct p from Platform p "+
                     "where p.platformType.id=?";
        return getSession()
            .createQuery(sql)
            .setInteger(0, pid.intValue())
            .list();
    }

    public List findByServers(Integer[] ids)
    {
        return createQuery("select distinct p from Platform p " +
        		"join p.servers s where s.id in (:ids) order by p.sortName")
            .setParameterList("ids", ids)
            .list();
    }

    public Platform findByServiceId(Integer id)
    {
        String sql = "select distinct p from Platform p " +
                     " join p.servers s " +
                     " join s.services sv " +
                     "where " +
                     " sv.id = ?";
        return (Platform)getSession()
            .createQuery(sql)
            .setInteger(0, id.intValue())
            .uniqueResult();
    }

    public Platform findByCertDN(String dn)
    {
        String sql = "from Platform where certdn = ?";
        return (Platform)getSession()
            .createQuery(sql)
            .setString(0, dn)
            .uniqueResult();
    }

    public Collection findByApplication(Application app)
    {
        String sql = "select distinct p from Platform p " +
                     " join p.servers s " +
                     " join s.services sv " +
                     " join sv.appServices asv " +
                     "where " +
                     " asv.appication.id = ?";
        return getSession()
            .createQuery(sql)
            .setInteger(0, app.getId().intValue())
            .list();
    }

    public Collection findByAgent(Agent agt)
    {
        String sql = "from Platform where agent.id=?";
        return getSession()
            .createQuery(sql)
            .setInteger(0, agt.getId().intValue())
            .list();
    }

    public Collection findByAgentToken(String token)
    {
        String sql = "select p from Platform p join fetch p.agent a " +
                     "where a.agentToken=?";
        return getSession()
            .createQuery(sql)
            .setString(0, token)
            .list();
    }

    public Collection findByIpAddr(String addr)
    {
        // here we do not to use 'fetch' join as fetch joins have a
        // side effect of also initializing the collection with the
        // result ips retrieved from the query.  That is the ips collection
        // in the 'fetch' join will have only one entry in this instance.
        // That entry being the the row from eam_ip table with eam_ip.address
        // to the 'addr' passed to this method.
        String sql = "select distinct p from Platform p " +
                     "join p.ips ip where ip.address=?";
        return getSession()
            .createQuery(sql)
            .setString(0, addr)
            .list();
    }

    public Resource findVirtualByInstanceId(Integer id) {
        VirtualDAO dao = DAOFactory.getDAOFactory().getVirtualDAO(); 
            
        return dao.findVirtualByInstanceId(id, AuthzConstants.platformResType);
    }

    public Collection findVirtualByProcessId(Integer id) {
        VirtualDAO dao = DAOFactory.getDAOFactory().getVirtualDAO(); 
        Collection resources =
            dao.findVirtualByProcessId(id, AuthzConstants.platformResType);
        List platforms = new ArrayList();
        for (Iterator it = resources.iterator(); it.hasNext(); ) {
            Virtual virt = (Virtual) it.next();
            platforms.add(findById(virt.getId()));
        }
        return platforms;
    }

    public Collection findVirtualByPhysicalId(Integer id) {
        VirtualDAO dao = DAOFactory.getDAOFactory().getVirtualDAO(); 
        Collection resources =
            dao.findVirtualByPysicalId(id, AuthzConstants.platformResType);
        List platforms = new ArrayList();
        for (Iterator it = resources.iterator(); it.hasNext(); ) {
            Virtual virt = (Virtual) it.next();
            platforms.add(findById(virt.getId()));
        }
        return platforms;
    }
    
    public List getPlatformTypeCounts() {
        String sql = "select t.name, count(*) from PlatformType t, " + 
                     "Platform p where p.platformType = t " + 
                     "group by t.name order by t.name";
        
        return getSession().createQuery(sql).list();
    }
    
    public Number getPlatformCount() {
        return (Number)getSession().createQuery("select count(*) from Platform")
            .uniqueResult();
    }
}
