package org.hyperic.plugin.vrealize.automation;

import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_ITBM_SERVER_GROUP;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_APPLICATION;

import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.hyperic.model.relations.ObjectFactory;
import com.vmware.hyperic.model.relations.RelationType;
import com.vmware.hyperic.model.relations.Resource;
import com.vmware.hyperic.model.relations.ResourceSubType;
import com.vmware.hyperic.model.relations.ResourceTier;

/**
 *
 * @author Sharon Rozinsky
 */
public class DiscoveryITBMServer extends Discovery {
    private static final String CSP_COMPONENT_REGISTRY_URL = "csp.component.registry.url";
    private static final String SECURITY_PROPERTIES_FILE_PATH =
                "/usr/local/tcserver/vfabric-tc-server-standard/itbm-server/conf/security.properties";
    private static final Log log = LogFactory.getLog(DiscoveryITBMServer.class);

    @Override
    public List<ServerResource> getServerResources(ConfigResponse platformConfig)
        throws PluginException {
        log.debug("[getServerResources] platformConfig=" + platformConfig);
        List<ServerResource> servers = super.getServerResources(platformConfig);

        Properties props = VRAUtils.configFile(SECURITY_PROPERTIES_FILE_PATH);
        String applicationName = VRAUtils.getFQDN(props
                    .getProperty(CSP_COMPONENT_REGISTRY_URL));
        log.debug("[getServerResources] " + CSP_COMPONENT_REGISTRY_URL + " = "
                    + applicationName);

        if (applicationName != null) {
            for (ServerResource serverResource : servers) {
                Resource modelResource = getCommonModel(serverResource,
                            applicationName);
                String modelXml = VRAUtils.marshallResource(modelResource);
                VRAUtils.setModelProperty(serverResource, modelXml);
            }
        }
        return servers;
    }

    private Resource getCommonModel(ServerResource server,
                                    String applicationName) {
        String itbmServerGroupName = VRAUtils.getFullResourceName(applicationName, TYPE_ITBM_SERVER_GROUP);

        String applicationTagName = VRAUtils.getFullResourceName(applicationName, TYPE_VRA_APPLICATION);
        ObjectFactory objectFactory = new ObjectFactory();

        Resource itbmServer = objectFactory.createResource(false,
                    server.getType(), server.getName(), ResourceTier.SERVER);
        Resource itbmGroup = objectFactory.createResource(true,
                    TYPE_ITBM_SERVER_GROUP, itbmServerGroupName, ResourceTier.LOGICAL,
                    ResourceSubType.TAG);
        Resource application = objectFactory.createResource(true,
                    TYPE_VRA_APPLICATION, applicationTagName, ResourceTier.LOGICAL,
                    ResourceSubType.TAG);

        itbmServer.addRelations(objectFactory.createRelation(itbmGroup,
                    RelationType.PARENT));
        itbmGroup.addRelations(objectFactory.createRelation(application,
                    RelationType.PARENT));

        return itbmServer;
    }

}
