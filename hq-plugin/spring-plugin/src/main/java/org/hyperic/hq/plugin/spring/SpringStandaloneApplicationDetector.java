package org.hyperic.hq.plugin.spring;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.jmx.MxServerDetector;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

/**
 * Extension of MxServerDetector that discovers Standalone Spring Applications by looking for
 * -Dspring.managed.application.name. Default behavior is overriden here so that server instances can include the value
 * of "spring.managed.application.name" in the Server name (to uniquely identify 2 different applications running on the
 * same machine)
 * @author Jennifer Hickey
 * 
 */
public class SpringStandaloneApplicationDetector extends MxServerDetector {

	public List getServerResources(ConfigResponse platformConfig) throws PluginException {
		List servers = new ArrayList();
		List procs = getServerProcessList();

		for (int i = 0; i < procs.size(); i++) {
			MxProcess process = (MxProcess) procs.get(i);
			String dir = process.getInstallPath();

			if (!isInstallTypeVersion(dir)) {
				continue;
			}

			ConfigResponse config = new ConfigResponse();
			ConfigSchema schema = getConfigSchema(getTypeInfo().getName(), ProductPlugin.CFGTYPE_IDX_PRODUCT);

			if (schema != null) {
				ConfigOption option = schema.getOption(PROP_PROCESS_QUERY);

				if (option != null) {
					// Configure process.query
					String query = PROC_JAVA + ",Args.*.eq=-D" + getProcHomeProperty() + "=" + dir;
					config.setValue(option.getName(), query);
				}
			}

			if (process.getURL() != null) {
				config.setValue(MxUtil.PROP_JMX_URL, process.getURL());
			} else {
				String[] args = process.getArgs();
				for (int j = 0; j < args.length; j++) {
					if (configureMxURL(config, args[j])) {
						break;
					}
				}
			}

			// Create the server resource
			// <platform name> <application name> Spring Application
			final String fqn = platformConfig.getValue("platform.name") + " " + dir + " Spring Application";
			ServerResource server = new ServerResource();
			server.setType(getTypeInfo().getName());
			server.setInstallPath(dir);
			server.setName(fqn);
			server.setIdentifier(fqn);
			// default anything not auto-configured
			setProductConfig(server, config);
			server.setMeasurementConfig();
			servers.add(server);
		}
		return servers;
	}

}
