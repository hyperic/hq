package org.hyperic.hq.inventory.domain;

import java.util.HashSet;
import java.util.Set;

import org.hyperic.hq.inventory.NotUniqueException;
import org.hyperic.hq.inventory.dao.ResourceDao;
import org.hyperic.hq.inventory.dao.ResourceTypeDao;
import org.hyperic.hq.reference.RelationshipTypes;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.core.Direction;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@DirtiesContext
public class ResourceTypeIntegrationTest
    extends BaseInfrastructureTest {

    @Autowired
    private ResourceTypeDao resourceTypeDao;
    
    @Autowired
    private ResourceDao resourceDao;

    private ResourceType store;

    private ResourceType produceDept;

    @Before
    public void setUp() {
        this.store = new ResourceType("Grocery Store");
        resourceTypeDao.persist(store);
        this.produceDept = new ResourceType("Produce Dept");
        resourceTypeDao.persist(produceDept);
        store.relateTo(produceDept, RelationshipTypes.CONTAINS);
    }

    @Test
    public void testAddOperationType() {
        OperationType inventory = new OperationType("inventory");
        store.addOperationType(inventory);
        Set<OperationType> expected = new HashSet<OperationType>(1);
        expected.add(inventory);
        assertEquals(expected, store.getOperationTypes());
    }

    @Test
    public void testAddOperationTypeTwice() {
        OperationType inventory = new OperationType("inventory");
        store.addOperationType(inventory);
        store.addOperationType(inventory);
        Set<OperationType> expected = new HashSet<OperationType>(1);
        expected.add(inventory);
        assertEquals(expected, store.getOperationTypes());
    }

    @Test
    public void testAddPropertyType() {
        PropertyType address = new PropertyType("Address", String.class);
        store.addPropertyType(address);
        Set<PropertyType> expected = new HashSet<PropertyType>(1);
        expected.add(address);
        assertEquals(expected, store.getPropertyTypes());
    }

    @Test
    public void testAddPropertyTypeTwice() {
        PropertyType address = new PropertyType("Address", String.class);
        store.addPropertyType(address);
        store.addPropertyType(address);
        Set<PropertyType> expected = new HashSet<PropertyType>(1);
        expected.add(address);
        assertEquals(expected, store.getPropertyTypes());
    }

    @Test
    public void testGetOperationType() {
        OperationType inventory = new OperationType("inventory");
        store.addOperationType(inventory);
        assertEquals(inventory, store.getOperationType("inventory"));
    }

    @Test
    public void testGetOperationTypeNotExistent() {
        assertNull(store.getOperationType("inventory"));
    }

    @Test
    public void testGetPropertyType() {
        PropertyType address = new PropertyType("Address", String.class);
        store.addPropertyType(address);
        assertEquals(address, store.getPropertyType("Address"));
    }

    @Test
    public void testGetPropertyTypeNotExistent() {
        assertNull(store.getPropertyType("Address"));
    }

    @Test
    public void testGetRelationships() {
        Set<ResourceTypeRelationship> relationships = store.getRelationships();
        assertEquals(1, relationships.size());
        ResourceTypeRelationship relationship = relationships.iterator().next();
        assertEquals(store, relationship.getFrom());
        assertEquals(produceDept, relationship.getTo());
    }

    @Test
    public void testGetRelationshipsNameAndDirection() {
        Set<ResourceTypeRelationship> relationships = store.getRelationships(produceDept,
            RelationshipTypes.CONTAINS, Direction.OUTGOING);
        assertEquals(1, relationships.size());
        ResourceTypeRelationship relationship = relationships.iterator().next();
        assertEquals(store, relationship.getFrom());
        assertEquals(produceDept, relationship.getTo());
    }

    @Test
    public void testGetRelationshipsNameAndDirectionNoRelation() {
        assertTrue(store.getRelationships(produceDept, RelationshipTypes.CONTAINS,
            Direction.INCOMING).isEmpty());
    }

    @Test
    public void testGetRelationshipsFrom() {
        Set<ResourceTypeRelationship> relationships = store.getRelationshipsFrom(RelationshipTypes.CONTAINS);
        assertEquals(1, relationships.size());
        ResourceTypeRelationship relationship = relationships.iterator().next();
        assertEquals(store, relationship.getFrom());
        assertEquals(produceDept, relationship.getTo());
    }

    @Test
    public void testGetRelationshipsTo() {
        Set<ResourceTypeRelationship> relationships = produceDept.getRelationshipsTo(RelationshipTypes.CONTAINS);
        assertEquals(1, relationships.size());
        ResourceTypeRelationship relationship = relationships.iterator().next();
        assertEquals(store, relationship.getFrom());
        assertEquals(produceDept, relationship.getTo());
    }

    @Test
    public void testGetRelationshipTo() {
        ResourceTypeRelationship relationship = produceDept.getRelationshipTo(store,RelationshipTypes.CONTAINS);
        assertEquals(store, relationship.getFrom());
        assertEquals(produceDept, relationship.getTo());
    }
    
    @Test
    public void testGetResourceTypesFrom() {
        Set<ResourceType> resourceTypes = store.getResourceTypesFrom(RelationshipTypes.CONTAINS);
        assertEquals(1, resourceTypes.size());
        assertEquals(produceDept, resourceTypes.iterator().next());
    }
    
    @Test
    public void testGetResourceTypeFrom() {
        ResourceType resourceType = store.getResourceTypeFrom(RelationshipTypes.CONTAINS);
        assertEquals(produceDept, resourceType);
    }
    
    @Test
    public void testGetResourceTypeFromNone() {
        assertNull(produceDept.getResourceTypeFrom(RelationshipTypes.CONTAINS));
    }
    
    @Test(expected=NotUniqueException.class)
    public void testGetResourceTypeFromMultiple() {
        ResourceType dairyDept = new ResourceType("Dairy");
        resourceTypeDao.persist(dairyDept);
        store.relateTo(dairyDept,RelationshipTypes.CONTAINS);
        store.getResourceTypeFrom(RelationshipTypes.CONTAINS);
    }
    
    @Test
    public void testGetResourceTypesTo() {
        Set<ResourceType> resourceTypes = produceDept.getResourceTypesTo(RelationshipTypes.CONTAINS);
        assertEquals(1, resourceTypes.size());
        assertEquals(store, resourceTypes.iterator().next());
    }
    
    @Test
    public void testGetResourceTypeTo() {
        ResourceType resourceType = produceDept.getResourceTypeTo(RelationshipTypes.CONTAINS);
        assertEquals(store, resourceType);
    }
    
    @Test
    public void testGetResourceTypeToNone() {
        assertNull(store.getResourceTypeTo(RelationshipTypes.CONTAINS));
    }
    
    @Test(expected=NotUniqueException.class)
    public void testGetResourceTypeToMultiple() {
        ResourceType region = new ResourceType("Region");
        resourceTypeDao.persist(region);
        region.relateTo(produceDept,RelationshipTypes.CONTAINS);
        produceDept.getResourceTypeTo(RelationshipTypes.CONTAINS);
    }
    
    @Test
    public void testHasResourcesNone() {
        assertFalse(store.hasResources());
    }
    
    @Test
    public void testHasResources() {
        Resource safeway = new Resource("Safeway",store);
        resourceDao.persist(safeway);
        assertTrue(store.hasResources());
    }
    
    @Test
    public void testIsRelatedTo() {
        assertTrue(store.isRelatedTo(produceDept,RelationshipTypes.CONTAINS));
    }
    
    @Test
    public void testIsRelatedToIncoming() {
        assertFalse(produceDept.isRelatedTo(store,RelationshipTypes.CONTAINS));
    }
    
    @Test
    public void testRemove() {
        Resource safeway = new Resource("Safeway",store);
        resourceDao.persist(safeway);
        store.remove();
        assertNull(resourceTypeDao.findById(store.getId()));
        assertNull(resourceDao.findById(safeway.getId()));
        assertTrue(store.getRelationships().isEmpty());
    }
    
    @Test
    public void testRemoveRelationshipsEntityName() {
        store.removeRelationships(produceDept,RelationshipTypes.CONTAINS);
        assertTrue(store.getRelationships().isEmpty());
    }
    
    @Test
    public void testRemoveRelationships() {
        store.removeRelationships();
        assertTrue(store.getRelationships().isEmpty()); 
    }
    
    @Test
    public void testRemoveRelationshipsEntityNameDir() {
        store.removeRelationships(produceDept,RelationshipTypes.CONTAINS,Direction.INCOMING);
        assertEquals(1,store.getRelationships().size());  
        store.removeRelationships(produceDept,RelationshipTypes.CONTAINS,Direction.OUTGOING);
        assertTrue(store.getRelationships().isEmpty()); 
    }
    
    @Test
    public void testRemoveRelationshipName() {
        store.removeRelationships(RelationshipTypes.CONTAINS);
        assertTrue(store.getRelationships().isEmpty()); 
    }

}
