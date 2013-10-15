/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.GroupCreationException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.util.Transformer;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ResourceGroupDAO
    extends HibernateDAO<ResourceGroup> {
    private static final Log _log = LogFactory.getLog(ResourceGroupDAO.class.getName());
    private static final Integer rootResourceGroupId = AuthzConstants.rootResourceGroupId;

    @Autowired
    private ResourceDAO rDao;

    @Autowired
    private ResourceTypeDAO resourceTypeDAO;

    @Autowired
    public ResourceGroupDAO(SessionFactory sessionFactory) {
        super(ResourceGroup.class, sessionFactory);
    }

    private void assertNameConstraints(String name) throws GroupCreationException {
        if ((name == null) || (name.length() == 0) || (name.length() > 100)) {
            throw new GroupCreationException("Group name must be between "
                                             + "1 and 100 characters in length");
        }
    }

    private void assertDescriptionConstraints(String desc) throws GroupCreationException {
        if ((desc != null) && (desc.length() > 100)) {
            throw new GroupCreationException("Group description must be "
                                             + "between 1 and 100 characters in length");
        }
    }

    private void assertLocationConstraints(String loc) throws GroupCreationException {
        if ((loc != null) && (loc.length() > 100)) {
            throw new GroupCreationException("Group location must be "
                                             + "between 1 and 100 characters in length");
        }
    }

    /**
     * @param groupResource - Typically this param is null and the behavior is that a resource is create of authzGroup
     *  type. Only used when associating a group with an existing resource.
     */
    ResourceGroup create(AuthzSubject creator, ResourceGroupCreateInfo cInfo,
                         Collection<Resource> resources, Collection<Role> roles, Resource groupResource)
        throws GroupCreationException {
        if (groupResource == null) {
            // name is not persisted if the groupResource != null
            assertNameConstraints(cInfo.getName());
        }
        assertDescriptionConstraints(cInfo.getDescription());
        assertLocationConstraints(cInfo.getLocation());
        switch (cInfo.getGroupType()) {
            case AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP:
            case AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP:
            case AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS:
                if (cInfo.getResourcePrototype() != null) {
                    throw new GroupCreationException("Cannot specify a prototype for mixed groups");
                }
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS:
            case AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC:
                if (cInfo.getResourcePrototype() == null) {
                    throw new GroupCreationException("Compatable groups must specify a prototype");
                }
                break;
        }
        ResourceGroup resGrp = cInfo.getResourceGroup(creator);
        ResourceType resType = resourceTypeDAO.findById(AuthzConstants.authzGroup);
        assert resType != null;
        final Resource proto = rDao.findById(AuthzConstants.rootResourceId);
        Resource r = null;
        if (groupResource == null) {
            r = cInfo.isPrivateGroup() ?
                rDao.createPrivate(resType, proto, cInfo.getName(), creator, resGrp.getId(), cInfo.isSystem()) :
                rDao.create(resType, proto, cInfo.getName(), creator, resGrp.getId(), cInfo.isSystem());
        } else {
            r = groupResource;
        }
        resGrp.setResource(r);
        save(resGrp);
        /*
         * The following oddity is needed because the above rDao.create()
         * flushes the session. If we don't refresh the object, then changing
         * the instanceId here doens't seem to do anything. This is definitely a
         * hacky workaround for a Hibernate issue.
         */
        r = rDao.findById(r.getId());
        getSession().refresh(r);
        if (groupResource == null) {
            r.setInstanceId(resGrp.getId());
        }
        getSession().saveOrUpdate(r);
        flushSession();
        setMembers(resGrp, new HashSet<Resource>(resources));
        resGrp.setRoles(new HashSet<Role>(roles));
        return resGrp;
    }

    ResourceGroup findResourceGroup(Resource resource) {
        final String hql = "from ResourceGroup where resource = :resource";
        return (ResourceGroup) createQuery(hql).setParameter("resource", resource).uniqueResult();
    }

    void removeAllMembers(ResourceGroup group) {
        // Don't want to mark the Root Resource Group dirty to avoid optimistic
        // locking issues. Since the root group is associated with all
        // resources, transactions which involve creating/deleting resources
        // are not self-contained and therefore any changes to this object
        // would make these types of transactions potentially fail.
        if (!group.getId().equals(rootResourceGroupId)) {
            group.markDirty();
        }
        createQuery("delete from GroupMember g " + "where g.group = :group").setParameter("group",
            group).executeUpdate();
    }

    boolean isMember(ResourceGroup group, Resource resource) {
        GroupMember gm = (GroupMember) createQuery(
            "from GroupMember g where g.group = :group " + " and g.resource = :resource")
            .setParameter("group", group).setParameter("resource", resource).uniqueResult();

        return gm != null;
    }

    @SuppressWarnings("unchecked")
    void removeMembers(ResourceGroup group, Collection<Resource> members) {
        if (group == null || members == null || members.isEmpty()) {
            return;
        }
        // Don't want to mark the Root Resource Group dirty to avoid optimistic
        // locking issues. Since the root group is associated with all
        // resources, transactions which involve creating/deleting resources
        // are not self-contained and therefore any changes to this object
        // would make these types of transactions potentially fail.
        if (!group.getId().equals(rootResourceGroupId)) {
            group.markDirty();
        }
        final List<Integer> memberIds = new Transformer<Resource, Integer>() {
            @Override
            public Integer transform(Resource r) {
                return r.getId();
            }
        }.transform(members);
        final Query query = createQuery("from GroupMember where group = :group and resource.id in (:members)");
        final int size = memberIds.size();
        final List<GroupMember> toDelete = new ArrayList<GroupMember>();
        for (int i=0; i<size; i+=BATCH_SIZE) {
            final int end = Math.min(i+BATCH_SIZE, size);
            final List<Integer> list = memberIds.subList(i, end);
            toDelete.addAll(query.setParameter("group", group)
                 .setParameterList("members", list)
                 .list());
        }
        for (final GroupMember member : toDelete) {
            getSession().delete(member);
        }
    }

    void addMember(ResourceGroup group, Resource resource) {
        addMembers(group, Collections.singleton(resource));
    }

    void addMembers(ResourceGroup group, Collection<Resource> resources) {
        Session sess = getSession();

        // Don't want to mark the Root Resource Group dirty to avoid optimistic
        // locking issues. Since the root group is associated with all
        // resources, transactions which involve creating/deleting resources
        // are not self-contained and therefore any changes to this object
        // would make these types of transactions potentially fail.
        if (!group.getId().equals(rootResourceGroupId)) {
            group.markDirty();
        }
        for (Resource r : resources) {
            GroupMember m = new GroupMember(group, r);

            sess.save(m);
        }
    }

    void setMembers(ResourceGroup group, Collection<Resource> resources) {
        removeAllMembers(group);
        addMembers(group, resources);
    }

    /**
     * Get groups that a resource belongs to via the persistence mechanism (i.e.
     * mapping table)
     * 
     * @return {@link ResourceGroup}s
     */
    @SuppressWarnings("unchecked")
    Collection<ResourceGroup> getGroups(Resource r) {
        return createQuery(
            "select g.group from GroupMember g " + "where g.resource = :resource").setParameter(
            "resource", r).list();
    }

    /**
     * Get resources belonging to a group via the persistence mechanism.
     * 
     * @return {@link Resource}s
     */
    int getNumMembers(ResourceGroup g) {
        String hql = "select count(g.resource) from GroupMember g " +
                     "where g.group = :group and g.resource.resourceType is not null";
        return ((Number) createQuery(hql).setParameter("group", g).uniqueResult()).intValue();
    }

    /**
     * Get resources belonging to a group via the persistence mechanism.
     * 
     * @return {@link Resource}s
     */
    @SuppressWarnings("unchecked")
    List<Resource> getMembers(ResourceGroup g) {
        String hql = "select g.resource from GroupMember g " +
                     "where g.group = :group and g.resource.resourceType is not null order by g.resource.name";
        return createQuery(hql).setParameter("group", g).list();
    }

    @SuppressWarnings("unchecked")
    List<Resource> getMembers(Collection<ResourceGroup> groups) {
        if ((groups == null) || groups.isEmpty()) {
            return Collections.emptyList();
        }
        final String hql = "select g.resource.id from GroupMember g where g.group in (:groups)";
        final List<Integer> resourceIds = createQuery(hql).setParameterList("groups", groups).list();
        final List<Resource> rtn = new ArrayList<Resource>(resourceIds.size());
        for (final Integer resourceId : resourceIds) {
            final Resource resource = rDao.get(resourceId);
            if ((resource == null) || resource.isInAsyncDeleteState()) {
                continue;
            }
            rtn.add(resource);
        }
        return rtn;
    }

    /**
     * Get counts of resources mapped by type name.
     * 
     * @return {@link Resource}s
     */
    @SuppressWarnings("unchecked")
    Map<String, Number> getMemberTypes(ResourceGroup g) {
        List<Object[]> counts = createQuery(
            "select p.name, count(r) from GroupMember g " + "join g.resource r "
                + "join r.prototype p " + "where g.group = :group group by p.name").setParameter(
            "group", g).list();
        Map<String, Number> types = new HashMap<String, Number>();
        for (Object[] objs : counts) {
            types.put((String) objs[0], (Number) objs[1]);
        }
        return types;
    }

    @Override
    public void remove(ResourceGroup entity) {
        remove(entity, false);
    }

    /**
     * @param removeResource true when the group is associated with a resource that belongs to another first class
     * entity
     */
    void remove(ResourceGroup entity, boolean removeResource) {
        // remove all roles
        entity.getRoles().clear();
        removeAllMembers(entity);
        Resource res = entity.getResource();
        // remove this resourceGroup itself
        super.remove(entity);
        flushSession();
        if (removeResource) {
            rDao.remove(res);
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
        String sql = "from ResourceGroup g where lower(g.resource.name) = lower(?) " +
                     "AND g.resource.resourceType.id = :groupType";
        return (ResourceGroup) getSession().createQuery(sql)
                                           .setString(0, name).setCacheable(true)
                                           .setInteger("groupType", AuthzConstants.authzGroup)
                                           .setCacheable(true)
                                           .setCacheRegion("ResourceGroup.findByName")
                                           .uniqueResult();
    }
    
    @SuppressWarnings("unchecked")
    public Collection<ResourceGroup> findByRoleIdAndSystem_orderName(Integer roleId,
                                                                     boolean system, boolean asc) {
        String sql = "select g from ResourceGroup g join g.roles r " +
                     "where r.id = ? and g.system = ? " + "order by g.resource.sortName " +
                     (asc ? "asc" : "desc");
        return getSession().createQuery(sql).setInteger(0,
            roleId.intValue()).setBoolean(1, system).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<ResourceGroup> findByRolesAndGroupTypeAndSystem(Collection<Role> roles, int groupType, boolean system) {
        
        if ((roles==null) || roles.isEmpty()){
            return Collections.emptyList();
        }

        String sql = "select g from ResourceGroup g join g.roles r " +
                     "where r in (:roles) and g.groupType = :type and g.system = :system ";
        return getSession().createQuery(sql)
                .setParameterList("roles", roles)
                .setInteger("type", groupType)
                .setBoolean("system", system).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<ResourceGroup> findWithNoRoles_orderName(boolean asc) {
        String sql = "from ResourceGroup g " + "where g.roles.size = 0 and g.system = false " +
                     "order by g.resource.sortName " + (asc ? "asc" : "desc");
        return getSession().createQuery(sql).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<ResourceGroup> findByNotRoleId_orderName(Integer roleId, boolean asc) {
        return getSession().createQuery(
            "from ResourceGroup g " + "where ? not in (select id from g.roles) and " +
                "g.system = false order by g.resource.sortName " + (asc ? "asc" : "desc"))
            .setInteger(0, roleId.intValue()).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<ResourceGroup> findCompatible(Resource proto) {
        String sql = "from ResourceGroup g " + "where g.resourcePrototype = ? and "
                     + "(g.groupType = ? or g.groupType = ?)";

        return getSession().createQuery(sql).setParameter(0, proto)
            .setInteger(1, AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS).setInteger(2,
                AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC).list();
    }

   

    @SuppressWarnings("unchecked")
    PageList<ResourceGroup> findGroupsClusionary(AuthzSubject subject, Resource member,
                                                 Resource prototype,
                                                 Collection<ResourceGroup> excludeGroups,
                                                 PageInfo pInfo, boolean inclusive, boolean includeDynamicGroup) {
        ResourceGroupSortField sort = (ResourceGroupSortField) pInfo.getSort();
        String hql = "from ResourceGroup g where g.system = false and ";

        if (prototype != null) {
            hql += " (g.resourcePrototype = :proto ";

            // Mixed groups, too

            Integer protoType = prototype.getResourceType().getId();
            if (protoType.equals(AuthzConstants.authzPlatformProto) ||
                protoType.equals(AuthzConstants.authzServerProto) ||
                protoType.equals(AuthzConstants.authzServiceProto)) {
                hql += " or g.groupType = " + AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS;
            } else if (protoType.equals(AuthzConstants.authzApplicationProto)) {
                hql += " or g.groupType = " + AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP;
            }

            hql += ") and ";
        }

        List<Integer> excludes = new ArrayList<Integer>(excludeGroups.size());
        for (ResourceGroup g : excludeGroups) {
            excludes.add(g.getId());
        }
        if (!excludes.isEmpty()) {
            hql += " g.id not in (:excludes) and ";
        }

        String inclusionStr = "";
        if (!inclusive) {
            inclusionStr = " not ";
        }

        PermissionManager pm = PermissionManagerFactory.getInstance();
        hql += inclusionStr + " exists ( " + " select m.id from GroupMember m " +
               " where m.resource = :resource and m.group = g " + ") ";

        String pmql = pm.getOperableGroupsHQL(subject, "g",
            inclusive ? AuthzConstants.groupOpViewResourceGroup
                     : AuthzConstants.groupOpModifyResourceGroup);

        if (pmql.length() > 0) {
            hql += pmql;
        }

        if (!includeDynamicGroup) {
            hql += " and not g.groupType = " + AppdefEntityConstants.APPDEF_TYPE_GROUP_DYNAMIC;
        }

        String countHql = "select count(g.id) " + hql;
        String actualHql = "select g " + hql + " order by " + sort.getSortString("g");

        Query q = getSession().createQuery(countHql).setParameter("resource", member);

        if (!excludes.isEmpty()) {
            q.setParameterList("excludes", excludes);
        }

        if (prototype != null) {
            q.setParameter("proto", prototype);
        }

        if (pmql.length() > 0) {
            q.setInteger("subjId", subject.getId().intValue());
        }

        int total = ((Number) (q.uniqueResult())).intValue();
        q = getSession().createQuery(actualHql).setParameter("resource", member);

        if (prototype != null) {
            q.setParameter("proto", prototype);
        }

        if (!excludes.isEmpty()) {
            q.setParameterList("excludes", excludes);
        }

        if (pmql.length() > 0) {
            q.setInteger("subjId", subject.getId().intValue());
        }

        List<ResourceGroup> vals = pInfo.pageResults(q).list();
        return new PageList<ResourceGroup>(vals, total);
    }

    @SuppressWarnings("unchecked")
    public Collection<ResourceGroup> findByGroupType(int groupType) {
        String sql = "from ResourceGroup g where g.groupType = :type";
        return getSession().createQuery(sql).setInteger("type", groupType).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<ResourceGroup> findByGroupType_orderName(boolean isAscending, int groupType) {
        String sql = "from ResourceGroup g where g.groupType = :type" +
                     " ORDER BY g.resource.name " + ((isAscending) ? "asc" : "desc");
        return getSession().createQuery(sql).setInteger("type",
            groupType).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<ResourceGroup> findDeletedGroups() {
        String hql = "from ResourceGroup where resource.resourceType = null";
        return createQuery(hql).list();
    }
    
    @SuppressWarnings("unchecked")
    List<ResourceGroup> getGroupsByType(int groupType) {
        String hql = "from ResourceGroup where groupType = :type";
        return createQuery(hql).setInteger("type", groupType).list();
    }

    @SuppressWarnings("unchecked")
    public List<ResourceGroup> getGroupsByTypeAndOwners(int groupType, Collection<AuthzSubject> owners) {
        
        if ((owners==null) || owners.isEmpty()){
            return Collections.emptyList();
        }
        
        String hql = "select g from ResourceGroup g join g.resource r " + 
                     " where groupType = :type and r.owner in (:owners)";
        return createQuery(hql)
                .setInteger("type", groupType)
                .setParameterList("owners", owners).list();
    }

    @SuppressWarnings("unchecked")
    Collection<GroupMember> getOrphanedResourceGroupMembers() {
        String hql = new StringBuilder(512)
            .append("from GroupMember g where exists (")
                .append("SELECT 1 FROM Resource r WHERE r.resourceType.id = :platformType AND g.resource != r ")
                .append("AND r.instanceId not in (select p.id from Platform p)")
            .append(") OR exists (")
                .append("SELECT 1 FROM Resource r WHERE r.resourceType.id = :serverType AND g.resource != r ")
                .append("AND r.instanceId not in (select s.id from Server s)")
            .append(") OR exists (")
                .append("SELECT 1 FROM Resource r WHERE r.resourceType.id = :serviceType AND g.resource != r ")
                .append("AND r.instanceId not in (select s.id from Service s)")
            .append(")")
            .toString();
        return createQuery(hql)
            .setInteger("platformType", AuthzConstants.authzPlatform)
            .setInteger("serverType", AuthzConstants.authzServer)
            .setInteger("serviceType", AuthzConstants.authzService)
            .list();
    }

}
