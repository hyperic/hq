package org.hyperic.hq.inventory.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.hyperic.hq.inventory.NotUniqueException;
import org.hyperic.hq.inventory.data.ResourceDao;
import org.hyperic.hq.inventory.data.ResourceTypeDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.core.Direction;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext
@Transactional("neoTxManager")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:META-INF/spring/neo4j-context.xml",
                                   "classpath:org/hyperic/hq/inventory/InventoryIntegrationTest-context.xml" })
public class ResourceTypeIntegrationTest {

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
        OperationArgType lettuceCount = new OperationArgType("LettuceCount", Integer.class);
        OperationArgType cucumberCount = new OperationArgType("CucumberCount", Integer.class);
        Set<OperationArgType> argTypes = new HashSet<OperationArgType>();
        argTypes.add(lettuceCount);
        argTypes.add(cucumberCount);
        inventory.addOperationArgTypes(argTypes);
        inventory.setReturnType(String.class.getName());
        Set<OperationType> expected = new HashSet<OperationType>(1);
        expected.add(inventory);
        assertEquals(expected, store.getOperationTypes());
        assertEquals(2,store.getOperationTypes().iterator().next().getOperationArgTypes().size());
        assertEquals(String.class.getName(),store.getOperationTypes().iterator().next().getReturnType());
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
    public void testAddOperationTypes() {
        OperationType inventory = new OperationType("inventory");
        OperationType takeBreak = new OperationType("takeBreak");
        Set<OperationType> actual = new HashSet<OperationType>(2);
        actual.add(inventory);
        actual.add(takeBreak);
        store.addOperationTypes(actual);
        Set<OperationType> expected = new HashSet<OperationType>(2);
        expected.add(inventory);
        expected.add(takeBreak);
        assertEquals(expected, store.getOperationTypes());
    }
    
    @Test
    public void testAddConfigType() {
        ConfigType security = new ConfigType("security");
        store.addConfigType(security);
        ConfigOptionType securityCode = new ConfigOptionType("SecurityCode", "The security code");
        security.addConfigOptionType(securityCode);
        Set<ConfigType> expected = new HashSet<ConfigType>(1);
        expected.add(security);
        assertEquals(expected,store.getConfigTypes());
        assertEquals(1,store.getConfigTypes().iterator().next().getConfigOptionTypes().size());
        assertEquals(securityCode,store.getConfigTypes().iterator().next().getConfigOptionTypes().iterator().next());
    }
    
    @Test
    public void testAddConfigTypeMultipleConfigOpts() {
        ConfigType security = new ConfigType("security");
        store.addConfigType(security);
        ConfigOptionType securityCode = new ConfigOptionType("SecurityCode", "The security code");
        ConfigOptionType registerPw = new ConfigOptionType("RegisterPw", "The register PW");
        Set<ConfigOptionType> optTypes = new HashSet<ConfigOptionType>();
        optTypes.add(securityCode);
        optTypes.add(registerPw);
        security.addConfigOptionTypes(optTypes);
        Set<ConfigType> expected = new HashSet<ConfigType>(1);
        expected.add(security);
        assertEquals(expected,store.getConfigTypes());
        assertEquals(2,store.getConfigTypes().iterator().next().getConfigOptionTypes().size());
    }
    
    @Test
    public void testAddConfigTypeTwice() {
        ConfigType security = new ConfigType("security");
        store.addConfigType(security);
        store.addConfigType(security);
        Set<ConfigType> expected = new HashSet<ConfigType>(1);
        expected.add(security);
        assertEquals(expected,store.getConfigTypes());
    }

    @Test
    public void testAddPropertyType() {
        PropertyType address = new PropertyType("Address", "The store location");
        store.addPropertyType(address);
        Set<PropertyType> expected = new HashSet<PropertyType>(1);
        expected.add(address);
        assertEquals(expected, store.getPropertyTypes());
    }

    @Test
    public void testAddPropertyTypeTwice() {
        PropertyType address = new PropertyType("Address", "The store location");
        store.addPropertyType(address);
        store.addPropertyType(address);
        Set<PropertyType> expected = new HashSet<PropertyType>(1);
        expected.add(address);
        assertEquals(expected, store.getPropertyTypes());
    }
    
    @Test
    public void testAddPropertyTypes() {
        PropertyType address = new PropertyType("Address", "The store location");
        PropertyType manager = new PropertyType("Manager", "The store manager");
        Set<PropertyType> actual = new HashSet<PropertyType>(1);
        actual.add(address);
        actual.add(manager);
        store.addPropertyTypes(actual);
        Set<PropertyType> expected = new HashSet<PropertyType>(1);
        expected.add(address);
        expected.add(manager);
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
    public void testGetConfigType() {
        ConfigType measurement = new ConfigType("Measurement");
        store.addConfigType(measurement);
        assertEquals(measurement, store.getConfigType("Measurement"));
    }

    @Test
    public void testGetConfigTypeNotExistent() {
        assertNull(store.getConfigType("Measurement"));
    }

    @Test
    public void testGetPropertyType() {
        PropertyType address = new PropertyType("Address", "The store location");
        store.addPropertyType(address);
        assertEquals(address, store.getPropertyType("Address"));
    }

    @Test
    public void testGetPropertyTypeNotExistent() {
        assertNull(store.getPropertyType("Address"));
    }
    
    @Test
    public void testGetPropertyTypesFilterHidden() {
        PropertyType address = new PropertyType("Address", "The store location");
        store.addPropertyType(address);
        PropertyType ssn = new PropertyType("SSN", "Social Security Num");
        ssn.setHidden(true);
        store.addPropertyType(ssn);
        Set<PropertyType> expected = new HashSet<PropertyType>();
        expected.add(address);
        assertEquals(expected,store.getPropertyTypes(false));
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
    public void testGetResourceTypesFrom() {
        Set<ResourceType> resourceTypes = store.getResourceTypesFrom(RelationshipTypes.CONTAINS);
        assertEquals(1, resourceTypes.size());
        assertEquals(produceDept, resourceTypes.iterator().next());
    }
     
    @Test
    public void testGetResourceTypesTo() {
        Set<ResourceType> resourceTypes = produceDept.getResourceTypesTo(RelationshipTypes.CONTAINS);
        assertEquals(1, resourceTypes.size());
        assertEquals(store, resourceTypes.iterator().next());
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
        store.addPropertyType(new PropertyType("address","The store location"));
        OperationType inventory = new OperationType("inventory");
        store.addOperationType(inventory);
        OperationArgType lettuceCount = new OperationArgType("LettuceCount", Integer.class);
        inventory.addOperationArgType(lettuceCount);
        ConfigType security = new ConfigType("security");
        store.addConfigType(security);
        ConfigOptionType securityCode = new ConfigOptionType("SecurityCode", "The security code");
        security.addConfigOptionType(securityCode);
        Resource safeway = new Resource("Safeway",store);
        resourceDao.persist(safeway);
        safeway.setProperty("address","123 My Street");
        Config securityConfig = new Config();
        securityConfig.setType(security);
        safeway.addConfig(securityConfig);
        store.remove();
        assertEquals(Long.valueOf(1),resourceTypeDao.count());
        assertEquals(Long.valueOf(0),resourceDao.count());
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
    
    @Test(expected=NotUniqueException.class)
    public void testPersistResourceTypeAlreadyExists() {
        resourceTypeDao.persist(new ResourceType("Grocery Store"));
    }

}
