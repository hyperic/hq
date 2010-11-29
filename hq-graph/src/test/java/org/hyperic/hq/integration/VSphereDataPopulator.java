package org.hyperic.hq.integration;

import org.hyperic.hq.alert.domain.Alert;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceRelation;
import org.hyperic.hq.plugin.domain.ResourceType;
import org.hyperic.hq.reference.RelationshipTypes;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates some VSphere resources. VSphereResourceModelPopulator must be run as
 * a prerequisite to set up the resource model (as would be done on plugin
 * installation)
 * @author jhickey
 * 
 */
@Component
@Transactional
public class VSphereDataPopulator {

    static final String DATA_CENTER_NAME = "Camb-HQ";
    static final String GUEST_OS_NAME = "Microsoft Windows Server 2003";
    static final String RESOURCE_POOL_NAME = "Test Resource Pool";
    static final String CLUSTER_NAME = "Sonoma";
    static final String HOST_NAME = "vmc-ssrc-c7k2-esx-10.eng.vmare.com";
    static final String DATASTORE_NAME = "SON-L40";
    static final String VM_NAME = "2k328VCclone9-24";

    public void populateData() {
        Resource rootNode = Resource
            .findResourceByName(VSphereResourceModelPopulator.ROOT_NODE_NAME);
        Resource vCenterServer = new Resource();
        vCenterServer.setName("VMC-SSRC-2K328");
        vCenterServer.setType(ResourceType
            .findResourceTypeByName(VSphereResourceModelPopulator.VCENTER_SERVER_TYPE));
        vCenterServer.persist();

        rootNode.relateTo(vCenterServer, RelationshipTypes.CONTAINS);
        
        Resource dataCenter = new Resource();
        dataCenter.setName(DATA_CENTER_NAME);
        dataCenter.setType(ResourceType
            .findResourceTypeByName(VSphereResourceModelPopulator.DATACENTER_TYPE));
        dataCenter.persist();
        vCenterServer.relateTo(dataCenter, VSphereResourceModelPopulator.MANAGES);
        // dataCenters are containers, let's make them top level elements
       

        ResourceGroup cluster = new ResourceGroup();
        cluster.setType(ResourceType
            .findResourceTypeByName(VSphereResourceModelPopulator.CLUSTER_TYPE));
        cluster.setName(CLUSTER_NAME);
        cluster.persist();
        dataCenter.relateTo(cluster, RelationshipTypes.CONTAINS);

        Resource host = new Resource();
        host.setName(HOST_NAME);
        host.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.HOST_TYPE));
        host.persist();
        host.setProperty("version", "4.1");
        host.merge();
        cluster.addMember(host);

        ResourceGroup vApp = new ResourceGroup();
        vApp.setType(ResourceType.findResourceTypeByName(VSphereResourceModelPopulator.VAPP_TYPE));
        vApp.setName("Sonoma Dev vApp");
        vApp.persist();
        
        Resource vm = new Resource();
        vm.setName(VM_NAME);
        vm.setType(ResourceType
            .findResourceTypeByName(VSphereResourceModelPopulator.VIRTUAL_MACHINE_TYPE));
        vm.persist();
        host.relateTo(vm, VSphereResourceModelPopulator.HOSTS);

        Resource resourcePool = new Resource();
        resourcePool.setName(RESOURCE_POOL_NAME);
        resourcePool.setType(ResourceType
            .findResourceTypeByName(VSphereResourceModelPopulator.RESOURCE_POOL_TYPE));
        resourcePool.persist();
        vm.relateTo(resourcePool, VSphereResourceModelPopulator.RUNS_IN);

        Resource dataStore = new Resource();
        dataStore.setName(DATASTORE_NAME);
        dataStore.setType(ResourceType
            .findResourceTypeByName(VSphereResourceModelPopulator.DATASTORE_TYPE));
        dataStore.persist();
        // TODO virtual disk size and provisioning policy are properties of this
        // relationship. Need relationship props
        ResourceRelation vmToDataStore = vm.relateTo(dataStore, VSphereResourceModelPopulator.USES);
        vmToDataStore.setProperty("Disk Size",1234);

        // A Node is an OS Instance
        Resource guestOs = new Resource();
        guestOs.setName(GUEST_OS_NAME);
        // TODO How to diff OS types like Windows, Linux, etc (are these just
        // properties of the node? probably need them at type level for metric
        // collection)
        guestOs.setType(ResourceType
            .findResourceTypeByName(VSphereResourceModelPopulator.NODE_TYPE));
        guestOs.persist();
        vm.relateTo(guestOs, VSphereResourceModelPopulator.HOSTS);
        
        Alert alert =  new Alert();
        alert.setComment("Saw this");
        alert.persist();
       
       
        alert.setResource(vm);
    }
  
    public static void main(String[] args) {
        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext(
            "classpath:/META-INF/spring/applicationContext.xml");
        VSphereDataPopulator dataPopulator = appContext.getBean(VSphereDataPopulator.class);
        dataPopulator.populateData();
        System.exit(0);
    }
}
