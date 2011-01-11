package org.hyperic.hq.inventory.domain;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.reference.RelationshipTypes;
import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.annotation.GraphProperty;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.api.Direction;

@Entity
@NodeEntity(partial = true)
public class ResourceGroup extends Resource {
    @RelatedTo(type = RelationshipTypes.HAS_MEMBER, direction = Direction.OUTGOING, elementClass = Resource.class)
    @ManyToMany(targetEntity = Resource.class)
    @Transient
    private Set<Resource> members;

    @GraphProperty
    @Transient
    private boolean privateGroup;

    @RelatedTo(type = RelationshipTypes.HAS_ROLE, direction = Direction.OUTGOING, elementClass = Role.class)
    @ManyToMany(targetEntity = Role.class)
    @Transient
    private Set<Role> roles;

    public ResourceGroup() {
    }

    public ResourceGroup(Node n) {
        setUnderlyingState(n);
    }

    public void addMember(Resource member) {
        members.add(member);
    }

    public void addRole(Role role) {
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
        members.remove(member);
    }

    public void removeRole(Role role) {
        roles.remove(role);
    }

    public void setMembers(Set<Resource> members) {
        this.members = members;
    }
    
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public void setPrivateGroup(boolean privateGroup) {
        this.privateGroup = privateGroup;
    }
}
