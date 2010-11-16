package org.hyperic.hq.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hyperic.hq.inventory.InvalidRelationshipException;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.plugin.domain.ResourceType;
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

    private Resource vm;
    private Resource dataStore;

    @Before
    public void setUp() {
        modelPopulator.populateResourceModel();
    }

    @Test
    public void testModel() {
        createResources();
        assertTrue(vm.isRelatedTo(dataStore, VSphereResourceModelPopulator.USES));
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
        assertFalse(vm.isRelatedTo(dataStore, VSphereResourceModelPopulator.CONTAINS));
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

    private void createResources() {
        Resource rootNode = Resource.findResourceByName(VSphereResourceModelPopulator.ROOT_NODE_NAME);
        Resource vCenterServer = new Resource();
        vCenterServer.setName("VMC-SSRC-2K328");
        vCenterServer.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.VCENTER_SERVER_TYPE));

        Resource dataCenter = new Resource();
        dataCenter.setName("Camb-HQ");
        dataCenter.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.DATACENTER_TYPE));
        vCenterServer.relateTo(dataCenter, VSphereResourceModelPopulator.MANAGES);
        // dataCenters are containers, let's make them top level elements
        rootNode.relateTo(dataCenter, VSphereResourceModelPopulator.CONTAINS);

        ResourceGroup cluster = new ResourceGroup();
        cluster.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.CLUSTER_TYPE));
        cluster.setName("Sonoma");

        dataCenter.relateTo(cluster, VSphereResourceModelPopulator.CONTAINS);

        Resource host = new Resource();
        host.setName("vmc-ssrc-c7k2-esx-10.eng.vmare.com");
        host.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.HOST_TYPE));
        cluster.addMember(host);

        ResourceGroup vApp = new ResourceGroup();
        vApp.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.VAPP_TYPE));
        vApp.setName("Sonoma Dev vApp");

        vm = new Resource();
        vm.setName("2k328VCclone9-24");
        vm.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.VIRTUAL_MACHINE_TYPE));
        host.relateTo(vm, VSphereResourceModelPopulator.HOSTS);

        Resource resourcePool = new Resource();
        resourcePool.setName("Test Resource Pool");
        resourcePool.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.RESOURCE_POOL_TYPE));
        vm.relateTo(resourcePool, VSphereResourceModelPopulator.RUNS_IN);

        dataStore = new Resource();
        dataStore.setName("SON-L40");
        dataStore.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.DATASTORE_TYPE));
        // TODO virtual disk size and provisioning policy are properties of this
        // relationship. Need relationship props
        vm.relateTo(dataStore, VSphereResourceModelPopulator.USES);

        // A Node is an OS Instance
        Resource guestOs = new Resource();
        guestOs.setName("Microsoft Windows Server 2003");
        // TODO How to diff OS types like Windows, Linux, etc (are these just
        // properties of the node? probably need them at type level for metric
        // collection)
        guestOs.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.NODE_TYPE));
        vm.relateTo(guestOs, VSphereResourceModelPopulator.HOSTS);
    }

}
