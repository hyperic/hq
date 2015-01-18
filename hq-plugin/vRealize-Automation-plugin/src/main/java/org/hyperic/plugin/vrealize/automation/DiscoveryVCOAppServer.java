package org.hyperic.plugin.vrealize.automation;

import static com.vmware.hyperic.model.relations.RelationType.PARENT;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.configFile;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.getDnsNames;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.marshallResource;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.setModelProperty;
import static org.hyperic.plugin.vrealize.automation.VraConstants.CREATE_IF_NOT_EXIST;
import static org.hyperic.plugin.vrealize.automation.VraConstants.KEY_APPLICATION_NAME;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VCO_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_APPLICATION;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_DATABASES_GROUP;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_VCO_LOAD_BALANCER;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.hyperic.model.relations.CommonModelUtils;
import com.vmware.hyperic.model.relations.ObjectFactory;
import com.vmware.hyperic.model.relations.RelationType;
import com.vmware.hyperic.model.relations.Resource;
import com.vmware.hyperic.model.relations.ResourceTier;

/**
 * @author imakhlin
 */
public class DiscoveryVCOAppServer extends Discovery {

    private static final Log log = LogFactory.getLog(DiscoveryVCOAppServer.class);
    private static final String appName = CommonModelUtils.getParametrizedName(KEY_APPLICATION_NAME);

    @Override
    public List<ServerResource> getServerResources(ConfigResponse platformConfig)
                throws PluginException {
        log.debug("[getServerResources] platformConfig=" + platformConfig);
        String platformFqdn = platformConfig.getValue("platform.fqdn");
        VRAUtils.setLocalFqdn(platformFqdn);
        log.debug("[getServerResources] platformFqdn=" + platformFqdn);

        @SuppressWarnings("unchecked")
        List<ServerResource> servers = super.getServerResources(platformConfig);

        for (ServerResource server : servers) {
            String srvName = server.getName();
            String srvType = server.getType();
            log.debug("[getServerResources] vCO server=" + srvName + " vCO Type=" + srvType);

            Properties cfg = configFile("/etc/vco/app-server/vmo.properties");
            String jdbcURL = cfg.getProperty("database.url", "").replaceAll("\\:", ":");
            log.debug("[getServerResources] jdbcURL='" + jdbcURL + "'");

            String databaseServerFqdn = getDatabaseFqdn(jdbcURL);
            log.debug("[getServerResources] databaseServerFqdn=" + databaseServerFqdn);

            final Collection<String> vcoLoadBalancerFqdns = getVcoLoadBalancerFqdns(platformFqdn);
            Resource modelResource =
                        getCommonModel(srvName, srvType, vcoLoadBalancerFqdns, databaseServerFqdn, platformFqdn);
            String modelXml = marshallResource(modelResource);
            setModelProperty(server, modelXml);
        }
        return servers;
    }

    /**
     * @return
     */
    private Collection<String> getVcoLoadBalancerFqdns(String vcoServerName) {
        return getDnsNames(String.format("https://%s:8281", vcoServerName));
    }

    private static Resource getCommonModel(
                String serverName,
                String serverType,
                Collection<String> loadBalancerFqdns,
                String databaseServerFqdn,
                String platformFqdn) {
        ObjectFactory factory = new ObjectFactory();

        Resource vcoServer = factory.createResource(!CREATE_IF_NOT_EXIST, serverType, serverName, ResourceTier.SERVER);
        Resource serverGroup = factory.createLogicalResource(TYPE_VCO_TAG, appName);
        Resource vraApp = factory.createApplicationResource(TYPE_VRA_APPLICATION, appName);

        vcoServer.addRelations(factory.createRelation(serverGroup, PARENT));
        serverGroup.addRelations(factory.createRelation(vraApp, PARENT));

        createRelationshipLoadBalancer(loadBalancerFqdns, factory, vcoServer, serverGroup, vraApp);

        createRelationshipDataBase(databaseServerFqdn, factory, vcoServer, vraApp, platformFqdn);

        return vcoServer;
    }

    private static void createRelationshipDataBase(
                String databaseServerFqdn,
                ObjectFactory factory,
                Resource vcoServer,
                Resource vraApp,
                String platformFqdn) {
        Resource vraDatabasesGroup = factory.createLogicalResource(TYPE_VRA_DATABASES_GROUP, appName);

        vraDatabasesGroup.addRelations(factory.createRelation(vraApp, RelationType.PARENT));

        Resource databaseServerHostWin =
                    factory.createResource(!CREATE_IF_NOT_EXIST, VraConstants.TYPE_WINDOWS, databaseServerFqdn,
                                ResourceTier.PLATFORM);
        Resource databaseServerHostLinux =
                    factory.createResource(!CREATE_IF_NOT_EXIST, VraConstants.TYPE_LINUX, databaseServerFqdn,
                                ResourceTier.PLATFORM);

        databaseServerHostWin.addRelations(factory.createRelation(vraDatabasesGroup, RelationType.PARENT));
        databaseServerHostLinux.addRelations(factory.createRelation(vraDatabasesGroup, RelationType.PARENT));

        vcoServer.addRelations(
                    factory.createRelation(databaseServerHostWin, VRAUtils.getDataBaseRalationType(databaseServerFqdn)),
                    factory.createRelation(databaseServerHostLinux,
                                VRAUtils.getDataBaseRalationType(databaseServerFqdn)));
    }

    private static void createRelationshipLoadBalancer(
                Collection<String> loadBalancerFqdns,
                ObjectFactory factory,
                Resource vcoServer,
                Resource serverGroup,
                Resource vraApp) {

        if (loadBalancerFqdns == null || loadBalancerFqdns.size() <= 0) {
            log.debug("Unable to get the VCO load balancer FQDN name");
            return;
        }

        Resource topLoadBalancerTag = factory.createLogicalResource(VraConstants.TYPE_LOAD_BALANCER_TAG, appName);
        topLoadBalancerTag.addRelations(factory.createRelation(vraApp, PARENT));

        Resource vcoLoadBalancerTag =
                    factory.createLogicalResource(VraConstants.TYPE_VRA_VCO_LOAD_BALANCER_TAG, appName);
        vcoLoadBalancerTag.addRelations(factory.createRelation(topLoadBalancerTag, PARENT, CREATE_IF_NOT_EXIST));

        for (String loadBalancerFqdn : loadBalancerFqdns) {
            Resource vcoLoadBalancer =
                        factory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_VCO_LOAD_BALANCER, loadBalancerFqdn,
                                    ResourceTier.SERVER);

            vcoLoadBalancer.addRelations(factory.createRelation(vcoLoadBalancerTag, PARENT, CREATE_IF_NOT_EXIST),
                        (factory.createRelation(serverGroup, RelationType.PARENT, CREATE_IF_NOT_EXIST)));

            vcoServer.addRelations(factory.createRelation(vcoLoadBalancer, RelationType.SIBLING));
        }
    }

    private String getDatabaseFqdn(String jdbcConnectionString) {

        // jdbcURL='jdbc:jtds:sqlserver://mssql-a2-bg-01.refarch.eng.vmware.com:1433/vCO;domain=refarch.eng.vmware.com;useNTLMv2=true'

        return VRAUtils.getFqdn(jdbcConnectionString, AddressExtractorFactory.getDatabaseServerFqdnExtractor());
    }

    /* inline unit test
    @Test
    public void test() {
        ServerResource server = new ServerResource();
        server.setName("THE_SERVER");
        server.setType("THE_SERVER_TYPE");
        Collection<String> loadBalancerFqdns = new ArrayList<String>();
        loadBalancerFqdns.add("vco.lb.com");
        String jdbcUrl = getDatabaseFqdn("jdbc:jtds:sqlserver://mssql-a2-bg-01.refarch.eng.vmware.com:1433/vCO;domain=refarch.eng.vmware.com;useNTLMv2=true");

        Resource modelResource = getCommonModel(server.getName(), server.getType(), loadBalancerFqdns, jdbcUrl);
        String modelXml = marshallResource(modelResource);
        Assert.assertNotNull(modelXml);

        System.out.println(modelXml);
    }
    */

}
