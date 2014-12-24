package org.hyperic.plugin.vrealize.automation;

import static com.vmware.hyperic.model.relations.RelationType.PARENT;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.createLogialResource;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.executeXMLQuery;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.getFQDNFromURI;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.getFullResourceName;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.getParameterizedName;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.marshallResource;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.setModelProperty;
import static org.hyperic.plugin.vrealize.automation.VraConstants.KEY_APPLICATION_NAME;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_DEM_SERVER_GROUP;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_LOAD_BALANCER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_APPLICATION;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_MANAGER_SERVER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_MANAGER_SERVER_LOAD_BALANCER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_WEB;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_WEB_LOAD_BALANCER;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.ServerResource;

import com.vmware.hyperic.model.relations.ObjectFactory;
import com.vmware.hyperic.model.relations.RelationType;
import com.vmware.hyperic.model.relations.Resource;
import com.vmware.hyperic.model.relations.ResourceSubType;
import com.vmware.hyperic.model.relations.ResourceTier;

/**
 *
 * @author Tomer Shetah
 */
public class DiscoveryDEM extends Discovery {
    private static final Log log = LogFactory.getLog(DiscoveryDEM.class);

    @Override
    protected ServerResource newServerResource(long pid,
                                               String exe) {
        ServerResource server = super.newServerResource(pid, exe);
        log.debug("[newServerResource] pid=" + pid);

        File configFile = new File(exe + ".config");
        log.debug("[newServerResource] configFile=" + configFile);

        String vRAIaasWebLB = executeXMLQuery("//appSettings/add[@key='repositoryAddress']/@value", configFile);
        if (!StringUtils.isEmpty(vRAIaasWebLB)) {
            vRAIaasWebLB = getFQDNFromURI(vRAIaasWebLB);
        }
        log.debug("[newServerResource] vRAIaasWebLB (repositoryAddress) = '" + vRAIaasWebLB + "'");

        String managerLB = executeXMLQuery("//system.serviceModel/client/endpoint/@address", configFile);
        if (!StringUtils.isEmpty(managerLB)) {
            managerLB = getFQDNFromURI(managerLB);
        }
        log.debug("[newServerResource] l (DynamicOps_Vmps_Agent_Core_VMPSAgentService_ProxyAgentService) = '"
                    + managerLB + "'");

        Resource modelResource = getCommonModel(server, vRAIaasWebLB, managerLB);
        String modelXml = marshallResource(modelResource);
        setModelProperty(server, modelXml);

        return server;
    }

    private static Resource getCommonModel(ServerResource server,
                                           String vRAIaasWebLB,
                                           String managerLB) {
        String demServerGroupName = getParameterizedName(KEY_APPLICATION_NAME, TYPE_DEM_SERVER_GROUP);
        String applicationTagName = getParameterizedName(KEY_APPLICATION_NAME, TYPE_VRA_APPLICATION);

        ObjectFactory objectFactory = new ObjectFactory();

        Resource demServer = objectFactory.createResource(false,
                    server.getType(), server.getName(), ResourceTier.SERVER);
        Resource demGroup = objectFactory.createResource(true,
                    TYPE_DEM_SERVER_GROUP, demServerGroupName, ResourceTier.LOGICAL,
                    ResourceSubType.TAG);
        Resource application = objectFactory.createResource(true,
                    TYPE_VRA_APPLICATION, applicationTagName, ResourceTier.LOGICAL,
                    ResourceSubType.TAG);

        demServer.addRelations(objectFactory.createRelation(demGroup,
                    RelationType.PARENT));

        demGroup.addRelations(objectFactory.createRelation(application,
                    RelationType.PARENT));

        Resource loadBalancerSuperTag =
                    createLogialResource(objectFactory, TYPE_LOAD_BALANCER_TAG,
                                getParameterizedName(KEY_APPLICATION_NAME));

        if (!StringUtils.isEmpty(vRAIaasWebLB)) {
            Resource vraIaasWebLoadBalancer = objectFactory.createResource(true,
                        TYPE_VRA_IAAS_WEB_LOAD_BALANCER,
                        getFullResourceName(vRAIaasWebLB, TYPE_VRA_IAAS_WEB_LOAD_BALANCER),
                        ResourceTier.LOGICAL, ResourceSubType.TAG);
            vraIaasWebLoadBalancer.addRelations(objectFactory.createRelation(loadBalancerSuperTag, PARENT));

            demServer.addRelations(objectFactory.createRelation(vraIaasWebLoadBalancer,
                        RelationType.SIBLING));

            Resource vRAIaasWebServer = objectFactory.createResource(true,
                        TYPE_VRA_IAAS_WEB,
                        getFullResourceName(vRAIaasWebLB, TYPE_VRA_IAAS_WEB),
                        ResourceTier.LOGICAL, ResourceSubType.TAG);
            demServer.addRelations(objectFactory.createRelation(vRAIaasWebServer,
                        RelationType.SIBLING));
        }

        if (!StringUtils.isEmpty(managerLB)) {
            Resource manager = objectFactory.createResource(true,
                        TYPE_VRA_IAAS_MANAGER_SERVER_LOAD_BALANCER,
                        getFullResourceName(managerLB, TYPE_VRA_IAAS_MANAGER_SERVER_LOAD_BALANCER),
                        ResourceTier.LOGICAL, ResourceSubType.TAG);

            manager.addRelations(objectFactory.createRelation(loadBalancerSuperTag, PARENT));

            demServer.addRelations(objectFactory.createRelation(manager,
                        RelationType.SIBLING));
            Resource managerServer = objectFactory.createResource(true,
                        TYPE_VRA_IAAS_MANAGER_SERVER,
                        getFullResourceName(managerLB, TYPE_VRA_IAAS_MANAGER_SERVER),
                        ResourceTier.LOGICAL, ResourceSubType.TAG);
            demServer.addRelations(objectFactory.createRelation(managerServer,
                        RelationType.SIBLING));
        }

        return demServer;
    }
}
