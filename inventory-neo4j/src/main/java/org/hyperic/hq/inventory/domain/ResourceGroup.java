package org.hyperic.hq.inventory.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hyperic.hq.auth.domain.Role;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.Direction;
import org.springframework.transaction.annotation.Transactional;

/**
 * A group of {@link Resource}s, which is also a {@link Resource} in order to
 * participate in relationships, etc
 * @author jhickey
 * @author dcrutchfield
 * 
 */
@Entity
@NodeEntity(partial = true)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ResourceGroup
    extends Resource {

    @RelatedTo(type = RelationshipTypes.HAS_MEMBER, direction = Direction.OUTGOING, elementClass = Resource.class)
    @Transient
    private Set<Resource> members;

    @GraphProperty
    @Transient
    private boolean privateGroup;

    @RelatedTo(type = RelationshipTypes.HAS_ROLE, direction = Direction.OUTGOING, elementClass = Role.class)
    @Transient
    private Set<Role> roles;
    
    public ResourceGroup() {
    }

    /**
     * 
     * @param name The name of the Group
     * @param type The group type (i.e. "vApp" or "Cluster")
     */
    public ResourceGroup(String name, ResourceType type) {
        super(name, type);
    }

    /**
     * 
     * @param name The name of the Group
     * @param type The group type (i.e. "vApp" or "Cluster")
     * @param privateGroup true if group is meant to be visible to owner only
     */
    public ResourceGroup(String name, ResourceType type, boolean privateGroup) {
        super(name, type);
        this.privateGroup = privateGroup;
    }

    /**
     * 
     * @param member Add a member to the group
     */
    @Transactional
    public void addMember(Resource member) {
       if(this.members == null) {
           this.members = new HashSet<Resource>();
       }
       members.add(member);
    }

    /**
     * 
     * @param role Add a role to the Group TODO keep this for authorization?
     */
    @Transactional
    public void addRole(Role role) {
       roles.add(role);
    }

    /**
     * 
     * @return The members of this group
     */
    public Set<Resource> getMembers() {
        return members;
    }

    /**
     * 
     * @return The roles associated with this Group
     */
    public Set<Role> getRoles() {
        return roles;
    }

    /**
     * 
     * @param member A potential group member
     * @return true if the Resource is a member of the group
     */
    public boolean isMember(Resource member) {
        return members.contains(member);
    }

    /**
     * 
     * @return true if group is meant to be visible to owner only
     */
    public boolean isPrivateGroup() {
        return privateGroup;
    }

    /**
     * 
     * @param member Remove a group member
     */
    @Transactional
    public void removeMember(Resource member) {
        members.remove(member);
    }

    /**
     * 
     * @param role Remove a role
     */
    @Transactional
    public void removeRole(Role role) {
       roles.remove(role);
    }

    /**
     * 
     * @param privateGroup true if group is meant to be visible to owner only
     */
    public void setPrivateGroup(boolean privateGroup) {
        this.privateGroup = privateGroup;
    }
}
