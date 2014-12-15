package org.hyperic.plugin.vrealize.automation;

import java.util.List;
import java.util.Properties;

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
* @author Sharon Rozinsky
*/
public class DiscoveryITBMServer extends DaemonDetector {
	private static final String CSP_COMPONENT_REGISTRY_URL = "csp.component.registry.url";
	private static final String SECURITY_PROPERTIES_FILE_PATH = "/usr/local/tcserver/vfabric-tc-server-standard/itbm-server/conf/security.properties";	
	private static final Log log = LogFactory.getLog(DiscoveryITBMServer.class);

	@Override
	public List getServerResources(ConfigResponse platformConfig)
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
		final String ITBM_GROUP_TYPE = "ITBM Group";
		final String APPLICATION_TYPE = "vRealize Automation Application";
		String groupString = String.format("%s %s", applicationName,
				ITBM_GROUP_TYPE);
		String applicationString = String.format("%s %s", applicationName,
				APPLICATION_TYPE);
		ObjectFactory objectFactory = new ObjectFactory();

		Resource itbmServer = objectFactory.createResource(false,
				server.getType(), server.getName(), ResourceTier.SERVER);
		Resource itbmGroup = objectFactory.createResource(true,
				ITBM_GROUP_TYPE, groupString, ResourceTier.LOGICAL,
				ResourceSubType.TAG);
		Resource application = objectFactory.createResource(true,
				APPLICATION_TYPE, applicationString, ResourceTier.LOGICAL,
				ResourceSubType.TAG);

		itbmServer.addRelations(objectFactory.createRelation(itbmGroup,
				RelationType.PARENT));
		itbmGroup.addRelations(objectFactory.createRelation(application,
				RelationType.PARENT));

		return itbmServer;
	}

}
