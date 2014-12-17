package org.hyperic.plugin.vrealize.automation;

import static com.vmware.hyperic.model.relations.RelationType.PARENT;
import static com.vmware.hyperic.model.relations.ResourceTier.SERVER;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.getParameterizedName;
import static org.hyperic.plugin.vrealize.automation.VraConstants.CREATE_IF_NOT_EXIST;
import static org.hyperic.plugin.vrealize.automation.VraConstants.KEY_APPLICATION_NAME;
import static org.hyperic.plugin.vrealize.automation.VraConstants.KEY_VCO_LOAD_BALANCER_FQDN;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VCO_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_APPLICATION;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_VCO_LOAD_BALANCER;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.DaemonDetector;
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
 * @author imakhlin
 */
public class DiscoveryVCOAppServer extends DaemonDetector {

    private static final Log log = LogFactory.getLog(DiscoveryVCOAppServer.class);

    @Override
    public List<ServerResource> getServerResources(ConfigResponse platformConfig)
        throws PluginException {
        log.debug("[getServerResources] platformConfig=" + platformConfig);
        List<ServerResource> servers = super.getServerResources(platformConfig);

        for (ServerResource server : servers) {
            String srvName = server.getName();
            String platformFqdn = server.getPlatformFqdn();
            String srvType = server.getType();
            log.debug("[getServerResources] vCO server=" + srvName + " vCO Type=" + srvType + " platformFqdn="
                        + platformFqdn);

            Resource modelResource = getCommonModel(platformFqdn, srvName, srvType);
            String modelXml = VRAUtils.marshallResource(modelResource);
            VRAUtils.setModelProperty(server, modelXml);
        }
        return servers;
    }

    private static Resource getCommonModel(final String platformFqdn,
                                           String serverName,
                                           String serverType) {
        ObjectFactory factory = new ObjectFactory();

        Resource vcoServer =
                    factory.createResource(false, serverType, serverName, ResourceTier.SERVER);

        Resource serverGroup =
                    factory.createResource(true, TYPE_VCO_TAG,
                                getParameterizedName(KEY_APPLICATION_NAME, TYPE_VCO_TAG),
                                ResourceTier.LOGICAL,
                                ResourceSubType.TAG);

        Resource vraApp =
                    factory.createResource(false, TYPE_VRA_APPLICATION,
                                getParameterizedName(KEY_APPLICATION_NAME, TYPE_VRA_APPLICATION), ResourceTier.LOGICAL);

        vcoServer.addRelations(factory.createRelation(serverGroup, RelationType.PARENT));
        serverGroup.addRelations(factory.createRelation(vraApp, RelationType.PARENT));

        Resource topLoadBalancerTag =
                    VRAUtils.createLogialResource(factory, VraConstants.TYPE_VRA_LOAD_BALANCER_TAG,
                                VRAUtils.getParameterizedName(KEY_APPLICATION_NAME));
        topLoadBalancerTag.addRelations(factory.createRelation(vraApp, PARENT));

        Resource vcoLoadBalancerTag =
                    VRAUtils.createLogialResource(factory, VraConstants.TYPE_VRA_VCO_LOAD_BALANCER_TAG,
                                VRAUtils.getParameterizedName(KEY_APPLICATION_NAME));
        vcoLoadBalancerTag.addRelations(factory.createRelation(topLoadBalancerTag, PARENT, CREATE_IF_NOT_EXIST));

        Resource vcoLoadBalancer =
                    factory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_VCO_LOAD_BALANCER,
                                VRAUtils.getParameterizedName(KEY_VCO_LOAD_BALANCER_FQDN, TYPE_VRA_VCO_LOAD_BALANCER), SERVER);

        vcoLoadBalancer.addRelations(factory.createRelation(vcoLoadBalancerTag, PARENT, CREATE_IF_NOT_EXIST));

        vcoServer.addRelations(factory.createRelation(vcoLoadBalancer, RelationType.SIBLING));

        return vcoServer;
    }
}
