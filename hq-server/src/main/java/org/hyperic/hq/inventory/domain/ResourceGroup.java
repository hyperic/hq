package org.hyperic.hq.inventory.domain;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;

import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.annotation.GraphProperty;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.api.Direction;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;

@Entity
@NodeEntity(partial = true)
public class ResourceGroup
    extends Resource {

    @javax.annotation.Resource
    transient FinderFactory finderFactory;

    @RelatedTo(type = "HAS_MEMBER", direction = Direction.OUTGOING, elementClass = Resource.class)
    @ManyToMany(targetEntity = Resource.class)
    private Set<Resource> members;

    @RelatedTo(type = "HAS_ROLE", direction = Direction.OUTGOING, elementClass = Role.class)
    @ManyToMany(targetEntity = Role.class)
    private Set<Role> roles;

    @GraphProperty
    private String location;

    @GraphProperty
    private String description;

    @GraphProperty
    private boolean privateGroup;

    public ResourceGroup() {
    }

    public ResourceGroup(Node n) {
        setUnderlyingState(n);
    }

    public void addMember(Resource member) {
        // TODO confirm actually adds
        members.add(member);
    }

    public void removeMember(Resource member) {
        // TODO confims actually removes
        members.remove(member);
    }

    public boolean isMember(Resource member) {
        return members.contains(member);
    }

    public void setMembers(Set<Resource> members) {
        this.members = members;
    }

    public Set<Resource> getMembers() {
        return members;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPrivateGroup() {
        return privateGroup;
    }

    public void setPrivateGroup(boolean privateGroup) {
        this.privateGroup = privateGroup;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public static int countResourceGroups() {
        return entityManager().createQuery("select count(o) from ResourceGroup o", Integer.class)
            .getSingleResult();
    }

    public static List<ResourceGroup> findAllResourceGroups() {
        return entityManager().createQuery("select o from ResourceGroup o", ResourceGroup.class)
            .getResultList();
    }

    public static ResourceGroup findResourceGroup(Integer id) {
        if (id == null)
            return null;
        return entityManager().find(ResourceGroup.class, id);
    }

    public static ResourceGroup findResourceGroupByName(String name) {
        return new Resource().finderFactory.getFinderForClass(ResourceGroup.class)
            .findByPropertyValue("name", name);
    }

    public static List<ResourceGroup> findResourceGroupEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("select o from ResourceGroup o", ResourceGroup.class)
            .setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

    public void setModifiedBy(String name) {
        // TODO remove?
    }

    public void setGroupType(int type) {
        // TODO remove?
    }

    public Integer getGroupType() {
        // TODO remove?
        return 0;
    }

    public boolean isMixed() {
        // TODO remove
        return false;
    }

    public Integer getGroupEntResType() {
        // TODO remove
        return 0;
    }

    public Integer getGroupEntType() {
        // TODO remove
        return 0;
    }

    public ResourceGroupValue getResourceGroupValue() {
        // TODO remove
        return null;
    }

    public Collection findInGroup_orderName(Boolean fSystem) {
        // TODO from ResourceDAO seriously get rid of this
        return null;
    }

    public Resource getResourcePrototype() {
        // TODO remove - was the types of Resources this group can have
        return null;
    }

    public boolean isSystem() {
        // TODO remove
        return false;
    }
    
    public void addRole(Role role) {
        //TODO what else?
        roles.add(role);
    }
    
    public void removeRole(Role role) {
      //TODO what else?
        roles.remove(role);
    }

    public static Collection<ResourceGroup> findByRoleIdAndSystem_orderName(Integer roleId,
                                                                            boolean system,
                                                                            boolean asc) {

        // TODO
        return null;
    }
    
    public static Collection<ResourceGroup> findWithNoRoles_orderName(boolean asc) {
        //TODO
        return null;
    }
    
    public static Collection<ResourceGroup> findByNotRoleId_orderName(Integer roleId, boolean asc) {
        //TODO
        return null;
    }

}
