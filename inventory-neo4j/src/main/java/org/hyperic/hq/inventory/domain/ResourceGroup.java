package org.hyperic.hq.inventory.domain;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.reference.RelationshipTypes;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.Direction;

@Entity
@NodeEntity(partial = true)
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class ResourceGroup extends Resource {
    
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
    
    public ResourceGroup(String name, ResourceType type) {
        super(name, type);
    }
    
    public ResourceGroup(String name, ResourceType type, boolean privateGroup) {
        super(name, type);
        this.privateGroup = privateGroup;
    }
    
    public void addMember(Resource member) {
        relateTo(member,DynamicRelationshipType.withName(RelationshipTypes.HAS_MEMBER));
    }

    public void addRole(Role role) {
        relateTo(role,DynamicRelationshipType.withName(RelationshipTypes.HAS_ROLE));
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
       removeRelationshipTo(member, RelationshipTypes.HAS_MEMBER);
    }

    public void removeRole(Role role) {
        removeRelationshipTo(role, RelationshipTypes.HAS_ROLE);
    }

    public void setPrivateGroup(boolean privateGroup) {
        this.privateGroup = privateGroup;
    }
}
