package org.hyperic.hq.integration;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.plugin.domain.PropertyType;
import org.hyperic.hq.plugin.domain.ResourceType;
import org.hyperic.hq.reference.RelationshipTypes;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Transactional
@Component
public class VSphereResourceModelPopulator {

    public static final String SYSTEM_TYPE = "System";

    public static final String ROOT_NODE_NAME = "Root";

    public static final String TEMPLATE_TYPE = "Template";

    public static final String NODE_TYPE = "Node";

    public static final String DATASTORE_TYPE = "Datastore";

    public static final String RESOURCE_POOL_TYPE = "Resource Pool";

    public static final String VIRTUAL_MACHINE_TYPE = "Virtual Machine";

    public static final String VAPP_TYPE = "vApp";

    public static final String HOST_TYPE = "Host";

    public static final String CLUSTER_TYPE = "Cluster";

    public static final String DATACENTER_TYPE = "Datacenter";

    public static final String VCENTER_SERVER_TYPE = "vCenter Server";

    // TODO is dependency hierarchical?
    public static final String USES = "USES";

    // Less common relationships. Would they need to extend something for
    // regular representation in our UI?
    public static final String MANAGES = "MANAGES";
    public static final String HOSTS = "HOSTS";
    public static final String RUNS_IN = "RUNS_IN";
    public static final String PROVIDES_RESOURCES = "PROVIDES_RESOURCES";
    public static final String CREATED_FROM = "CREATED_FROM";

    // TODO is relationship cardinality important in ResourceType model?
    // TODO important to enforce required relationships on model creation? (i.e.
    // vm must have a datastore)

    public void populateResourceModel() {
        addRootNode();

        // TODO resource types unique per plugin or across the entire model
        // (need to prefix with VMware?)?
        ResourceType vCenterType = new ResourceType();
        vCenterType.setName(VCENTER_SERVER_TYPE);
        vCenterType.persist();

        ResourceType dataCenterType = new ResourceType();
        dataCenterType.setName(DATACENTER_TYPE);
        dataCenterType.persist();

        // TODO any reason to have ResourceGroupType vs ResourceType or not have
        // ResourceGroup
        // extend Resource?
        ResourceType clusterType = new ResourceType();
        clusterType.setName(CLUSTER_TYPE);
        clusterType.persist();

        ResourceType hostType = new ResourceType();
        hostType.setName(HOST_TYPE);
        hostType.persist();
        
        PropertyType versionProp = new PropertyType();
        versionProp.setName("version");
        versionProp.setDescription("VMware Version");
        versionProp.setResourceType(hostType);
        versionProp.persist();

        ResourceType vAppType = new ResourceType();
        vAppType.setName(VAPP_TYPE);
        vAppType.persist();

        ResourceType vmType = new ResourceType();
        vmType.setName(VIRTUAL_MACHINE_TYPE);
        vmType.persist();

        ResourceType resourcePoolType = new ResourceType();
        resourcePoolType.setName(RESOURCE_POOL_TYPE);
        resourcePoolType.persist();

        ResourceType dataStoreType = new ResourceType();
        dataStoreType.setName(DATASTORE_TYPE);
        dataStoreType.persist();

        ResourceType nodeType = new ResourceType();
        nodeType.setName(NODE_TYPE);
        nodeType.persist();

        ResourceType vmTemplateType = new ResourceType();
        vmTemplateType.setName(TEMPLATE_TYPE);
        vmTemplateType.persist();

        // A vCenterServer manages Datacenters
        vCenterType.relateTo(dataCenterType, MANAGES);

        // A dataCenter Relationship.CONTAINS hosts and clusters (TODO folders? Doesn't seem
        // relevant right now)
        dataCenterType.relateTo(clusterType, RelationshipTypes.CONTAINS);
        dataCenterType.relateTo(hostType, RelationshipTypes.CONTAINS);

        // A cluster is a ResourceGroup of hosts
        // TODO What types of resources can be in the ResourceGroup represented
        // as
        // another ResourceTypeRelation for now. Any reason to change?
        clusterType.relateTo(hostType, RelationshipTypes.CONTAINS);

        // Hosts host VMs (required for VM to have a host)
        hostType.relateTo(vmType, HOSTS);

        // VMs run in Resource Pools (required)
        vmType.relateTo(resourcePoolType, RUNS_IN);

        // TODO vApps and Clusters can be
        // resource pools. If you add a VM to a vApp, you can't pick a diff
        // pool, but in a cluster, you can pick the cluster itself or a vApp or
        // resourcePool. Some way to enforce this in model?)
        clusterType.relateTo(resourcePoolType, RelationshipTypes.EXTENDS);
        vAppType.relateTo(resourcePoolType, RelationshipTypes.EXTENDS);

        // ResourcePools can have child ResourcePools
        // TODO Neo4J doesn't allow startNode to equal endNode
        // resourcePoolType.relatedTo(resourcePoolType, Relationship.CONTAINS);

        // ResourcePools divide resources of clusters or hosts
        clusterType.relateTo(resourcePoolType, PROVIDES_RESOURCES);
        hostType.relateTo(resourcePoolType, PROVIDES_RESOURCES);

        // Datastores store VM files (required)
        vmType.relateTo(dataStoreType, USES);

        // VMs host Nodes
        vmType.relateTo(nodeType, HOSTS);

        // A vApp is a ResourceGroup of VMs
        vAppType.relateTo(vmType, RelationshipTypes.CONTAINS);
        // Traverse VMs to Cluster to get cluster containing vApp. TODO model
        // directly?

        vmType.relateTo(vmTemplateType, CREATED_FROM);
    }

    public void populateResourceModelByApi() {
    	RestTemplate api = new RestTemplate();
    	List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
    	String uri = "http://localhost:8080/hq-graph/resourcetypes";
    	HttpHeaders headers = new HttpHeaders();
    	
    	headers.setContentType(MediaType.APPLICATION_JSON);    	
    	converters.add(new MappingJacksonHttpMessageConverter());
    	api.setMessageConverters(converters);
    	
    	String[] names = new String[] {
       			"__" + VCENTER_SERVER_TYPE,
       			"__" + DATACENTER_TYPE,
       			"__" + CLUSTER_TYPE,
       			"__" + HOST_TYPE,
       			"__" + VAPP_TYPE,
       			"__" + VIRTUAL_MACHINE_TYPE,
       			"__" + RESOURCE_POOL_TYPE,
       			"__" + DATASTORE_TYPE,
       			"__" + NODE_TYPE,
       			"__" + TEMPLATE_TYPE   			
    	};
    	
    	for (String name : names) {
    		Map<String, String> payload = new HashMap<String, String>();
    		
    		payload.put("name", name);
    	
    		Map<String, String> result = api.postForObject(uri, new HttpEntity<Map<String, String>>(payload, headers), Map.class);
    		
    		System.out.println("Created [" + name + "] at " + result.get("uri"));
    	}
    }

    private void addRootNode() {
        // This isn't vsphere specific, but working w/idea that we'll
        // pre-install a System Node Type and a Root Node for hierarchy
        // traversal
        ResourceType rootType = new ResourceType();
        rootType.setName(SYSTEM_TYPE);
        rootType.persist();
        Resource rootNode = new Resource();
        rootNode.setName(ROOT_NODE_NAME);
        rootNode.setType(rootType);
        rootNode.persist();
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext(
            "classpath:/META-INF/spring/applicationContext.xml");
        VSphereResourceModelPopulator modelPopulator = appContext
            .getBean(VSphereResourceModelPopulator.class);
        modelPopulator.populateResourceModel();
        System.exit(0);
    }

}
