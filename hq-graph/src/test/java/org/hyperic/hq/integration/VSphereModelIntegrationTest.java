package org.hyperic.hq.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hyperic.hq.inventory.InvalidRelationshipException;
import org.hyperic.hq.inventory.domain.Group;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.plugin.domain.ResourceType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/applicationContext.xml")
public class VSphereModelIntegrationTest {

    private ResourceType vCenterType;
    private ResourceType dataCenterType;
    private ResourceType hostType;
    private ResourceType clusterType;
    private ResourceType vAppType;
    private ResourceType vmType;
    private ResourceType resourcePoolType;
    private ResourceType dataStoreType;
    private ResourceType nodeType;
    private ResourceType vmTemplateType;
    
    private Resource vm;
    private Resource dataStore;

    // TODO this is used for parent/child and group membership. Probably OK
    private static final String CONTAINS = "CONTAINS";

    // TODO is the best way to represent hierarchy at the resource level?
    private static final String IS_A = "IS_A";

    // TODO is dependency hierarchical?
    private static final String USES = "USES";

    // Less common relationships. Would they need to extend something for
    // regular representation in our UI?
    private static final String MANAGES = "MANAGES";
    private static final String HOSTS = "HOSTS";
    private static final String RUNS_IN = "RUNS_IN";
    private static final String PROVIDES_RESOURCES = "PROVIDES_RESOURCES";
    private static final String CREATED_FROM = "CREATED_FROM";

    // TODO is relationship cardinality important in ResourceType model?
    // TODO important to enforce required relationships on model creation? (i.e.
    // vm must have a datastore)

    @Before
    public void setUp() {
        // TODO resource types unique per plugin or across the entire model
        // (need to prefix with VMware?)?
        vCenterType = new ResourceType();
        vCenterType.setName("vCenter Server");

        dataCenterType = new ResourceType();
        dataCenterType.setName("Datacenter");

        // TODO any reason to have GroupType vs ResourceType or not have Group
        // extend Resource?
        clusterType = new ResourceType();
        clusterType.setName("Cluster");

        hostType = new ResourceType();
        hostType.setName("Host");

        vAppType = new ResourceType();
        vAppType.setName("vApp");

        vmType = new ResourceType();
        vmType.setName("Virtual Machine");

        resourcePoolType = new ResourceType();
        resourcePoolType.setName("Resource Pool");

        dataStoreType = new ResourceType();
        dataStoreType.setName("Datastore");

        nodeType = new ResourceType();
        nodeType.setName("Node");

        vmTemplateType = new ResourceType();
        vmTemplateType.setName("Template");

        // A vCenterServer manages Datacenters
        vCenterType.relateTo(dataCenterType, MANAGES);

        // A dataCenter contains hosts and clusters (TODO folders? Doesn't seem
        // relevant right now)
        dataCenterType.relateTo(clusterType, CONTAINS);
        dataCenterType.relateTo(hostType, CONTAINS);

        // A cluster is a group of hosts
        // TODO What types of resources can be in the group represented as
        // another ResourceTypeRelation for now. Any reason to change?
        clusterType.relateTo(hostType, CONTAINS);

        // Hosts host VMs (required for VM to have a host)
        hostType.relateTo(vmType, HOSTS);

        // VMs run in Resource Pools (required)
        vmType.relateTo(resourcePoolType, RUNS_IN);

        // TODO vApps and Clusters can be
        // resource pools. If you add a VM to a vApp, you can't pick a diff
        // pool, but in a cluster, you can pick the cluster itself or a vApp or
        // resourcePool. Some way to enforce this in model?)
        clusterType.relateTo(resourcePoolType, IS_A);
        vAppType.relateTo(resourcePoolType, IS_A);

        // ResourcePools can have child ResourcePools
        // TODO Neo4J doesn't allow startNode to equal endNode
        // resourcePoolType.relatedTo(resourcePoolType, CONTAINS);

        // ResourcePools divide resources of clusters or hosts
        clusterType.relateTo(resourcePoolType, PROVIDES_RESOURCES);
        hostType.relateTo(resourcePoolType, PROVIDES_RESOURCES);

        // Datastores store VM files (required)
        vmType.relateTo(dataStoreType, USES);

        // VMs host Nodes
        vmType.relateTo(nodeType, HOSTS);

        // A vApp is a group of VMs
        vAppType.relateTo(vmType, CONTAINS);
        // Traverse VMs to Cluster to get cluster containing vApp. TODO model
        // directly?

        vmType.relateTo(vmTemplateType, CREATED_FROM);
    }

    @Test
    public void testModel() {
        createResources();
        assertTrue(vm.isRelatedTo(dataStore, USES));
    }

    @Test(expected = InvalidRelationshipException.class)
    public void testInvalidRelationship() {
        Resource vm = new Resource();
        vm.setName("2k328VCclone9-24");
        vm.setType(vmType);

        Resource dataStore = new Resource();
        dataStore.setName("SON-L40");
        dataStore.setType(dataStoreType);

        vm.relateTo(dataStore, "DoesSomethingFake");
    }

    @Test
    public void testNotRelatedTo() {
        Resource vm = new Resource();
        vm.setName("2k328VCclone9-24");
        vm.setType(vmType);

        Resource dataStore = new Resource();
        dataStore.setName("SON-L40");
        dataStore.setType(dataStoreType);

        assertFalse(vm.isRelatedTo(dataStore, USES));
    }

    @Test
    public void testNotRelatedToByRelationship() {
        Resource vm = new Resource();
        vm.setName("2k328VCclone9-24");
        vm.setType(vmType);

        Resource dataStore = new Resource();
        dataStore.setName("SON-L40");
        dataStore.setType(dataStoreType);

        vm.relateTo(dataStore, USES);
        assertFalse(vm.isRelatedTo(dataStore, CONTAINS));
    }

    @Test
    public void testNotRelatedOppDirection() {
        Resource vm = new Resource();
        vm.setName("2k328VCclone9-24");
        vm.setType(vmType);

        Resource dataStore = new Resource();
        dataStore.setName("SON-L40");
        dataStore.setType(dataStoreType);

        vm.relateTo(dataStore, USES);
        assertFalse(dataStore.isRelatedTo(vm, USES));
    }

    @Test
    public void testGetTree() {
        // TODO how to represent hierarchy in return value while abstracting
        // Neo4J

    }

    private void createResources() {
        // For rendering of hierarchy, we'd need to have a root node always
        // present in the system...
        Resource rootNode = new Resource();
        ResourceType rootType = new ResourceType();
        rootType.setName("System");
        rootNode.setName("Root");
        rootNode.setType(rootType);

        Resource vCenterServer = new Resource();
        vCenterServer.setName("VMC-SSRC-2K328");
        vCenterServer.setType(vCenterType);

        Resource dataCenter = new Resource();
        dataCenter.setName("Camb-HQ");
        dataCenter.setType(dataCenterType);
        vCenterServer.relateTo(dataCenter, MANAGES);
        // dataCenters are containers, let's make them top level elements
        rootNode.relateTo(dataCenter, CONTAINS);

        Group cluster = new Group();
        cluster.setType(clusterType);
        cluster.setName("Sonoma");

        dataCenter.relateTo(cluster, CONTAINS);

        Resource host = new Resource();
        host.setName("vmc-ssrc-c7k2-esx-10.eng.vmare.com");
        host.setType(hostType);
        cluster.addMember(host);

        Group vApp = new Group();
        vApp.setType(vAppType);
        vApp.setName("Sonoma Dev vApp");

        vm = new Resource();
        vm.setName("2k328VCclone9-24");
        vm.setType(vmType);
        host.relateTo(vm, "HOSTS");

        Resource resourcePool = new Resource();
        resourcePool.setName("Test Resource Pool");
        resourcePool.setType(resourcePoolType);
        vm.relateTo(resourcePool, RUNS_IN);

        dataStore = new Resource();
        dataStore.setName("SON-L40");
        dataStore.setType(dataStoreType);
        // TODO virtual disk size and provisioning policy are properties of this
        // relationship. Need relationship props
        vm.relateTo(dataStore, USES);

        // A Node is an OS Instance
        Resource guestOs = new Resource();
        guestOs.setName("Microsoft Windows Server 2003");
        // TODO How to diff OS types like Windows, Linux, etc (are these just
        // properties of the node? probably need them at type level for metric
        // collection)
        guestOs.setType(nodeType);
        vm.relateTo(guestOs, HOSTS);
    }

}
