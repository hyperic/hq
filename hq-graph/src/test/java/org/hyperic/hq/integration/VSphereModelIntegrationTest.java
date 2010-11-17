package org.hyperic.hq.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.hyperic.hq.inventory.InvalidRelationshipException;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
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
       
       assertEquals("4.1",host.getProperties().get("version"));
       assertEquals(1234,vm.getRelationshipTo(Resource.findResourceByName(VSphereDataPopulator.DATASTORE_NAME), 
           VSphereResourceModelPopulator.USES).getProperty("Disk Size"));
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

        Resource dataStore = new Resource();
        dataStore.setName("SON-L40");
        dataStore.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.DATASTORE_TYPE));

        assertFalse(vm.isRelatedTo(dataStore, VSphereResourceModelPopulator.USES));
    }

    @Test
    public void testNotRelatedToByRelationship() {
        Resource vm = new Resource();
        vm.setName("2k328VCclone9-24");
        vm.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.VIRTUAL_MACHINE_TYPE));

        Resource dataStore = new Resource();
        dataStore.setName("SON-L40");
        dataStore.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.DATASTORE_TYPE));

        vm.relateTo(dataStore, VSphereResourceModelPopulator.USES);
        assertFalse(vm.isRelatedTo(dataStore, RelationshipTypes.CONTAINS));
    }

    @Test
    public void testNotRelatedOppDirection() {
        Resource vm = new Resource();
        vm.setName("2k328VCclone9-24");
        vm.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.VIRTUAL_MACHINE_TYPE));

        Resource dataStore = new Resource();
        dataStore.setName("SON-L40");
        dataStore.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.DATASTORE_TYPE));

        vm.relateTo(dataStore, VSphereResourceModelPopulator.USES);
        assertFalse(dataStore.isRelatedTo(vm, VSphereResourceModelPopulator.USES));
    }

    @Test
    public void testGetTree() {
        // TODO how to represent hierarchy in return value while abstracting
        // Neo4J

    }

}
