/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.hyperic.model.relations.ObjectFactory;
import com.vmware.hyperic.model.relations.Relation;
import com.vmware.hyperic.model.relations.RelationType;
import com.vmware.hyperic.model.relations.Resource;
import com.vmware.hyperic.model.relations.ResourceSubType;
import com.vmware.hyperic.model.relations.ResourceTier;
import static org.hyperic.plugin.vrealize.automation.VraConstants.*;

/**
 *
 * @author laullon
 */
public class DiscoveryVRAServer extends DaemonDetector {

    private static final Log log = LogFactory.getLog(DiscoveryVRAServer.class);

    @Override
    public List<ServerResource> getServerResources(ConfigResponse platformConfig)
            throws PluginException {
        log.debug("[getServerResources] platformConfig=" + platformConfig);

        List<ServerResource> servers = super.getServerResources(platformConfig);

        Properties props = VRAUtils.configFile("/etc/vcac/security.properties");
        String cspHost = props.getProperty("csp.host");
        String websso = props.getProperty("vmidentity.websso.host");

        // TODO: German to implement VCO component discovery
        String vcoServerFqdn = props.getProperty("vco.server.fqdn", "VCO discovery not implemented");

        if (websso != null){
            websso = websso.substring(0, websso.indexOf(":"));
        }

        log.debug("[getServerResources] csp.host=" + cspHost);
        log.debug("[getServerResources] websso=" + websso);
        log.debug("[getServerResources] vcoServerFqdn=" + vcoServerFqdn);

        if (cspHost != null) {
            for (ServerResource server : servers) {
                String model = VRAUtils.marshallResource(getResource(cspHost, websso, vcoServerFqdn, getPlatformName()));
                server.getProductConfig().setValue(PROP_EXTENDED_REL_MODEL,
                        new String(Base64.encodeBase64(model.getBytes())));

                ConfigResponse c = new ConfigResponse();
                c.setValue(PROP_EXTENDED_REL_MODEL, new String(Base64.encodeBase64(model.getBytes())));

                // do not remove, why? please don't ask.
                server.setProductConfig(server.getProductConfig());
                server.setCustomProperties(c);
            }
        }
        return servers;
    }

    private Resource getResource(String lbHostName,
            String websso,
            String vcoServerFqdn,
            String platform) {
        ObjectFactory factory = new ObjectFactory();

        Resource lb
                = factory.createResource(Boolean.TRUE, TYPE_LOAD_BALANCER,
                        VRAUtils.getFullResourceName(lbHostName, TYPE_LOAD_BALANCER),
                        ResourceTier.LOGICAL,
                        ResourceSubType.TAG);

        Resource vralbServer
                = factory.createResource(Boolean.FALSE, TYPE_VRA_VA_LOAD_BALANCER,
                        VRAUtils.getFullResourceName(lbHostName, TYPE_VRA_VA_LOAD_BALANCER),
                        ResourceTier.SERVER);
        Resource vralbServerGroup
                = factory.createResource(Boolean.TRUE, TYPE_VRA_LOAD_BALANCER,
                        VRAUtils.getFullResourceName(lbHostName, TYPE_VRA_LOAD_BALANCER),
                        ResourceTier.LOGICAL,
                        ResourceSubType.TAG);

        Resource vraApplication
                = factory.createResource(Boolean.TRUE, TYPE_VRA_APPLICATION,
                        VRAUtils.getFullResourceName(lbHostName, TYPE_VRA_APPLICATION), ResourceTier.LOGICAL,
                        ResourceSubType.TAG);

        vraApplication.addIdentifiers(factory.createIdentifier(KEY_APPLICATION_NAME, lbHostName));

        Relation rl_toVraApp = factory.createRelation(vraApplication, RelationType.PARENT, Boolean.TRUE);
        Relation rl_toGenericLbGroup = factory.createRelation(lb, RelationType.PARENT, Boolean.TRUE);
        Relation rl_toVraLbServer = factory.createRelation(vralbServer, RelationType.SIBLING, Boolean.TRUE);
        Relation rl_toVraLbServerGroup = factory.createRelation(vralbServerGroup, RelationType.PARENT, Boolean.TRUE);

        vralbServer.addRelations(rl_toVraLbServerGroup);
        vralbServerGroup.addRelations(rl_toGenericLbGroup);
        lb.addRelations(rl_toVraApp);

        // SSO
        Resource ssoGroup
                = factory.createResource(Boolean.TRUE, TYPE_SSO, VRAUtils.getFullResourceName(lbHostName, TYPE_SSO),
                        ResourceTier.LOGICAL, ResourceSubType.TAG);
        Resource vraSsoServer
                = factory.createResource(Boolean.FALSE, TYPE_VRA_VSPHERE_SSO,
                        VRAUtils.getFullResourceName(websso, TYPE_VRA_VSPHERE_SSO), ResourceTier.SERVER);

        Relation rl_ssoServer = factory.createRelation(vraSsoServer, RelationType.SIBLING, Boolean.FALSE);
        Relation rl_ssoGroup = factory.createRelation(ssoGroup, RelationType.PARENT, Boolean.FALSE);

        vraSsoServer.getRelations().add(rl_ssoGroup);
        ssoGroup.getRelations().add(rl_toVraApp);

        // VCO Server
        Resource vcoGroup
                = factory.createResource(Boolean.TRUE, TYPE_VCO,
                        VRAUtils.getFullResourceName(lbHostName, TYPE_VCO),
                        ResourceTier.LOGICAL, ResourceSubType.TAG);
        Resource vcoServer
                = factory.createResource(Boolean.FALSE, TYPE_VRA_VCO,
                        VRAUtils.getFullResourceName(vcoServerFqdn, TYPE_VRA_VCO),
                        ResourceTier.SERVER);

        Relation rl_vco = factory.createRelation(vcoGroup, RelationType.PARENT, Boolean.FALSE);
        Relation rl_server = factory.createRelation(vcoServer, RelationType.SIBLING, Boolean.FALSE);
        vcoServer.addRelations(rl_vco, rl_toVraApp);

        // VRA Server
        Resource vraServersGroup
                = factory.createResource(Boolean.TRUE, TYPE_VRA_SERVER_GROUP,
                        VRAUtils.getFullResourceName(lbHostName, TYPE_VRA_SERVER_GROUP), ResourceTier.LOGICAL,
                        ResourceSubType.TAG);
        Relation rl_asg = factory.createRelation(vraServersGroup, RelationType.PARENT, Boolean.TRUE);

        vraServersGroup.addRelations(rl_toVraApp);

        Resource vRaServer
                = factory.createResource(Boolean.FALSE, TYPE_VRA_SERVER,
                        VRAUtils.getFullResourceName(platform, TYPE_VRA_SERVER), ResourceTier.SERVER);
        vRaServer.addRelations(rl_toVraLbServer, rl_ssoServer, rl_server, rl_asg);

        return vRaServer;
    }

}
