package org.hyperic.hq.inventory.domain;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.hyperic.hq.authz.server.session.Role;
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

    @GraphProperty
    @Transient
    private boolean privateGroup;

  

    @RelatedTo(type = "HAS_ROLE", direction = Direction.OUTGOING, elementClass = Role.class)
    @ManyToMany(targetEntity = Role.class)
    private Set<Role> roles;

    public ResourceGroup() {
    }

    public ResourceGroup(Node n) {
        setUnderlyingState(n);
    }

    public void addMember(Resource member) {
        // TODO confirm actually adds
        members.add(member);
    }

    public void addRole(Role role) {
        //TODO what else?
        roles.add(role);
    }

    public Set<Resource> getMembers() {
        return members;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public boolean isMember(Resource member) {
        return members.contains(member);
    }

    public boolean isPrivateGroup() {
        return privateGroup;
    }

    public void removeMember(Resource member) {
        // TODO confims actually removes
        members.remove(member);
    }

    public void removeRole(Role role) {
      //TODO what else?
        roles.remove(role);
    }

    public void setMembers(Set<Resource> members) {
        this.members = members;
    }

    public void setPrivateGroup(boolean privateGroup) {
        this.privateGroup = privateGroup;
    }
    
    public int getGroupType() {
        //TODO replace with calls to group.getType().getId()
       //Doing this just in case we can't pre-populate ResourceTypes with IDs as expected in AppdefEntityConstants.getAppdefGroupTypeName
        return getType().getId();
    }

    public static int countResourceGroups() {
        return entityManager().createQuery("select count(o) from ResourceGroup o", Integer.class)
            .getSingleResult();
    }

    public static List<ResourceGroup> findAllResourceGroups() {
        return entityManager().createQuery("select o from ResourceGroup o", ResourceGroup.class)
            .getResultList();
    }

    public static ResourceGroup findById(Long id) {
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
}
