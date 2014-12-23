/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import static org.hyperic.plugin.vrealize.automation.VraConstants.KEY_APPLICATION_NAME;
import static org.hyperic.plugin.vrealize.automation.VraConstants.PROP_EXTENDED_REL_MODEL;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_LOAD_BALANCER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_SSO_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_APPLICATION;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_LOAD_BALANCER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_SERVER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_SERVER_LOAD_BALANCER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_SERVER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_VSPHERE_SSO;

import java.util.List;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
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

        List<ServerResource> servers = super.getServerResources(platformConfig);

        Properties props = VRAUtils.configFile("/etc/vcac/security.properties");
        String cspHost = props.getProperty("csp.host");
        String websso = props.getProperty("vmidentity.websso.host");

        if (websso != null) {
            websso = websso.substring(0, websso.indexOf(":"));
        }

        log.debug("[getServerResources] csp.host=" + cspHost);
        log.debug("[getServerResources] websso=" + websso);

        if (cspHost != null) {
            for (ServerResource server : servers) {
                String model =
                            VRAUtils.marshallResource(getResource(cspHost, websso, getPlatformName()));
                server.getProductConfig().setValue(PROP_EXTENDED_REL_MODEL,
                            new String(Base64.encodeBase64(model.getBytes())));

                // do not remove, why? please don't ask.
                server.setProductConfig(server.getProductConfig());
            }
        }
        return servers;
    }

    private Resource getResource(String lbHostName,
                                 String websso,
                                 String platform) {
        ObjectFactory factory = new ObjectFactory();

        Resource vraApplication = factory.createResource(Boolean.TRUE, TYPE_VRA_APPLICATION,
                    VRAUtils.getFullResourceName(lbHostName, TYPE_VRA_APPLICATION), ResourceTier.LOGICAL,
                    ResourceSubType.TAG);
        Relation rl_toVraApp = factory.createRelation(vraApplication, RelationType.PARENT, Boolean.TRUE);
        vraApplication.addProperty(factory.createProperty(KEY_APPLICATION_NAME, lbHostName));

        // SSO
        Resource ssoGroup =
                    factory.createResource(Boolean.TRUE, TYPE_SSO_TAG,
                                VRAUtils.getFullResourceName(lbHostName, TYPE_SSO_TAG),
                                ResourceTier.LOGICAL, ResourceSubType.TAG);
        Resource vraSsoServer = factory.createResource(Boolean.FALSE, TYPE_VRA_VSPHERE_SSO,
                    VRAUtils.getFullResourceName(websso, TYPE_VRA_VSPHERE_SSO), ResourceTier.SERVER);

        Relation rl_ssoServer = factory.createRelation(vraSsoServer, RelationType.SIBLING, Boolean.FALSE);
        Relation rl_ssoGroup = factory.createRelation(ssoGroup, RelationType.PARENT, Boolean.FALSE);

        vraSsoServer.getRelations().add(rl_ssoGroup);
        ssoGroup.getRelations().add(rl_toVraApp);

        // VCO Server
        /*
        Resource vcoGroup = factory.createResource(Boolean.TRUE, TYPE_VCO_TAG,
                    VRAUtils.getFullResourceName(lbHostName, TYPE_VCO_TAG),
                    ResourceTier.LOGICAL, ResourceSubType.TAG);
        Resource vcoServer = factory.createResource(Boolean.FALSE, TYPE_VRA_VCO,
                    VRAUtils.getParameterizedName(VraConstants.KEY_VCO_SERVER_FQDN, TYPE_VRA_VCO),
                    ResourceTier.SERVER);

        Relation rl_vcoGroup = factory.createRelation(vcoGroup, RelationType.PARENT, Boolean.FALSE);
        Relation rl_vcoServer = factory.createRelation(vcoServer, RelationType.SIBLING, Boolean.FALSE);
        vcoServer.addRelations(rl_vcoGroup);
        vcoGroup.addRelations(rl_toVraApp);
         */

        // VRA Server
        Resource vraServersGroup = factory.createResource(Boolean.TRUE, TYPE_VRA_SERVER_TAG,
                    VRAUtils.getFullResourceName(lbHostName, TYPE_VRA_SERVER_TAG), ResourceTier.LOGICAL,
                    ResourceSubType.TAG);
        Relation rl_ToVraServersGroup = factory.createRelation(vraServersGroup, RelationType.PARENT, Boolean.TRUE);

        vraServersGroup.addRelations(rl_toVraApp);

        Resource vRaServer = factory.createResource(Boolean.FALSE, TYPE_VRA_SERVER,
                    VRAUtils.getFullResourceName(platform, TYPE_VRA_SERVER), ResourceTier.SERVER);

        vRaServer.addRelations(rl_ssoServer /*, rl_vcoServer*/, rl_ToVraServersGroup);

        if (!StringUtils.isEmpty(lbHostName) && !lbHostName.equals(platform)) {
            log.debug("[getResource] platform name (" + platform + ") and load balancer fqdn (" + lbHostName
                        + ") are not similar. This is distributed deployment.");
            // Distributed vRA cluster has load balancer
            Resource topLbGroup = factory.createResource(Boolean.TRUE, TYPE_LOAD_BALANCER_TAG,
                        VRAUtils.getFullResourceName(lbHostName, TYPE_LOAD_BALANCER_TAG),
                        ResourceTier.LOGICAL,
                        ResourceSubType.TAG);

            Resource vraLbServer = factory.createResource(Boolean.FALSE, TYPE_VRA_SERVER_LOAD_BALANCER,
                        VRAUtils.getFullResourceName(lbHostName, TYPE_VRA_SERVER_LOAD_BALANCER),
                        ResourceTier.SERVER);
            Resource vraLbServerGroup = factory.createResource(Boolean.TRUE, TYPE_VRA_LOAD_BALANCER_TAG,
                        VRAUtils.getFullResourceName(lbHostName, TYPE_VRA_LOAD_BALANCER_TAG),
                        ResourceTier.LOGICAL,
                        ResourceSubType.TAG);

            vraLbServer.addRelations(factory.createRelation(vraLbServerGroup, RelationType.PARENT, Boolean.TRUE));
            vraLbServerGroup.addRelations(factory.createRelation(topLbGroup, RelationType.PARENT, Boolean.TRUE));
            topLbGroup.addRelations(rl_toVraApp);

            vRaServer.addRelations(factory.createRelation(vraLbServer, RelationType.SIBLING, Boolean.TRUE));
        }

        return vRaServer;
    }

}
