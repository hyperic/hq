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

package org.hyperic.hq.authz.server.session;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;

public class ResourceGroupDAO extends HibernateDAO
{
    public ResourceGroupDAO(DAOFactory f) {
        super(ResourceGroup.class, f);
    }

    public ResourceGroup create(AuthzSubject creator,
                                ResourceGroupCreateInfo cInfo)
    {
        ResourceGroup resGrp = new ResourceGroup(cInfo, creator);

        ResourceType resType = new ResourceTypeDAO(DAOFactory.getDAOFactory())
            .findById(AuthzConstants.authzGroup);
        
        if (resType == null) {
            throw new SystemException("ResourceType not found " +
                                      AuthzConstants.groupResourceTypeName);
        }

        ResourceDAO rDao = new ResourceDAO(DAOFactory.getDAOFactory());
        Resource proto = rDao.findById(AuthzConstants.rootResourceId);
        Resource r = rDao.create(resType, proto, resGrp.getName(), creator,  
                                 resGrp.getId(), cInfo.getSystem());

        resGrp.setResource(r);
        save(resGrp);
        
        /* The following oddity is needed because the above rDao.create()
         * flushes the session.  If we don't refresh the object, then 
         * changing the instanceId here doens't seem to do anything.  This
         * is definitely a hacky workaround for a Hibernate issue. 
         */
        r = rDao.findById(r.getId());
        getSession().refresh(r);
        r.setInstanceId(resGrp.getId());
        save(r);
        flushSession();
        return resGrp;
    }

    public ResourceGroup findById(Integer id) {
        return (ResourceGroup) super.findById(id);
    }

    public ResourceGroup get(Integer id) {
        return (ResourceGroup) super.get(id);
    }

    public void save(ResourceGroup entity) {
        super.save(entity);
    }

    public void remove(ResourceGroup entity) {
        // remove all roles
        entity.getRoles().clear();
        // remove all resources
        entity.getResourceSet().clear();

        super.remove(entity);
        flushSession();
        // remove this resourceGroup itself
        ResourceDAO dao = new ResourceDAO(DAOFactory.getDAOFactory());
        Resource resource =
            dao.findByInstanceId(AuthzConstants.authzGroup, entity.getId());
        dao.remove(resource);
        flushSession();
    }
    
    public void addResource(ResourceGroup entity, Resource res) {
        entity.getResources().add(res);
    }
    
    public void removeAllResources(ResourceGroup entity) {
        entity.getResourceSet().clear();
    }

    public void removeResources(ResourceGroup entity, Collection resources) {
        Collection resCol = entity.getResourceSet();
        
        for (Iterator i=resources.iterator(); i.hasNext(); ) {
            resCol.remove(i.next());
        }
    }

    public ResourceGroup findRootGroup() {
        ResourceGroup res = findByName(AuthzConstants.rootResourceGroupName);
        
        if (res == null) {
            throw new SystemException("Root group should exist");
        }
        return res;
    }
        
    public ResourceGroup findByName(String name) {            
        String sql = "from ResourceGroup where lower(name) = lower(?)";
        return (ResourceGroup)getSession().createQuery(sql)
            .setString(0, name)
            .setCacheable(true)
            .setCacheRegion("ResourceGroup.findByName")
            .uniqueResult();
    }
    
    public Collection findByRoleIdAndSystem_orderName(Integer roleId,
                                                         boolean system,
                                                         boolean asc) {            
        String sql = "select g from ResourceGroup g join fetch g.roles r " +
                     "where r.id = ? and g.system = ? order by g.sortName " +
                     (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, roleId.intValue())
            .setBoolean(1, system)
            .list();
    }

    public Collection findWithNoRoles_orderName(boolean asc) {            
        String sql = "from ResourceGroup g " +
                     "where g.roles.size = 0 and g.system = false " +
                     "order by sortName " + (asc ? "asc" : "desc");
        return getSession().createQuery(sql).list();
    }

    public Collection findByNotRoleId_orderName(Integer roleId, boolean asc)
    {
        return getSession()
            .createQuery("from ResourceGroup g " +
                         "where ? not in (select id from g.roles) and " +
                         "g.system = false order by g.sortName " +
                         (asc ? "asc" : "desc"))
            .setInteger(0, roleId.intValue())
            .list();
    }

    public Collection findContaining_orderName(Integer instanceId,
                                               Integer typeId,
                                               boolean asc)
    {

        String sql="select distinct rg from ResourceGroup rg " +
                   "join fetch rg.resourceSet r " +
                   "where r.instanceId = ? and  r.resourceType.id = ? " +
                   "order by rg.sortName " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, instanceId.intValue())
            .setInteger(1, typeId.intValue())
            .list();
    }

    public Collection findContaining(Resource r) {
        String sql = "select distinct rg from ResourceGroup rg " +
                   "join fetch rg.resourceSet r " +
                   "where r.instanceId = ? and  r.resourceType.id = ?";
        return getSession().createQuery(sql)
            .setInteger(0, r.getInstanceId().intValue())
            .setInteger(1, r.getResourceType().getId().intValue())
            .list();
    }

    public Collection findCompatible(Resource proto) {
        String sql =
            "from ResourceGroup g " +
            "where g.resourcePrototype = ? and " +
            "(g.groupType = ? or g.groupType = ?)";

        return getSession().createQuery(sql)
            .setParameter(0, proto)
            .setInteger(1, AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS)
            .setInteger(2, AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC)
            .list();
    }

    /**
     * Return the maximum collection interval for the given template within
     * the group.
     *
     * @param g The group in question.
     * @param templateId The measurement template to query.
     * @return templateId The maximum collection time in milliseconds.
     */
    public Long getMaxCollectionInterval(ResourceGroup g, Integer templateId) {
        String sql =
            "select max(m.interval) from Measurement m, " +
            "ResourceGroup g join g.resourceSet r " +
            "where m.instanceId = r.instanceId and "+
            "g = ? and m.template.id = ?";

        return (Long)getSession().createQuery(sql)
            .setParameter(0, g)
            .setInteger(1, templateId.intValue())
            .setCacheable(true)
            .setCacheRegion("ResourceGroup.getMaxCollectionInterval")
            .uniqueResult();
    }

    /**
     * Return a List of Measurements that are collecting for the given
     * template ID and group.
     *
     * @param g The group in question.
     * @param templateId The measurement template to query.
     * @return templateId A list of Measurement objects with the given template
     * id in the group that are set to be collected.
     */
    public List getMetricsCollecting(ResourceGroup g, Integer templateId) {
        String sql =
            "select m from Measurement m, " +
            "ResourceGroup g join g.resourceSet r " +
            "where m.instanceId = r.instanceId and "+
            "g = ? and m.template.id = ? and m.enabled = true";

        return getSession().createQuery(sql)
            .setParameter(0, g)
            .setInteger(1, templateId.intValue())
            .setCacheable(true)
            .setCacheRegion("ResourceGroup.getMetricsCollecting")
            .list();
    }
}
