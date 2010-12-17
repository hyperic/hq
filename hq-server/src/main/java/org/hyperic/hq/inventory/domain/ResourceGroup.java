package org.hyperic.hq.inventory.domain;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import org.hyperic.hq.authz.server.session.Role;
import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.annotation.GraphProperty;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.api.Direction;

@Entity
@NodeEntity(partial = true)
public class ResourceGroup extends Resource {
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
}
