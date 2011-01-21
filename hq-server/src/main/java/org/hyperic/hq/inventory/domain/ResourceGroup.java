package org.hyperic.hq.inventory.domain;

import java.util.Set;

import org.hyperic.hq.authz.server.session.Role;

public interface ResourceGroup extends Resource {

    Set<Resource> getMembers();
    
    void addMember(Resource member);
    
    boolean isMember(Resource member);
    
    void removeMember(Resource member);
    
    void addRole(Role role);
    
    void removeRole(Role role);
    
    Set<Role> getRoles();
    
    boolean isPrivateGroup();
    
}
