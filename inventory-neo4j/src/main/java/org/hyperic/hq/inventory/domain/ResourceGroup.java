package org.hyperic.hq.inventory.domain;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser;
import org.springframework.beans.factory.annotation.Configurable;
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
@Configurable
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
        Traverser relationTraverser = getTraverser();
        for (Node related : relationTraverser) {
            memberIds.add((Integer) related.getProperty("id"));
        }
        return memberIds;
    }
    
    private Traverser getTraverser() {
        return getPersistentState().traverse(Traverser.Order.BREADTH_FIRST,
            new StopEvaluator() {
                public boolean isStopNode(TraversalPosition currentPos) {
                    return currentPos.depth() >= 1;
                }
            }, ReturnableEvaluator.ALL_BUT_START_NODE,
            DynamicRelationshipType.withName(RelationshipTypes.HAS_MEMBER), Direction.OUTGOING.toNeo4jDir());
    }

    /**
     * 
     * @param member A potential group member
     * @return true if the Resource is a member of the group
     */
    public boolean isMember(Resource member) {
        Traverser relationTraverser = getTraverser();
        for (Node related : relationTraverser) {
            if(related.equals(member.getPersistentState())) {
                return true;
            }
        }
        return false;
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
