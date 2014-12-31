/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import static org.hyperic.plugin.vrealize.automation.VRAUtils.configFile;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.createLogialResource;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.getFullResourceName;
import static org.hyperic.plugin.vrealize.automation.VraConstants.CREATE_IF_NOT_EXIST;
import static org.hyperic.plugin.vrealize.automation.VraConstants.KEY_APPLICATION_NAME;
import static org.hyperic.plugin.vrealize.automation.VraConstants.PROP_EXTENDED_REL_MODEL;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_APP_SERVICES_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_LOAD_BALANCER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_SSO_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_APPLICATION;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_APP_SERVICES;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_LOAD_BALANCER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_SERVER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_SERVER_LOAD_BALANCER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_SERVER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_VSPHERE_SSO;

import java.util.List;
import java.util.Properties;

//import org.junit.Assert;
//import org.junit.Test;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.hyperic.model.relations.ObjectFactory;
import com.vmware.hyperic.model.relations.Relation;
import com.vmware.hyperic.model.relations.RelationType;
import com.vmware.hyperic.model.relations.Resource;
import com.vmware.hyperic.model.relations.ResourceSubType;
import com.vmware.hyperic.model.relations.ResourceTier;

/**
 *
 * @author laullon
 */
public class DiscoveryVRAServer extends Discovery {

    private static final Log log = LogFactory.getLog(DiscoveryVRAServer.class);

    @Override
    public List<ServerResource> getServerResources(ConfigResponse platformConfig)
                throws PluginException {
        log.debug("[getServerResources] platformConfig=" + platformConfig);
        String platformFqdn = platformConfig.getValue("platform.fqdn");
        log.debug("[getServerResources] platformFqdn=" + platformFqdn);

        @SuppressWarnings("unchecked")
        List<ServerResource> servers = super.getServerResources(platformConfig);

        if (servers.isEmpty())
            return servers;

        Properties props = configFile("/etc/vcac/security.properties");
        String cspHost = props.getProperty("csp.host");
        String webSso = props.getProperty("vmidentity.websso.host");

        webSso = VRAUtils.getFQDN(webSso);
        cspHost = VRAUtils.getFQDN(cspHost);

        log.debug("[getServerResources] csp.host=" + cspHost);
        log.debug("[getServerResources] websso=" + webSso);

        if (StringUtils.isBlank(cspHost))
            return servers;

        String applicationServicesPath = getApplicationServicePath(platformFqdn);

        for (ServerResource server : servers) {
            String model = VRAUtils.marshallResource(
                        getCommonModel(cspHost, webSso, getPlatformName(), applicationServicesPath));
            server.getProductConfig().setValue(PROP_EXTENDED_REL_MODEL, new String(Base64.encodeBase64(model.getBytes())));

            // do not remove, why? please don't ask.
            server.setProductConfig(server.getProductConfig());
        }

        return servers;
    }

    private String getApplicationServicePath(String platformFqdn) {
        String applicationServicesPath = null;
        try {
            String applicationServicesXML = VRAUtils.getWGet(
                        String.format("https://%s/component-registry/services/status/current", platformFqdn));
            applicationServicesPath = VRAUtils.getApplicationServicePathFromJson(applicationServicesXML);
            log.debug("Application services host is  = " + applicationServicesPath);
        } catch (Exception e) {
            log.debug("Failed to get getApplicationServicePath");
        }
        return applicationServicesPath;
    }

    private Resource getCommonModel(String lbHostName, String webSso, String platform, String applicationServicesHost) {
        ObjectFactory factory = new ObjectFactory();

        Resource vraApplication = createLogialResource(factory, TYPE_VRA_APPLICATION, lbHostName);
        vraApplication.addProperty(factory.createProperty(KEY_APPLICATION_NAME, lbHostName));

        Relation relationToVraApp = factory.createRelation(vraApplication, RelationType.PARENT, Boolean.TRUE);

        Resource vRaServer = factory.createResource(Boolean.FALSE, TYPE_VRA_SERVER, getFullResourceName(platform, TYPE_VRA_SERVER), ResourceTier.SERVER);
        Resource vraServersGroup = factory.createResource(Boolean.TRUE, TYPE_VRA_SERVER_TAG, getFullResourceName(lbHostName, TYPE_VRA_SERVER_TAG), ResourceTier.LOGICAL,
                    ResourceSubType.TAG);
        vRaServer.addRelations(factory.createRelation(vraServersGroup, RelationType.PARENT));
        vraServersGroup.addRelations(relationToVraApp);

        createApplicationServiceRelations(lbHostName, applicationServicesHost, factory, vraApplication, vRaServer);

        createWebSsoRelations(lbHostName, webSso, factory, relationToVraApp, vRaServer);

        createLoadBalancerRelations(lbHostName, platform, factory, relationToVraApp, vRaServer, vraServersGroup);

        return vRaServer;
    }

    private void createLoadBalancerRelations(
                String lbHostName,
                String platform,
                ObjectFactory factory,
                Relation relationToVraApp,
                Resource vRaServer,
                Resource vraServersGroup) {

        if (!StringUtils.isEmpty(lbHostName) && !lbHostName.equals(platform)) {
            // log.debug("[getResource] platform name (" + platform + ") and load balancer fqdn (" + lbHostName
            // + ") are not similar. This is distributed deployment.");

            // Distributed vRA cluster has load balancer

            Resource topLbGroup = createLogialResource(factory, TYPE_LOAD_BALANCER_TAG, lbHostName);

            Resource vraLbServer = factory.createResource(Boolean.FALSE, TYPE_VRA_SERVER_LOAD_BALANCER,
                        getFullResourceName(lbHostName, TYPE_VRA_SERVER_LOAD_BALANCER), ResourceTier.SERVER);
            Resource vraLbServerGroup = createLogialResource(factory, TYPE_VRA_LOAD_BALANCER_TAG, lbHostName);

            vraLbServer.addRelations(factory.createRelation(vraLbServerGroup, RelationType.PARENT, Boolean.TRUE));
            vraLbServer.addRelations(factory.createRelation(vraServersGroup, RelationType.PARENT, Boolean.TRUE));
            vraLbServerGroup.addRelations(factory.createRelation(topLbGroup, RelationType.PARENT, Boolean.TRUE));
            topLbGroup.addRelations(relationToVraApp);
            vRaServer.addRelations(factory.createRelation(vraLbServer, RelationType.SIBLING, Boolean.TRUE));

        }
    }

    private void createWebSsoRelations(
                String lbHostName, String webSso, ObjectFactory factory, Relation relationToVraApp, Resource vRaServer) {

        if (StringUtils.isNotBlank(webSso)) {
            Resource ssoGroup = createLogialResource(factory, TYPE_SSO_TAG, lbHostName);
            Resource vraSsoServer = factory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_VSPHERE_SSO,
                        getFullResourceName(webSso, TYPE_VRA_VSPHERE_SSO), ResourceTier.SERVER);
            vraSsoServer.setContextPropagationBarrier(true);

            vraSsoServer.addRelations(factory.createRelation(ssoGroup, RelationType.PARENT));
            ssoGroup.addRelations(relationToVraApp);

            vRaServer.addRelations(factory.createRelation(vraSsoServer, RelationType.SIBLING));
        }
    }

    private void createApplicationServiceRelations(
                String lbHostName,
                String applicationServicesHost,
                ObjectFactory factory,
                Resource vraApplication,
                Resource vRaServer) {

        if (StringUtils.isNotBlank(applicationServicesHost)) {
            Resource appServicesGroup = createLogialResource(factory, TYPE_APP_SERVICES_TAG, lbHostName);
            Resource vraAppServicesServer = factory.createResource(Boolean.FALSE, TYPE_VRA_APP_SERVICES, VRAUtils.getFullResourceName(applicationServicesHost, TYPE_VRA_APP_SERVICES),
                        ResourceTier.SERVER);
            vraAppServicesServer.addRelations(factory.createRelation(appServicesGroup, RelationType.PARENT));
            appServicesGroup.addRelations(factory.createRelation(vraApplication, RelationType.PARENT));
            vRaServer.addRelations(factory.createRelation(vraAppServicesServer, RelationType.SIBLING));
        }
    }

    //inline unit test
    //    @Test
    //    public void test() {
    //        ServerResource server = new ServerResource();
    //        server.setName("THE_SERVER");
    //        server.setType("THE_SERVER_TYPE");
    //        Resource modelResource = getCommonModel("THE_APP", "THE_SSO", "THE_PLATFORM", "shmulik.com");
    //        String modelXml = marshallResource(modelResource);
    //        Assert.assertNotNull(modelXml);
    //
    //        System.out.println(modelXml);
    //    }
}
