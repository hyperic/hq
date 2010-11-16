package org.hyperic.hq.integration;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.plugin.domain.ResourceType;
import org.hyperic.hq.reference.RelationshipTypes;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class VSphereDataPopulator {

    static final String CLUSTER_NAME = "Sonoma";
    static final String HOST_NAME = "vmc-ssrc-c7k2-esx-10.eng.vmare.com";
    static final String DATASTORE_NAME = "SON-L40";
    static final String VM_NAME = "2k328VCclone9-24";

    public void populateData() {
        Resource rootNode = Resource.findResourceByName(VSphereResourceModelPopulator.ROOT_NODE_NAME);
        Resource vCenterServer = new Resource();
        vCenterServer.setName("VMC-SSRC-2K328");
        vCenterServer.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.VCENTER_SERVER_TYPE));

        Resource dataCenter = new Resource();
        dataCenter.setName("Camb-HQ");
        dataCenter.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.DATACENTER_TYPE));
        vCenterServer.relateTo(dataCenter, VSphereResourceModelPopulator.MANAGES);
        // dataCenters are containers, let's make them top level elements
        rootNode.relateTo(dataCenter, RelationshipTypes.CONTAINS);

        ResourceGroup cluster = new ResourceGroup();
        cluster.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.CLUSTER_TYPE));
        cluster.setName(CLUSTER_NAME);

        dataCenter.relateTo(cluster, RelationshipTypes.CONTAINS);

        Resource host = new Resource();
        host.setName(HOST_NAME);
        host.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.HOST_TYPE));
        cluster.addMember(host);

        ResourceGroup vApp = new ResourceGroup();
        vApp.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.VAPP_TYPE));
        vApp.setName("Sonoma Dev vApp");

        Resource vm = new Resource();
        vm.setName(VM_NAME);
        vm.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.VIRTUAL_MACHINE_TYPE));
        host.relateTo(vm, VSphereResourceModelPopulator.HOSTS);

        Resource resourcePool = new Resource();
        resourcePool.setName("Test Resource Pool");
        resourcePool.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.RESOURCE_POOL_TYPE));
        vm.relateTo(resourcePool, VSphereResourceModelPopulator.RUNS_IN);

        Resource dataStore = new Resource();
        dataStore.setName(DATASTORE_NAME);
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
    
    public static void main(String[] args) {
        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext(
            "classpath:/META-INF/spring/applicationContext.xml");
        VSphereDataPopulator dataPopulator = appContext
            .getBean(VSphereDataPopulator.class);
        dataPopulator.populateData();
        System.exit(0);
    }
}
