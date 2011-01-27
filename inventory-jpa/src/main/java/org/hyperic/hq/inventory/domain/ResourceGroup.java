package org.hyperic.hq.inventory.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;

import org.hyperic.hq.authz.server.session.Role;

@Entity
public class ResourceGroup extends Resource {
  
    @ManyToMany(targetEntity = Resource.class)
    private Set<Resource> members= new HashSet<Resource>();
    
    private boolean privateGroup;

    @ManyToMany(targetEntity = Role.class)
    private Set<Role> roles= new HashSet<Role>();

    public ResourceGroup() {
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
    
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ResourceGroup)) {
            return false;
        }
        return this.getId() == ((ResourceGroup) obj).getId();
    }
}
