/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import static com.vmware.hyperic.model.relations.RelationType.PARENT;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.createLogialResource;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.executeXMLQuery;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.getFqdn;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.getParameterizedName;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.marshallResource;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.setModelProperty;
import static org.hyperic.plugin.vrealize.automation.VraConstants.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.ServerResource;

import com.vmware.hyperic.model.relations.ObjectFactory;
import com.vmware.hyperic.model.relations.RelationType;
import com.vmware.hyperic.model.relations.Resource;
import com.vmware.hyperic.model.relations.ResourceTier;

/**
 * @author glaullon
 */
public class DiscoveryVRAProxyAgent extends Discovery {

    private static final Log log = LogFactory.getLog(DiscoveryVRAProxyAgent.class);

    @Override
    protected ServerResource newServerResource(
                long pid, String exe) {
        ServerResource server = super.newServerResource(pid, exe);
        log.debug("[newServerResource] pid=" + pid);

        String configFilePath = exe + ".config";
        log.debug("[newServerResource] configFile=" + configFilePath);

        String vRAIaasWebOrLoadBalancer =
                    executeXMLQuery("//appSettings/add[@key='repositoryAddress']/@value", configFilePath);
        if (!StringUtils.isEmpty(vRAIaasWebOrLoadBalancer)) {
            vRAIaasWebOrLoadBalancer = getFqdn(vRAIaasWebOrLoadBalancer);
        }
        log.debug("[newServerResource] vRAIaasWebLB (repositoryAddress) = '" + vRAIaasWebOrLoadBalancer + "'");

        String managerServerOrLoadBalancer = executeXMLQuery(
                    "//applicationSettings/*/setting[@name='DynamicOps_Vmps_Agent_Core_VMPSAgentService_ProxyAgentService']/value/text()",
                    configFilePath);
        if (!StringUtils.isEmpty(managerServerOrLoadBalancer)) {
            managerServerOrLoadBalancer = getFqdn(managerServerOrLoadBalancer);
        }
        log.debug("[newServerResource] managerLB (DynamicOps_Vmps_Agent_Core_VMPSAgentService_ProxyAgentService) = '"
                    + managerServerOrLoadBalancer + "'");

        Resource modelResource = getCommonModel(server, vRAIaasWebOrLoadBalancer, managerServerOrLoadBalancer);
        String modelXml = marshallResource(modelResource);
        setModelProperty(server, modelXml);

        return server;
    }

    public static Resource getCommonModel(
                ServerResource server, String vRAIaasWebOrLoadBalancer, String managerServerOrLoadBalancer) {
        String proxyServerGroupName = getParameterizedName(KEY_APPLICATION_NAME);

        String parameterizedApplicationTagName = getParameterizedName(KEY_APPLICATION_NAME);
        ObjectFactory objectFactory = new ObjectFactory();

        Resource proxyServer = objectFactory.createResource(!CREATE_IF_NOT_EXIST, server.getType(), server.getName(),
                    ResourceTier.SERVER);
        Resource proxyGroup = createLogialResource(objectFactory, TYPE_PROXY_AGENT_SERVER_GROUP, proxyServerGroupName);

        Resource application =
                    createLogialResource(objectFactory, TYPE_VRA_APPLICATION, parameterizedApplicationTagName);

        proxyServer.addRelations(objectFactory.createRelation(proxyGroup, RelationType.PARENT));
        proxyGroup.addRelations(objectFactory.createRelation(application, RelationType.PARENT));

        Resource loadBalancerSuperTag = createLogialResource(objectFactory, TYPE_LOAD_BALANCER_TAG,
                    getParameterizedName(KEY_APPLICATION_NAME));

        createRelationIaasWebOrLoadBalancer(vRAIaasWebOrLoadBalancer, parameterizedApplicationTagName, objectFactory,
                    proxyServer, loadBalancerSuperTag);

        createRelationManagerServerOrLoadBalncer(managerServerOrLoadBalancer, parameterizedApplicationTagName,
                    objectFactory, proxyServer, loadBalancerSuperTag);

        return proxyServer;
    }

    private static void createRelationManagerServerOrLoadBalncer(
                String managerServerOrLoadBalancer,
                String parameterizedApplicationTagName,
                ObjectFactory objectFactory,
                Resource proxyServer,
                Resource loadBalancerSuperTag) {
        if (StringUtils.isEmpty(managerServerOrLoadBalancer))
            return;
        Resource managerLoadBalancer =
                    objectFactory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_MANAGER_SERVER_LOAD_BALANCER,
                                VRAUtils.getFullResourceName(managerServerOrLoadBalancer,
                                            TYPE_VRA_MANAGER_SERVER_LOAD_BALANCER), ResourceTier.SERVER);
        proxyServer.addRelations(objectFactory.createRelation(managerLoadBalancer, RelationType.SIBLING));

        Resource managerLoadBalancerTag = createLogialResource(objectFactory, TYPE_VRA_MANAGER_SERVER_LOAD_BALANCER_TAG,
                    getParameterizedName(KEY_APPLICATION_NAME));
        managerLoadBalancer.addRelations(objectFactory.createRelation(managerLoadBalancerTag, PARENT));
        managerLoadBalancerTag.addRelations(objectFactory.createRelation(loadBalancerSuperTag, PARENT));

        Resource managerServer = objectFactory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_MANAGER_SERVER,
                    VRAUtils.getFullResourceName(managerServerOrLoadBalancer, TYPE_VRA_MANAGER_SERVER),
                    ResourceTier.SERVER);
        Resource managerServerTag =
                    createLogialResource(objectFactory, TYPE_VRA_MANAGER_SERVER_TAG, parameterizedApplicationTagName);
        managerServer.addRelations(objectFactory.createRelation(managerServerTag, RelationType.PARENT));
        proxyServer.addRelations(objectFactory.createRelation(managerServer, RelationType.SIBLING));
        managerLoadBalancer.addRelations(objectFactory.createRelation(managerServerTag, PARENT));
    }

    private static void createRelationIaasWebOrLoadBalancer(
                String vRAIaasWebOrLoadBalancer,
                String parameterizedApplicationTagName,
                ObjectFactory objectFactory,
                Resource proxyServer,
                Resource loadBalancerSuperTag) {
        if (StringUtils.isEmpty(vRAIaasWebOrLoadBalancer))
            return;
        Resource vraIaasWebLoadBalancer =
                    objectFactory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_IAAS_WEB_LOAD_BALANCER,
                                VRAUtils.getFullResourceName(vRAIaasWebOrLoadBalancer, TYPE_VRA_IAAS_WEB_LOAD_BALANCER),
                                ResourceTier.SERVER);
        Resource iaasWebLoadBalancerTag = createLogialResource(objectFactory, TYPE_VRA_IAAS_WEB_TAG,
                    getParameterizedName(KEY_APPLICATION_NAME));
        vraIaasWebLoadBalancer.addRelations(objectFactory.createRelation(iaasWebLoadBalancerTag, PARENT));
        iaasWebLoadBalancerTag.addRelations(objectFactory.createRelation(loadBalancerSuperTag, PARENT));

        Resource vRAIaasWebServer = objectFactory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_IAAS_WEB,
                    VRAUtils.getFullResourceName(vRAIaasWebOrLoadBalancer, TYPE_VRA_IAAS_WEB), ResourceTier.SERVER);
        Resource vRAIaasWebServerTag =
                    createLogialResource(objectFactory, TYPE_VRA_IAAS_WEB_TAG, parameterizedApplicationTagName);
        vRAIaasWebServer.addRelations(objectFactory.createRelation(vRAIaasWebServerTag, RelationType.PARENT));
        proxyServer.addRelations(objectFactory.createRelation(vRAIaasWebServer, RelationType.SIBLING),
                    objectFactory.createRelation(vraIaasWebLoadBalancer, RelationType.SIBLING));
        vraIaasWebLoadBalancer.addRelations(objectFactory.createRelation(vRAIaasWebServerTag, PARENT));
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
