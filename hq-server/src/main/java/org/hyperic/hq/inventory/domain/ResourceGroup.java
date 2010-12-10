package org.hyperic.hq.inventory.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.shared.ResourceGroupValue;

public class ResourceGroup extends Resource {
    private Set<Resource> members;
    private String location;
    private String description;
    private boolean privateGroup;

    public ResourceGroup() {
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
        return null; //roles;
    }

    public static int count() {
    	return 0;
    }

    public static List<ResourceGroup> findAllResourceGroups() {
    	return new ArrayList<ResourceGroup>();
    }

    public static ResourceGroup findById(Integer id) {
    	return new ResourceGroup();
    }

    public static ResourceGroup findResourceGroupByName(String name) {
    	return new ResourceGroup();
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
        // roles.add(role);
    }
    
    public void removeRole(Role role) {
      //TODO what else?
      // roles.remove(role);
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