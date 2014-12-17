package org.hyperic.plugin.vrealize.automation;

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
import static org.hyperic.plugin.vrealize.automation.VRAUtils.getFullResourceName;
import static org.hyperic.plugin.vrealize.automation.VraConstants.KEY_APPLICATION_NAME_VAR;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VCO_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_APPLICATION;

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
            String appName = server.getName();
            String appType = server.getType();
            log.debug("[getServerResources] vCO server=" + appName + " vCO Type=" + appType);
            
            Resource modelResource = getCommonModel(appName, appType);
            String modelXml = VRAUtils.marshallResource(modelResource);
            VRAUtils.setModelProperty(server, modelXml);
        }
        return servers;
    }
    
    private static Resource getCommonModel(String ssoName, String ssoType) {
        ObjectFactory factory = new ObjectFactory();
        Resource server = factory.createResource(false, ssoType, ssoName, ResourceTier.SERVER);
        
        Resource serverGroup
                = factory.createResource(true, TYPE_VCO_TAG, getFullResourceName(KEY_APPLICATION_NAME_VAR, TYPE_VCO_TAG),
                        ResourceTier.LOGICAL,
                        ResourceSubType.TAG);
        
        Resource app = factory.createResource(false, TYPE_VRA_APPLICATION, getFullResourceName(KEY_APPLICATION_NAME_VAR, TYPE_VRA_APPLICATION), ResourceTier.SERVER);
        
        server.addRelations(factory.createRelation(serverGroup, RelationType.PARENT));
        serverGroup.addRelations(factory.createRelation(app, RelationType.PARENT));
        
        return server;
    }
}
