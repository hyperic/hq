/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import static com.vmware.hyperic.model.relations.RelationType.CHILD;
import static com.vmware.hyperic.model.relations.RelationType.PARENT;
import static com.vmware.hyperic.model.relations.RelationType.SIBLING;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.configFile;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.executeXMLQuery;
import static org.hyperic.plugin.vrealize.automation.VraConstants.CREATE_IF_NOT_EXIST;
import static org.hyperic.plugin.vrealize.automation.VraConstants.KEY_APPLICATION_NAME;
import static org.hyperic.plugin.vrealize.automation.VraConstants.PROP_EXTENDED_REL_MODEL;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_APP_SERVICES_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_LOAD_BALANCER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_SSO_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VCO_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_APPLICATION;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_APP_SERVICES;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_DATABASES_GROUP;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_WEB;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_WEB_LOAD_BALANCER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_WEB_LOAD_BALANCER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_WEB_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_LOAD_BALANCER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_SERVER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_SERVER_LOAD_BALANCER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_SERVER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_VCO;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_VSPHERE_SSO;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VSPHERE_SSO_LOAD_BALANCER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VSPHERE_SSO_LOAD_BALANCER_TAG;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.hyperic.model.relations.CommonModelUtils;
import com.vmware.hyperic.model.relations.ObjectFactory;
import com.vmware.hyperic.model.relations.Relation;
import com.vmware.hyperic.model.relations.Resource;
import com.vmware.hyperic.model.relations.ResourceTier;

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
        String vraApplicationFqdn = props.getProperty("csp.host");
        String webSsoLoadBalancer = props.getProperty("vmidentity.websso.host");

        webSsoLoadBalancer = VRAUtils.getFqdn(webSsoLoadBalancer);
        vraApplicationFqdn = VRAUtils.getFqdn(vraApplicationFqdn);

        log.debug("[getServerResources] vraApplicationFqdn=" + vraApplicationFqdn);
        log.debug("[getServerResources] webSsoLoadBalancer=" + webSsoLoadBalancer);

        if (StringUtils.isBlank(vraApplicationFqdn))
            return servers;

        String applicationServicesPath = getApplicationServicePath(platformFqdn);

        // Get Database URL
        String vraDatabaseURL = executeXMLQuery("//Resource[@name=\"jdbc/cafe\"]/@url", "/etc/vcac/server.xml");
        log.debug("[discoverServices] vraDatabaseURL=" + vraDatabaseURL);

        String databaseServerFqdn = getDatabaseFqdn(vraDatabaseURL);
        log.debug("[discoverServices] databaseServerFqdn=" + databaseServerFqdn);

        for (ServerResource server : servers) {
            String model = VRAUtils.marshallResource(
                        getCommonModel(vraApplicationFqdn, webSsoLoadBalancer, getPlatformName(), applicationServicesPath,
                                    databaseServerFqdn));
            server.getProductConfig().setValue(PROP_EXTENDED_REL_MODEL,
                        new String(Base64.encodeBase64(model.getBytes())));

            // do not remove, why? please don't ask.
            server.setProductConfig(server.getProductConfig());
        }

        return servers;
    }

    /**
     * @return Returns vRA Server FQDN (not Load Balancer)
     */
    private Collection<String> getIaaSFqdns() {
        Collection<String> iaasFqdns = new ArrayList<String>();
        VRAUtils.VraVersion vraVersion = VRAUtils.getVraVersion(isWin32());
        if (vraVersion.getMinor() > 1) {

            final String[] commandToGetJsonWithIaas =
                        new String[] { "/usr/sbin/vcac-config", "-v", "cluster-config", "-list" };
            String includingJsonWithIaas = new String();
            try {
                includingJsonWithIaas = VRAUtils.runCommandLine(commandToGetJsonWithIaas);
            } catch (PluginException ex) {
                log.error("[getIaaSFqdns] " + ex, ex);
            }
            String componentsRegistryJson = includingJsonWithIaas.split("\n")[1]; // The string is of the format:
                                                                                  // ---BEGIN---\n{JSON FILE}\n---END---
            log.debug("[getIaaSFqdns] The json is: " + componentsRegistryJson);

            try {
                Collection<String> iaasFqdnsOrIps =
                            VRAUtils.getNodeHostUrlsFromClusterConfigJson(componentsRegistryJson, "IAAS");

                for (String fqdnOrIp : iaasFqdnsOrIps) {
                    iaasFqdns.add(VRAUtils.getFqdn(fqdnOrIp));
                }

                log.debug("Following IaaS Service FQDNs were found => " + iaasFqdnsOrIps);
            } catch (Exception e) {
                log.error("Unable to get IaaS node host names from vRA configuration", e);
            }
        }

        return iaasFqdns;
    }

    private String getApplicationServicePath(String platformFqdn) {
        String applicationServicesPath = null;
        try {
            String componentRegistry =
                        VRAUtils.getWGet(
                                    String.format("https://%s/component-registry/services/status/current?$top=50",
                                                platformFqdn));

            Collection<String> applicationServices =
                        VRAUtils.getFqdnFromComponentRegistryJson(componentRegistry, "Application Services");

            applicationServicesPath = getFirstElementOrNull(applicationServices);

            log.debug("Application services host is  = " + applicationServicesPath);
        } catch (Exception e) {
            log.debug("Failed to get getApplicationServicePath", e);
        }
        return applicationServicesPath;
    }

    /**
     * @param Collection of String
     * @return only first element for given collection
     */
    private String getFirstElementOrNull(Collection<String> collection) {
        String result = null;
        if (collection != null) {
            for (String element : collection) {
                result = element;
                break;
            }
        }
        return result;
    }

    private String getIaasLoadBalancerFqdn(String platformFqdn) {
        String iaasLoadBalancerFqdn = null;
        try {
            String iaasLoadBalancerFqdnJson = VRAUtils.getWGet(
                        String.format("https://%s/component-registry/services/status/current?$top=50", platformFqdn));

            Collection<String> iaasLoadBalancerFqdns =
                        VRAUtils.getFqdnFromComponentRegistryJson(iaasLoadBalancerFqdnJson, "iaas-service");

            iaasLoadBalancerFqdn = getFirstElementOrNull(iaasLoadBalancerFqdns);

            log.debug("Iaas Load Balancer Fqdn is = " + iaasLoadBalancerFqdn);
        } catch (Exception e) {
            log.debug("Failed to get Iaas Load Balancer Fqdn");
            return null;
        }
        return iaasLoadBalancerFqdn;
    }

    private Resource getCommonModel(
                                    String vraApplicationFqdn,
                                    String webSsoOrSsoLoadBalancer,
                                    String platformName,
                                    String applicationServicesHost,
                                    String vraDatabaseFqdn) {
        ObjectFactory factory = new ObjectFactory();

        Resource vraApplication = factory.createApplicationResource(TYPE_VRA_APPLICATION, vraApplicationFqdn);
        vraApplication.addProperty(factory.createProperty(KEY_APPLICATION_NAME, vraApplicationFqdn));

        Relation relationToVraApp = factory.createRelation(vraApplication, PARENT, Boolean.TRUE);

        Resource vRaServer = factory.createResource(Boolean.FALSE, TYPE_VRA_SERVER, platformName, ResourceTier.SERVER);
        Resource vraServersGroup = factory.createLogicalResource(TYPE_VRA_SERVER_TAG, vraApplicationFqdn);
        vRaServer.addRelations(factory.createRelation(vraServersGroup, PARENT));
        vraServersGroup.addRelations(relationToVraApp);

        createApplicationServiceRelations(vraApplicationFqdn, applicationServicesHost, factory, vraApplication,
                    vRaServer);

        createRelationSsoOrLoadBalancer(vraApplicationFqdn, webSsoOrSsoLoadBalancer, factory, relationToVraApp,
                    vRaServer);

        createLoadBalancerRelations(vraApplicationFqdn, platformName, factory, relationToVraApp, vRaServer,
                    vraServersGroup);

        createVcoRelations(factory, vraApplication);

        createVraDatabaseRelations(factory, vraApplicationFqdn, vRaServer, vraApplication, vraDatabaseFqdn);

        createIaaSWebRelations(factory, vraApplicationFqdn, vRaServer, vraApplication);

        return vRaServer;
    }

    private void createIaaSWebRelations(ObjectFactory factory,
                                        String vRaApplicationFqdn,
                                        Resource vRaServer,
                                        Resource vraApplication) {

        log.debug("[createIaaSWebRelations] vRaApplicationFqdn = " + vRaApplicationFqdn);

        Collection<String> iaasFqdns = getIaaSFqdns();

        for (String iaasWebServerFqdn : iaasFqdns) {
            log.debug("[createIaaSWebRelations] iaasFqdn = " + iaasWebServerFqdn);
            Resource iaasWebServer = factory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_IAAS_WEB,
                        iaasWebServerFqdn, ResourceTier.SERVER);

            Resource iaasWebServerTag = factory.createLogicalResource(TYPE_VRA_IAAS_WEB_TAG, vRaApplicationFqdn);
            iaasWebServer.addRelations(factory.createRelation(iaasWebServerTag, PARENT));

            iaasWebServerTag.addRelations(factory.createRelation(vraApplication, PARENT));

            vRaServer.addRelations(factory.createRelation(iaasWebServer, SIBLING));
        }

        VRAUtils.VraVersion vraVersion = VRAUtils.getVraVersion(isWin32());
        createRelationIaasWebLoadBalancer(factory, vRaApplicationFqdn, vRaServer, vraApplication, vraVersion);

    }

    private void createRelationIaasWebLoadBalancer(ObjectFactory factory,
                                                   String vRaApplicationFqdn,
                                                   Resource vRaServer,
                                                   Resource vraApplication,
                                                   VRAUtils.VraVersion vraVersion) {
        if (vraVersion.getMinor() <= 1) {
            return;
        }

        String iaasWebLoadBalancerFqdn = getIaasLoadBalancerFqdn(vRaApplicationFqdn);
        if (StringUtils.isBlank(iaasWebLoadBalancerFqdn)) {
            return;
        }

        log.debug("[createRelationIaasWebLoadBalancer] iaasLoadBalancerFqdn = " + iaasWebLoadBalancerFqdn);
        Resource iaasWebLoadBalancer = factory.createResource(false, TYPE_VRA_IAAS_WEB_LOAD_BALANCER,
                    iaasWebLoadBalancerFqdn, ResourceTier.SERVER);
        vRaServer.addRelations(factory.createRelation(iaasWebLoadBalancer, SIBLING));
        Resource iaasWebLoadBalancerTag =
                    factory.createLogicalResource(TYPE_VRA_IAAS_WEB_LOAD_BALANCER_TAG, vRaApplicationFqdn);
        iaasWebLoadBalancer.addRelations(factory.createRelation(iaasWebLoadBalancerTag, PARENT));
        Resource loadBalancerSuperTag =
                    factory.createLogicalResource(TYPE_LOAD_BALANCER_TAG, vRaApplicationFqdn);
        iaasWebLoadBalancerTag.addRelations(factory.createRelation(loadBalancerSuperTag, PARENT));
        loadBalancerSuperTag.addRelations(factory.createRelation(vraApplication, PARENT));
    }

    private void createVraDatabaseRelations(
                                            ObjectFactory factory,
                                            String applicationName,
                                            Resource vraServer,
                                            Resource vraApplication,
                                            String databaseServerFqdn) {

        Resource vraDatabasesGroup = factory.createLogicalResource(TYPE_VRA_DATABASES_GROUP, applicationName);

        vraDatabasesGroup.addRelations(factory.createRelation(vraApplication, PARENT));

        Resource databaseServerHostWin =
                    factory.createResource(!CREATE_IF_NOT_EXIST, VraConstants.TYPE_WINDOWS, databaseServerFqdn,
                                ResourceTier.PLATFORM);
        Resource databaseServerHostLinux =
                    factory.createResource(!CREATE_IF_NOT_EXIST, VraConstants.TYPE_LINUX, databaseServerFqdn,
                                ResourceTier.PLATFORM);

        databaseServerHostWin.addRelations(factory.createRelation(vraDatabasesGroup, PARENT));
        databaseServerHostLinux.addRelations(factory.createRelation(vraDatabasesGroup, PARENT));


        if (!VRAUtils.areFqdnsEquivalent(VRAUtils.getLocalFqdn(), databaseServerFqdn)) {
            vraServer.addRelations(
                        factory.createRelation(databaseServerHostWin,
                                    VRAUtils.getDataBaseRalationType(databaseServerFqdn)),
                        factory.createRelation(databaseServerHostLinux,
                                    VRAUtils.getDataBaseRalationType(databaseServerFqdn)));
        }

    }

    /**
     * @param factory
     */
    private void createVcoRelations(
                                    ObjectFactory factory,
                                    Resource vraApplication) {
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
                                                 String vraApplicationName,
                                                 String webSsoOrSsoLoadBalancer,
                                                 ObjectFactory factory,
                                                 Relation relationToVraApp,
                                                 Resource vRaServer) {

        if (StringUtils.isBlank(webSsoOrSsoLoadBalancer)) {
            return;
        }

        Resource ssoGroup = factory.createLogicalResource(TYPE_SSO_TAG, vraApplicationName);
        Resource vraSsoServer =
                    factory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_VSPHERE_SSO, webSsoOrSsoLoadBalancer,
                                ResourceTier.SERVER);

        vraSsoServer.setContextPropagationBarrier(true);
        vraSsoServer.addRelations(factory.createRelation(ssoGroup, PARENT));
        ssoGroup.addRelations(relationToVraApp);
        vRaServer.addRelations(factory.createRelation(vraSsoServer, SIBLING));

        createRelationSsoLoadBalancer(webSsoOrSsoLoadBalancer, vraApplicationName, factory, relationToVraApp,
                    vRaServer, ssoGroup);
    }

    private void createRelationSsoLoadBalancer(
                                               String webSso,
                                               String vraApplicationName,
                                               ObjectFactory factory,
                                               Relation relationToVraApp,
                                               Resource vRaServer,
                                               Resource ssoGroup) {
        Resource topLoadBalancerTag = factory.createLogicalResource(TYPE_LOAD_BALANCER_TAG, appName);
        topLoadBalancerTag.addRelations(relationToVraApp);

        Resource ssoLoadBalancerTag = factory.createLogicalResource(TYPE_VSPHERE_SSO_LOAD_BALANCER_TAG,
                    CommonModelUtils.getParametrizedName(KEY_APPLICATION_NAME));
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
