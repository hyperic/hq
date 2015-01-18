package org.hyperic.plugin.vrealize.automation;

import static com.vmware.hyperic.model.relations.RelationType.PARENT;
import static com.vmware.hyperic.model.relations.RelationType.SIBLING;
import static org.hyperic.plugin.vrealize.automation.VraConstants.CREATE_IF_NOT_EXIST;
import static org.hyperic.plugin.vrealize.automation.VraConstants.KEY_APPLICATION_NAME;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_LOAD_BALANCER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_SSO_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_APPLICATION;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VSPHERE_SSO_LOAD_BALANCER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VSPHERE_SSO_LOAD_BALANCER_TAG;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.DaemonDetector;
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
public class DiscoveryVSphereSSO extends DaemonDetector {

    private static final Log log = LogFactory.getLog(DiscoveryVSphereSSO.class);

    public static final String HOSTNAME_FILE_PATH =
                String.format("%s\\VMware\\CIS\\cfg\\vmware-sso\\hostname.txt", System.getenv("ProgramData"));
    private ObjectFactory factory;

    @Override
    public List<ServerResource> getServerResources(ConfigResponse platformConfig)
                throws PluginException {
        log.debug(String.format("[getServerResources] platformConfig = '%s'", platformConfig));
        @SuppressWarnings("unchecked")
        List<ServerResource> servers = super.getServerResources(platformConfig);

        String ssoLoadBalancerFqdn = getSsoLoadBalancerFqdn();

        log.debug(String.format("[getServerResources] ssoLoadBalancerFqdn = '%s'", ssoLoadBalancerFqdn));

        for (ServerResource server : servers) {
            String ssoName = server.getName();
            String ssoType = server.getType();
            log.debug(String.format("[getServerResources] sso server = '%s' sso Type = '%s'", ssoName, ssoType));

            Resource commonModel = getCommonModel(ssoName, ssoType, ssoLoadBalancerFqdn);
            String model = marshallCommonModel(commonModel);
            VRAUtils.setModelProperty(server, model);
        }
        return servers;
    }

    private String getSsoLoadBalancerFqdn() {
        String ssoLoadBalancerFqdn = VRAUtils.readFile(HOSTNAME_FILE_PATH);
        if (ssoLoadBalancerFqdn == null) {
            return null;
        }

        ssoLoadBalancerFqdn = VRAUtils.getFqdn(ssoLoadBalancerFqdn.trim());
        return ssoLoadBalancerFqdn;
    }

    public String marshallCommonModel(Resource resource) {
        ObjectFactory factory = new ObjectFactory();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        factory.saveModel(resource, baos);
        log.debug("[testMarshallAsJson] fos=" + baos.toString());
        return baos.toString();
    }

    private Resource getCommonModel(
                String ssoName, String ssoType, String ssoLoadBalancerFqdn) {
        factory = new ObjectFactory();
        Resource ssoServer = factory.createResource(false, ssoType, ssoName, ResourceTier.SERVER);
        ssoServer.setContextPropagationBarrier(true);

        Resource ssoServerGroup = factory.createLogicalResource(TYPE_SSO_TAG,
                    CommonModelUtils.getParametrizedName(KEY_APPLICATION_NAME));

        //TODO make the app type a variable as well, to support an unknown app.
        Resource vraApp = factory.createApplicationResource(TYPE_VRA_APPLICATION,
                    CommonModelUtils.getParametrizedName(KEY_APPLICATION_NAME));
        ssoServerGroup.addRelations(factory.createRelation(vraApp, PARENT, CREATE_IF_NOT_EXIST));

        ssoServer.addRelations(factory.createRelation(ssoServerGroup, RelationType.PARENT));

        createRelationLoadBalancer(ssoLoadBalancerFqdn, ssoServer, vraApp, ssoServerGroup);

        return ssoServer;
    }

    private void createRelationLoadBalancer(
                String ssoLoadBalancerFqdn, Resource ssoServer, Resource vraApp, Resource ssoServerGroup) {
        if (StringUtils.isBlank(ssoLoadBalancerFqdn)) {
            return;
        }

        Resource topLoadBalancerTag = factory.createLogicalResource(TYPE_LOAD_BALANCER_TAG,
                    CommonModelUtils.getParametrizedName(KEY_APPLICATION_NAME));
        topLoadBalancerTag.addRelations(factory.createRelation(vraApp, PARENT));

        Resource ssoLoadBalancerTag = factory.createLogicalResource(TYPE_VSPHERE_SSO_LOAD_BALANCER_TAG,
                    CommonModelUtils.getParametrizedName(KEY_APPLICATION_NAME));
        ssoLoadBalancerTag.addRelations(factory.createRelation(topLoadBalancerTag, PARENT, CREATE_IF_NOT_EXIST));

        Resource ssoLoadBalancer = factory.createResource(false, TYPE_VSPHERE_SSO_LOAD_BALANCER, ssoLoadBalancerFqdn,
                    ResourceTier.SERVER);
        ssoLoadBalancer.addRelations(factory.createRelation(ssoLoadBalancerTag, PARENT, CREATE_IF_NOT_EXIST));
        ssoLoadBalancer.addRelations(factory.createRelation(ssoServerGroup, PARENT, CREATE_IF_NOT_EXIST));
        ssoLoadBalancer.setContextPropagationBarrier(true);

        ssoServer.addRelations(factory.createRelation(ssoLoadBalancer, SIBLING));
    }

}
