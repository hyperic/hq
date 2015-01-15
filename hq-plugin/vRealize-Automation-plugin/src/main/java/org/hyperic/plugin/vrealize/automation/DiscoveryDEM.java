package org.hyperic.plugin.vrealize.automation;

import com.vmware.hyperic.model.relations.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.ServerResource;

import static com.vmware.hyperic.model.relations.RelationType.PARENT;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.marshallResource;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.setModelProperty;
import static org.hyperic.plugin.vrealize.automation.VraConstants.*;

/**
 * @author Tomer Shetah
 */
public class DiscoveryDEM extends Discovery {
    private static final Log log = LogFactory.getLog(DiscoveryDEM.class);
    private static final String appName = CommonModelUtils.getParametrizedName(KEY_APPLICATION_NAME);

    @Override
    protected ServerResource newServerResource(
                long pid, String exe) {
        ServerResource server = super.newServerResource(pid, exe);
        log.debug("[newServerResource] pid=" + pid);

        String configFilePath = exe + ".config";
        log.debug("[newServerResource] configFile=" + configFilePath);

        String vRAIaasWebLB =
                    VRAUtils.executeXMLQuery("//appSettings/add[@key='repositoryAddress']/@value", configFilePath);
        if (!StringUtils.isEmpty(vRAIaasWebLB)) {
            vRAIaasWebLB = VRAUtils.getFqdn(vRAIaasWebLB);
        }
        log.debug("[newServerResource] vRAIaasWebLB (repositoryAddress) = '" + vRAIaasWebLB + "'");

        String managerServerLoadBalancerFqdn =
                    VRAUtils.executeXMLQuery("//system.serviceModel/client/endpoint/@address", configFilePath);
        if (!StringUtils.isEmpty(managerServerLoadBalancerFqdn)) {
            managerServerLoadBalancerFqdn = VRAUtils.getFqdn(managerServerLoadBalancerFqdn);
        }
        log.debug("[newServerResource] l (DynamicOps_Vmps_Agent_Core_VMPSAgentService_ProxyAgentService) = '"
                    + managerServerLoadBalancerFqdn + "'");

        Resource modelResource = getCommonModel(server, vRAIaasWebLB, managerServerLoadBalancerFqdn);
        String modelXml = marshallResource(modelResource);
        setModelProperty(server, modelXml);

        return server;
    }

    private static Resource getCommonModel(
                ServerResource server, String vRAIaasWebLB, String managerServerLoadBalancerFqdn) {
        ObjectFactory objectFactory = new ObjectFactory();

        Resource demServer =
                    objectFactory.createResource(false, server.getType(), server.getName(), ResourceTier.SERVER);
        Resource demGroup = objectFactory.createLogicalResource(TYPE_DEM_SERVER_GROUP, appName);
        Resource application = objectFactory.createLogicalResource(TYPE_VRA_APPLICATION, appName);

        demServer.addRelations(objectFactory.createRelation(demGroup, RelationType.PARENT));

        demGroup.addRelations(objectFactory.createRelation(application, RelationType.PARENT));

        Resource loadBalancerSuperTag = objectFactory.createLogicalResource(TYPE_LOAD_BALANCER_TAG,
                    CommonModelUtils.getParametrizedName(KEY_APPLICATION_NAME));

        createRelationIaasWebOrLoadBalancer(vRAIaasWebLB, objectFactory, demServer, loadBalancerSuperTag);

        createRelationManagerServerOrLoadBalancer(managerServerLoadBalancerFqdn, objectFactory, demServer,
                    loadBalancerSuperTag);

        return demServer;
    }

    private static void createRelationManagerServerOrLoadBalancer(
                String managerServerOrLoadBalancerFqdn,
                ObjectFactory objectFactory,
                Resource demServer,
                Resource loadBalancerSuperTag) {
        if (StringUtils.isEmpty(managerServerOrLoadBalancerFqdn)) {
            return;
        }

        Resource managerServerLoadBalancer =
                    objectFactory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_MANAGER_SERVER_LOAD_BALANCER,
                                managerServerOrLoadBalancerFqdn, ResourceTier.SERVER);
        Resource vraManagerServerLoadBalancerTag =
                    objectFactory.createLogicalResource(TYPE_VRA_MANAGER_SERVER_LOAD_BALANCER_TAG, appName);
        managerServerLoadBalancer.addRelations(objectFactory.createRelation(vraManagerServerLoadBalancerTag, PARENT));
        vraManagerServerLoadBalancerTag.addRelations(objectFactory.createRelation(loadBalancerSuperTag, PARENT));

        Resource managerServer =
                    objectFactory.createResource(true, TYPE_VRA_MANAGER_SERVER, managerServerOrLoadBalancerFqdn,
                                ResourceTier.SERVER);
        Resource managerServerTag = objectFactory.createLogicalResource(TYPE_VRA_MANAGER_SERVER_TAG, appName);
        managerServer.addRelations(objectFactory.createRelation(managerServerTag, RelationType.PARENT));
        demServer.addRelations(objectFactory.createRelation(managerServerLoadBalancer, RelationType.SIBLING),
                    objectFactory.createRelation(managerServer, RelationType.SIBLING));
        managerServerLoadBalancer.addRelations(objectFactory.createRelation(managerServerTag, RelationType.PARENT));
    }

    private static void createRelationIaasWebOrLoadBalancer(
                String vRAIaasWebOrLoadBalancerFqdn,
                ObjectFactory objectFactory,
                Resource demServer,
                Resource loadBalancerSuperTag) {
        if (StringUtils.isEmpty(vRAIaasWebOrLoadBalancerFqdn)) {
            return;
        }

        Resource vraIaasWebLoadBalancer =
                    objectFactory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_IAAS_WEB_LOAD_BALANCER,
                                vRAIaasWebOrLoadBalancerFqdn, ResourceTier.SERVER);
        Resource vraIaasWebLoadBalancerTag =
                    objectFactory.createLogicalResource(TYPE_VRA_IAAS_WEB_LOAD_BALANCER_TAG, appName);
        vraIaasWebLoadBalancer.addRelations(objectFactory.createRelation(vraIaasWebLoadBalancerTag, PARENT));
        vraIaasWebLoadBalancerTag.addRelations(objectFactory.createRelation(loadBalancerSuperTag, PARENT));
        demServer.addRelations(objectFactory.createRelation(vraIaasWebLoadBalancer, RelationType.SIBLING));

        Resource vRAIaasWebServer =
                    objectFactory.createResource(CREATE_IF_NOT_EXIST, TYPE_VRA_IAAS_WEB, vRAIaasWebOrLoadBalancerFqdn,
                                ResourceTier.SERVER);
        Resource vRAIaasWebServerTag = objectFactory.createLogicalResource(TYPE_VRA_IAAS_WEB_TAG, appName);
        vRAIaasWebServer.addRelations(objectFactory.createRelation(vRAIaasWebServerTag, RelationType.PARENT));
        demServer.addRelations(objectFactory.createRelation(vRAIaasWebServer, RelationType.SIBLING));
        vraIaasWebLoadBalancer.addRelations(
                    objectFactory.createRelation(vRAIaasWebServerTag, RelationType.PARENT, Boolean.TRUE));
    }

    /* inline unit test
    @Test
    public void test() {
        ServerResource server = new ServerResource();
        server.setName("111");
        server.setType("222");
        Resource modelResource = getCommonModel(server, "AAA", "BBB");
        String modelXml = marshallResource(modelResource);
        Assert.assertNotNull(modelXml);

        System.out.println(modelXml);
    }
    */

}
