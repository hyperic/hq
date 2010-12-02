package org.hyperic.hq.inventory.domain;

import java.util.List;
import java.util.Set;


import javax.persistence.Entity;
import javax.persistence.ManyToMany;

import org.hyperic.hq.reference.RelationshipTypes;
import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.api.Direction;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;

@Entity
@NodeEntity(partial=true)
public class ResourceGroup extends Resource {

    @javax.annotation.Resource
    transient FinderFactory finderFactory;
    
    @RelatedTo(type = RelationshipTypes.CONTAINS, direction = Direction.OUTGOING, elementClass = Resource.class)
    @ManyToMany(targetEntity = Resource.class)
    private Set<Resource> members;
    
    public ResourceGroup(Node n) {
        setUnderlyingState(n);
    }


	public void addMember(Resource member) {
        members.add(member);
    }

	public long count() {
        return finderFactory.getFinderForClass(ResourceGroup.class).count();

    }

	public ResourceGroup findById(Long id) {
        return finderFactory.getFinderForClass(ResourceGroup.class).findById(id);

    }

	public static long countResourceGroups() {
        return entityManager().createQuery("select count(o) from ResourceGroup o", Long.class).getSingleResult();
    }

	public static List<ResourceGroup> findAllResourceGroups() {
        return entityManager().createQuery("select o from ResourceGroup o", ResourceGroup.class).getResultList();
    }

	public static ResourceGroup findResourceGroup(Long id) {
        if (id == null) return null;
        return entityManager().find(ResourceGroup.class, id);
    }

	public static ResourceGroup findResourceGroupByName(String name) {
        return new Resource().finderFactory.getFinderForClass(ResourceGroup.class).findByPropertyValue("name", name);
    }

	public static List<ResourceGroup> findResourceGroupEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("select o from ResourceGroup o", ResourceGroup.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }
}
