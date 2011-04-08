package org.hyperic.hq.inventory.domain;

import java.util.HashSet;
import java.util.Set;

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

@NodeEntity
public class ResourceGroup
    extends Resource {

    @RelatedTo(type = RelationshipTypes.HAS_MEMBER, direction = Direction.OUTGOING, elementClass = Resource.class)
    private Set<Resource> members;

    @GraphProperty
    private boolean privateGroup;

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
    @Transactional("neoTxManager")
    public void addMember(Resource member) {
        if (this.members == null) {
            this.members = new HashSet<Resource>();
        }
        members.add(member);
    }

    /**
     * 
     * @return The members of this group
     */
    public Set<Resource> getMembers() {
        return members;
    }
    
    public Set<Integer> getMemberIds() {
        Set<Integer> memberIds = new HashSet<Integer>();
        for(Resource member: members) {
            memberIds.add(member.getId());
        }
        return memberIds;
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
    @Transactional("neoTxManager")
    public void removeMember(Resource member) {
        members.remove(member);
    }

    /**
     * 
     * @param privateGroup true if group is meant to be visible to owner only
     */
    public void setPrivateGroup(boolean privateGroup) {
        this.privateGroup = privateGroup;
    }
}
