/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import com.vmware.hyperic.model.relations.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;

import java.util.List;
import java.util.Properties;

import static com.vmware.hyperic.model.relations.RelationType.*;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.configFile;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.executeXMLQuery;
import static org.hyperic.plugin.vrealize.automation.VraConstants.*;

//import org.junit.Assert;
//import org.junit.Test;

/**
 * @author laullon
 */
public class DiscoveryVRAServer extends Discovery {

    private static final Log log = LogFactory.getLog(DiscoveryVRAServer.class);
    private final String appName = CommonModelUtils.getParametrizedName(KEY_APPLICATION_NAME);

    @Override
    public List<ServerResource> getServerResources(ConfigResponse platformConfig)
                throws PluginException {
        log.debug("[getServerResources] platformConfig=" + platformConfig);
        String platformFqdn = platformConfig.getValue("platform.fqdn");
        VRAUtils.setLocalFqdn(platformFqdn);
        log.debug("[getServerResources] platformFqdn=" + platformFqdn);

        @SuppressWarnings("unchecked")
        List<ServerResource> servers = super.getServerResources(platformConfig);

        if (servers.isEmpty())
            return servers;

        Properties props = configFile("/etc/vcac/security.properties");
        String cspHost = props.getProperty("csp.host");
        String webSso = props.getProperty("vmidentity.websso.host");

        webSso = VRAUtils.getFqdn(webSso);
        cspHost = VRAUtils.getFqdn(cspHost);

        log.debug("[getServerResources] csp.host=" + cspHost);
        log.debug("[getServerResources] websso=" + webSso);

        if (StringUtils.isBlank(cspHost))
            return servers;

        String applicationServicesPath = getApplicationServicePath(platformFqdn);

        // Get Database URL
        String vraDatabaseURL = executeXMLQuery("//Resource[@name=\"jdbc/cafe\"]/@url", "/etc/vcac/server.xml");
        log.debug("[discoverServices] vraDatabaseURL=" + vraDatabaseURL);

        String databaseServerFqdn = getDatabaseFqdn(vraDatabaseURL);
        log.debug("[discoverServices] databaseServerFqdn=" + databaseServerFqdn);

        for (ServerResource server : servers) {
            String model = VRAUtils.marshallResource(
                        getCommonModel(cspHost, webSso, getPlatformName(), applicationServicesPath,
                                    databaseServerFqdn));
            server.getProductConfig().setValue(PROP_EXTENDED_REL_MODEL,
                        new String(Base64.encodeBase64(model.getBytes())));

            // do not remove, why? please don't ask.
            server.setProductConfig(server.getProductConfig());
        }

        return servers;
    }

    private String getApplicationServicePath(String platformFqdn) {
        String applicationServicesPath = null;
        try {
            String applicationServicesXML = VRAUtils.getWGet(
                        String.format("https://%s/component-registry/services/status/current?$top=50", platformFqdn));
            applicationServicesPath = VRAUtils.getApplicationServicePathFromJson(applicationServicesXML);
            log.debug("Application services host is  = " + applicationServicesPath);
        } catch (Exception e) {
            log.debug("Failed to get getApplicationServicePath");
        }
        return applicationServicesPath;
    }

    private Resource getCommonModel(
                String localFqdn,
                String webSso,
                String platformName,
                String applicationServicesHost,
                String vraDatabaseFqdn) {
        ObjectFactory factory = new ObjectFactory();

        Resource vraApplication = factory.createLogicalResource(TYPE_VRA_APPLICATION, localFqdn);
        vraApplication.addProperty(factory.createProperty(KEY_APPLICATION_NAME, localFqdn));

        Relation relationToVraApp = factory.createRelation(vraApplication, PARENT, Boolean.TRUE);

        Resource vRaServer = factory.createResource(Boolean.FALSE, TYPE_VRA_SERVER, platformName, ResourceTier.SERVER);
        Resource vraServersGroup = factory.createLogicalResource(TYPE_VRA_SERVER_TAG, localFqdn);
        vRaServer.addRelations(factory.createRelation(vraServersGroup, PARENT));
        vraServersGroup.addRelations(relationToVraApp);

        createApplicationServiceRelations(localFqdn, applicationServicesHost, factory, vraApplication, vRaServer);

        createRelationSsoOrLoadBalancer(localFqdn, webSso, factory, relationToVraApp, vRaServer);

        createLoadBalancerRelations(localFqdn, platform, factory, relationToVraApp, vRaServer, vraServersGroup);

        createVcoRelations(factory, vraApplication);

        createVraDatabaseRelations(factory, localFqdn, vRaServer, vraApplication, vraDatabaseFqdn);

        createIaaSWebRelations(factory, vraApplicationFqdn, vRaServer, vraApplication);

        return vRaServer;
    }

    private void createIaaSWebRelations(ObjectFactory factory,
                                        String vRaApplicationFqdn,
                                        Resource vRaServer,
                                        Resource vraApplication) {

        log.debug("[createIaaSWebRelations] vRaApplicationFqdn = " + vRaApplicationFqdn);

        Vector<String> iaasFqdns = getIaaSFqdns();

        log.debug("[createIaaSWebRelations] vRaApplicationFqdn = " + vRaApplicationFqdn);

        for (String iaasWebServerFqdn : iaasFqdns) {
            Resource iaasWebServer = factory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_IAAS_WEB,
                        VRAUtils.getFullResourceName(iaasWebServerFqdn, TYPE_VRA_IAAS_WEB), SERVER);

            Resource iaasWebServerTag = VRAUtils.createLogicalResource(factory, TYPE_VRA_IAAS_WEB_TAG, vRaApplicationFqdn);
            iaasWebServer.addRelations(factory.createRelation(iaasWebServerTag, PARENT));

            iaasWebServerTag.addRelations(factory.createRelation(vraApplication, PARENT));

            vRaServer.addRelations(factory.createRelation(iaasWebServer, SIBLING));
        }

    }

    private void createVraDatabaseRelations(
                ObjectFactory factory,
                String applicationName,
                Resource vraServer,
                Resource vraApplication,
                String databaseServerFqdn) {
        Resource vraDatabasesGroup = createLogicalResource(factory, TYPE_VRA_DATABASES_GROUP, applicationName);

        vraDatabasesGroup.addRelations(factory.createRelation(vraApplication, PARENT));

        Resource databaseServerHostWin =
                    factory.createResource(!CREATE_IF_NOT_EXIST, VraConstants.TYPE_WINDOWS, databaseServerFqdn,
                                ResourceTier.PLATFORM);
        Resource databaseServerHostLinux =
                    factory.createResource(!CREATE_IF_NOT_EXIST, VraConstants.TYPE_LINUX, databaseServerFqdn,
                                ResourceTier.PLATFORM);

        databaseServerHostWin.addRelations(factory.createRelation(vraDatabasesGroup, PARENT));
        databaseServerHostLinux.addRelations(factory.createRelation(vraDatabasesGroup, PARENT));

        vraServer.addRelations(
                    factory.createRelation(databaseServerHostWin, VRAUtils.getDataBaseRalationType(databaseServerFqdn)),
                    factory.createRelation(databaseServerHostLinux,
                                VRAUtils.getDataBaseRalationType(databaseServerFqdn)));

    }

    /**
     * @param factory
     */
    private void createVcoRelations(
                ObjectFactory factory, Resource vraApplication) {
        // VCO Server

        Resource vcoGroup = factory.createLogicalResource(TYPE_VCO_TAG, vraApplication.getName());
        Resource vcoServer = factory.createResource(false, TYPE_VRA_VCO,
                    CommonModelUtils.getParametrizedName(VraConstants.KEY_VCO_SERVER_FQDN), ResourceTier.SERVER);

        vcoServer.addRelations(factory.createRelation(vcoGroup, PARENT));
        vcoGroup.addRelations(factory.createRelation(vraApplication, CHILD));
    }

    private void createLoadBalancerRelations(
                String lbHostName,
                String platform,
                ObjectFactory factory,
                Relation relationToVraApp,
                Resource vRaServer,
                Resource vraServersGroup) {

        if (StringUtils.isBlank(lbHostName) || lbHostName.equals(platform)) {
            return;
        }

        Resource topLbGroup = factory.createLogicalResource(TYPE_LOAD_BALANCER_TAG, lbHostName);
        Resource vraLbServer =
                    factory.createResource(false, TYPE_VRA_SERVER_LOAD_BALANCER, lbHostName, ResourceTier.SERVER);
        Resource vraLbServerGroup = factory.createLogicalResource(TYPE_VRA_LOAD_BALANCER_TAG, lbHostName);
        vraLbServer.addRelations(factory.createRelation(vraLbServerGroup, PARENT, true));
        vraLbServer.addRelations(factory.createRelation(vraServersGroup, PARENT, true));
        vraLbServerGroup.addRelations(factory.createRelation(topLbGroup, PARENT, true));
        topLbGroup.addRelations(relationToVraApp);
        vRaServer.addRelations(factory.createRelation(vraLbServer, SIBLING, true));
    }

    private void createRelationSsoOrLoadBalancer(
                String lbHostName,
                String webSso,
                ObjectFactory factory,
                Relation relationToVraApp,
                Resource vRaServer) {

        if (StringUtils.isBlank(webSso)) {
            return;
        }

        Resource ssoGroup = factory.createLogicalResource(TYPE_SSO_TAG, lbHostName);
        Resource vraSsoServer =
                    factory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_VSPHERE_SSO, webSso, ResourceTier.SERVER);
        vraSsoServer.setContextPropagationBarrier(true);
        vraSsoServer.addRelations(factory.createRelation(ssoGroup, PARENT));
        ssoGroup.addRelations(relationToVraApp);
        vRaServer.addRelations(factory.createRelation(vraSsoServer, SIBLING));

        createRelationSsoLoadBalancer(webSso, factory, relationToVraApp, vRaServer, ssoGroup);
    }

    private void createRelationSsoLoadBalancer(
                String webSso,
                ObjectFactory factory,
                Relation relationToVraApp,
                Resource vRaServer,
                Resource ssoGroup) {
        Resource topLoadBalancerTag = factory.createLogicalResource(TYPE_LOAD_BALANCER_TAG, appName);
        topLoadBalancerTag.addRelations(relationToVraApp);

        Resource ssoLoadBalancerTag = VRAUtils.createLogicalResource(factory, TYPE_VSPHERE_SSO_LOAD_BALANCER_TAG,
                    VRAUtils.getParameterizedName(KEY_APPLICATION_NAME));
        ssoLoadBalancerTag.addRelations(factory.createRelation(topLoadBalancerTag, PARENT, CREATE_IF_NOT_EXIST));

        Resource ssoLoadBalancer =
                    factory.createResource(false, TYPE_VSPHERE_SSO_LOAD_BALANCER, webSso, ResourceTier.SERVER);
        ssoLoadBalancer.setContextPropagationBarrier(true);

        ssoLoadBalancer.addRelations(factory.createRelation(ssoLoadBalancerTag, PARENT, CREATE_IF_NOT_EXIST));
        ssoLoadBalancer.addRelations(factory.createRelation(ssoGroup, PARENT, CREATE_IF_NOT_EXIST));
        vRaServer.addRelations(factory.createRelation(ssoLoadBalancer, SIBLING));
    }

    private void createApplicationServiceRelations(
                String lbHostName,
                String applicationServicesHost,
                ObjectFactory factory,
                Resource vraApplication,
                Resource vRaServer) {

        if (StringUtils.isBlank(applicationServicesHost)) {
            return;
        }

        Resource appServicesGroup = factory.createLogicalResource(TYPE_APP_SERVICES_TAG, lbHostName);
        Resource vraAppServicesServer =
                    factory.createResource(Boolean.FALSE, TYPE_VRA_APP_SERVICES, applicationServicesHost,
                                ResourceTier.SERVER);
        vraAppServicesServer.addRelations(factory.createRelation(appServicesGroup, PARENT));
        appServicesGroup.addRelations(factory.createRelation(vraApplication, PARENT));
        vRaServer.addRelations(factory.createRelation(vraAppServicesServer, SIBLING));
    }

    private String getDatabaseFqdn(String jdbcConnectionString) {

        // url="jdbc:postgresql://ra-psql-a-01.refarch.eng.vmware.com:5432/vcac"

        return VRAUtils.getFqdn(jdbcConnectionString, AddressExtractorFactory.getDatabaseServerFqdnExtractor());
    }

    // inline unit test
    /*
    @Test
    public void test() {
        ServerResource server = new ServerResource();
        server.setName("THE_SERVER");
        server.setType("THE_SERVER_TYPE");
        String vraDatabaseServerFqdn = getDatabaseFqdn("jdbc:postgresql://ra-psql-a-01.refarch.eng.vmware.com:5432/vcac");
        Resource modelResource = getCommonModel("THE_APP", "THE_SSO", "THE_PLATFORM", "shmulik.com", vraDatabaseServerFqdn);
        String modelXml = VRAUtils.marshallResource(modelResource);
        Assert.assertNotNull(modelXml);

        System.out.println(modelXml);
    }
     */
}
