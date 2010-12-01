package org.hyperic.hq.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.alert.domain.Alert;
import org.hyperic.hq.inventory.InvalidRelationshipException;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceRelation;
import org.hyperic.hq.plugin.domain.ResourceType;
import org.hyperic.hq.reference.RelationshipTypes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/applicationContext.xml")
public class VSphereModelIntegrationTest {

    @Autowired
    private VSphereResourceModelPopulator modelPopulator;

   @Autowired
   private VSphereDataPopulator dataPopulator;

    @Before
    public void setUp() {
        modelPopulator.populateResourceModel();
    }

    @Test
    public void testModel() {
       dataPopulator.populateData();
       Resource vm = Resource.findResourceByName(VSphereDataPopulator.VM_NAME);
       assertTrue(vm.isRelatedTo(Resource.findResourceByName(VSphereDataPopulator.DATASTORE_NAME), VSphereResourceModelPopulator.USES));
       Set<Resource> groupMembers = ResourceGroup.findResourceGroupByName(VSphereDataPopulator.CLUSTER_NAME).getMembers();
       assertEquals(1,groupMembers.size());
       //equals ends up comparing IDs of underlying Nodes for equality (see Neo4jNodeBacking)
       Resource host = Resource.findResourceByName(VSphereDataPopulator.HOST_NAME);
       assertEquals(host,groupMembers.iterator().next());
       //TODO Make sure a ResourceGroup comes back as a Resource
       //Currently any variation of findAll goes into infinite recursion in strange circumstances, such as 2 instances of an extending
       //class existing.  To make this work, have to comment out above call to findResourceGroupByName and creation of the vApp instance
       
       //Iterable<Resource> resources = Resource.findAllResources();
       //assertTrue(resources.contains(ResourceGroup.findResourceGroupByName(VSphereDataPopulator.CLUSTER_NAME)));
       
       //TODO findAllResourceGroups and variations has same problem as above
       //List<ResourceGroup> groups = ResourceGroup.findAllResourceGroups();
       //assertTrue(groups.contains(ResourceGroup.findResourceGroupByName(VSphereDataPopulator.CLUSTER_NAME)));
       Resource datastore = Resource.findResourceByName(VSphereDataPopulator.DATASTORE_NAME);
       assertEquals("4.1",host.getProperties().get("version"));
       assertEquals(1234,vm.getRelationshipTo(datastore,VSphereResourceModelPopulator.USES).getProperty("Disk Size"));
          
       Set<ExpectedRelation> expectedRelations = new HashSet<ExpectedRelation>();
       expectedRelations.add(new ExpectedRelation(vm,Resource.findResourceByName(VSphereDataPopulator.RESOURCE_POOL_NAME),VSphereResourceModelPopulator.RUNS_IN));
       ExpectedRelation vmToDatastore = new ExpectedRelation(vm, datastore, VSphereResourceModelPopulator.USES);
       vmToDatastore.addProperty("Disk Size",1234);
       expectedRelations.add(vmToDatastore);
       expectedRelations.add(new ExpectedRelation(vm,Resource.findResourceByName(VSphereDataPopulator.GUEST_OS_NAME), VSphereResourceModelPopulator.HOSTS));
       
       Set<ResourceRelation> relations = vm.getRelationships();
       assertEquals(expectedRelations.size(),relations.size());
       
       for(ResourceRelation relation: relations) {
          boolean foundExpected = false;
          for(ExpectedRelation expectedRelation: expectedRelations) {
              if(testRelationsEqual(relation, expectedRelation)) {
                  foundExpected=true;
                  break;
              }
          }
          if(! foundExpected) {
              fail("Relation " + relation.getName() + " from[" + relation.getFrom() + 
              "] to[" + relation.getTo() + "] with properties[" + relation.getProperties() + "] was not expected");
          }
       }
       
       //verify group relationships can still be obtained
       Resource dataCenter = Resource.findResourceByName(VSphereDataPopulator.DATA_CENTER_NAME);
       Set<ResourceRelation> datacenterRelations = dataCenter.getRelationships();
       ExpectedRelation datacenterToCluster = new ExpectedRelation(dataCenter,ResourceGroup.findResourceGroupByName(VSphereDataPopulator.CLUSTER_NAME),RelationshipTypes.CONTAINS);
       assertEquals(1,datacenterRelations.size());
       assertTrue(testRelationsEqual(datacenterRelations.iterator().next(),datacenterToCluster));
      
    }
    
    private boolean testRelationsEqual(ResourceRelation relation1, ExpectedRelation relation2) {
        return (relation1.getFrom().equals(relation2.getFrom())) &&
               (relation1.getTo().equals(relation2.getTo())) &&
               (relation1.getName().equals(relation2.getName())) &&
               (relation1.getProperties().equals(relation2.getProperties()));
    }
    
    @Test
    public void testCrossStorePersistence() {
    	dataPopulator.populateData();
    	Resource vm = Resource.findResourceByName(VSphereDataPopulator.VM_NAME);
    	List<Alert> alerts = Alert.findAllAlerts();
        assertEquals(vm.getName(),alerts.get(0).getResource().getName());
        Set<Alert> resourceAlerts = vm.getAlerts();
        assertEquals(1,resourceAlerts.size());
        Alert actual = alerts.get(0);
        Alert expected = resourceAlerts.iterator().next();
        assertEquals(expected,actual);
        assertEquals("Saw this",actual.getComment());
    }

    @Test(expected = InvalidRelationshipException.class)
    public void testInvalidRelationship() {
        Resource vm = new Resource();
        vm.setName("2k328VCclone9-24");
        vm.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.VIRTUAL_MACHINE_TYPE));

        Resource dataStore = new Resource();
        dataStore.setName("SON-L40");
        dataStore.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.DATASTORE_TYPE));

        vm.relateTo(dataStore, "DoesSomethingFake");
    }

    @Test
    public void testNotRelatedTo() {
        Resource vm = new Resource();
        vm.setName("2k328VCclone9-24");
        vm.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.VIRTUAL_MACHINE_TYPE));
        vm.persist();

        Resource dataStore = new Resource();
        dataStore.setName("SON-L40");
        dataStore.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.DATASTORE_TYPE));
        dataStore.persist();

        assertFalse(vm.isRelatedTo(dataStore, VSphereResourceModelPopulator.USES));
    }

    @Test
    public void testNotRelatedToByRelationship() {
        Resource vm = new Resource();
        vm.setName("2k328VCclone9-24");
        vm.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.VIRTUAL_MACHINE_TYPE));
        vm.persist();

        Resource dataStore = new Resource();
        dataStore.setName("SON-L40");
        dataStore.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.DATASTORE_TYPE));
        dataStore.persist();

        vm.relateTo(dataStore, VSphereResourceModelPopulator.USES);
        assertFalse(vm.isRelatedTo(dataStore, RelationshipTypes.CONTAINS));
    }

    @Test
    public void testNotRelatedOppDirection() {
        Resource vm = new Resource();
        vm.setName("2k328VCclone9-24");
        vm.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.VIRTUAL_MACHINE_TYPE));
        vm.persist();

        Resource dataStore = new Resource();
        dataStore.setName("SON-L40");
        dataStore.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.DATASTORE_TYPE));
        dataStore.persist();

        vm.relateTo(dataStore, VSphereResourceModelPopulator.USES);
        assertFalse(dataStore.isRelatedTo(vm, VSphereResourceModelPopulator.USES));
    }

    @Test
    public void testGetTree() {
        // TODO how to represent hierarchy in return value while abstracting
        // Neo4J

    }
    
    private class ExpectedRelation {
        private Resource from;
        private Resource to;
        private String name;
        private Map<String,Object> properties = new HashMap<String,Object>();
        
        public ExpectedRelation(Resource from, Resource to, String name) {
            this.from = from;
            this.to = to;
            this.name = name;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void addProperty(String key, Object value) {
            properties.put(key, value);
        }

        public Resource getFrom() {
            return from;
        }

        public Resource getTo() {
            return to;
        }

        public String getName() {
            return name;
        }
    }

}
