package org.hyperic.plugin.vrealize.automation;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
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
 * @author srozinsky
 */
public class DiscoveryVSphereSSO extends DaemonDetector {

    private static final Log log = LogFactory.getLog(DiscoveryVSphereSSO.class);

    public static final String KEY_APPLICATION_NAME = "application.name";
    public static final String TYPE_SSO = "SSO";
    public static final String TYPE_VRA_VSPHERE_SSO = "vSphere SSO";

    @Override
    public List<ServerResource> getServerResources(ConfigResponse platformConfig)
        throws PluginException {
        log.debug("[getServerResources] platformConfig=" + platformConfig);
        List<ServerResource> servers = super.getServerResources(platformConfig);

        for (ServerResource server : servers) {
            String ssoName = server.getName();
            String ssoType = server.getType();
            log.debug("[getServerResources] sso server=" + ssoName + " sso Type=" + ssoType);

            String model = marshallCommonModel(getCommonModel(ssoName, ssoType));
            server.getProductConfig().setValue("extended-relationship-model",
                        new String(Base64.encodeBase64(model.getBytes())));

            ConfigResponse configResponse = new ConfigResponse();
            configResponse.setValue("extended-relationship-model", new String(Base64.encodeBase64(model.getBytes())));

            // do not remove, why? please don't ask.
            server.setProductConfig(server.getProductConfig());
            server.setCustomProperties(configResponse);
        }
        return servers;
    }

    public String marshallCommonModel(Resource resource) {
        ObjectFactory factory = new ObjectFactory();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        factory.saveModel(resource, baos);
        log.debug("[testMarshallAsJson] fos=" + baos.toString());
        return baos.toString();
    }

    private Resource getCommonModel(String ssoName,
                                    String ssoType) {
        ObjectFactory factory = new ObjectFactory();
        Resource ssoServer = factory.createResource(false, ssoType, ssoName, ResourceTier.SERVER);
        Resource vraSsoServerGroup =
                    factory.createResource(true, TYPE_VRA_VSPHERE_SSO,
                                getParameterName(KEY_APPLICATION_NAME, TYPE_VRA_VSPHERE_SSO),
                                ResourceTier.LOGICAL,
                                ResourceSubType.TAG);
        Resource ssoServerGroup =
                    factory.createResource(true, TYPE_SSO, getParameterName(KEY_APPLICATION_NAME, TYPE_SSO),
                                ResourceTier.LOGICAL,
                                ResourceSubType.TAG);

        ssoServer.addRelations(factory.createRelation(vraSsoServerGroup, RelationType.PARENT));
        vraSsoServerGroup.addRelations(factory.createRelation(ssoServerGroup, RelationType.PARENT));

        return ssoServer;
    }

    /**
     * @param name
     * @param type
     * @return
     */
    private String getParameterName(String name,
                                       String type) {
        return String.format("${%s} %s", name, type);
    }
}
