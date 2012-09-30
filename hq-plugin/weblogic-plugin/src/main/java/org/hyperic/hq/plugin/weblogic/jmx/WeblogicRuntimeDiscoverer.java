/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.plugin.weblogic.jmx;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.remote.JMXConnector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServiceTypeValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.plugin.weblogic.WeblogicAuth;
import org.hyperic.hq.plugin.weblogic.WeblogicDetector;
import org.hyperic.hq.plugin.weblogic.WeblogicMetric;
import org.hyperic.hq.plugin.weblogic.WeblogicProductPlugin;
import org.hyperic.hq.plugin.weblogic.WeblogicUtil;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginUpdater;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.RuntimeDiscoverer;
import org.hyperic.hq.product.RuntimeResourceReport;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceType;
import org.hyperic.hq.product.jmx.MBeanUtil;
import org.hyperic.hq.product.jmx.ServiceTypeFactory;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

public class WeblogicRuntimeDiscoverer implements RuntimeDiscoverer, PrivilegedAction {

	private static final Double DYNAMIC_SVC_SUPPORTED_WEBLOGIC_VERSION = new Double(9.1d);
	
	

	private static final boolean useJAAS = WeblogicProductPlugin.useJAAS();
    private static final String PROP_FQDN = "weblogic.discover.fqdn";
	private static Log log = LogFactory.getLog(WeblogicRuntimeDiscoverer.class);

	private int serverId;
    private Properties props;

	private AIPlatformValue aiplatform;

	private ConfigResponse config;

	private String targetFqdn = null;

	private WeblogicDetector plugin;

	private String version;
	
	private ServiceTypeFactory serviceTypeFactory = new ServiceTypeFactory();
	
	private PluginUpdater pluginUpdater = new PluginUpdater();

	public WeblogicRuntimeDiscoverer(WeblogicDetector plugin) {
		this.plugin = plugin;
		this.version = plugin.getTypeInfo().getVersion();

	     props = plugin.getManager().getProperties();

		// this property can be used to host foreign nodes on
		// another platform. if set to "same" will use the same fqdn
		// as the admin server.
		this.targetFqdn = props.getProperty(PROP_FQDN);

	}

	public WeblogicDetector getPlugin() {
		return this.plugin;
	}

	public String getPluginVersion() {
		return this.version;
	}

	public RuntimeResourceReport discoverResources(int serverId, AIPlatformValue aiplatform, ConfigResponse config)
			throws PluginException {

		if (useJAAS) {
			return discoverAs(serverId, aiplatform, config);
		}
		else {
			try {
				return discover(serverId, aiplatform, config);
			}
			catch (SecurityException e) {
				String msg = "SecurityException: " + e.getMessage();
				throw new PluginException(msg, e);
			}
		}
	}

	private RuntimeResourceReport discoverAs(int serverId, AIPlatformValue aiplatform, ConfigResponse config)
			throws PluginException {

		Object obj;
		WeblogicAuth auth = WeblogicAuth.getInstance(config.toProperties());

		this.serverId = serverId;
		this.aiplatform = aiplatform;
		this.config = config;

		try {
			obj = auth.runAs(this);
		}
		catch (SecurityException e) {
			throw new PluginException(e.getMessage(), e);
		}

		if (obj instanceof RuntimeResourceReport) {
			return (RuntimeResourceReport) obj;
		}

		if (obj instanceof PluginException) {
			throw (PluginException) obj;
		}

		if (obj instanceof Exception) {
			Exception e = (Exception) obj;
			throw new PluginException(e.getMessage(), e);
		}

		throw new IllegalArgumentException();
	}

	public Object run() {
		try {
			return discover(this.serverId, this.aiplatform, this.config);
		}
		catch (Exception e) {
			return e;
		}
	}

	private RuntimeResourceReport discover(int serverId, AIPlatformValue aiplatform, ConfigResponse config)
			throws PluginException {

		String installpath = config.getValue(ProductPlugin.PROP_INSTALLPATH);
		// incase jaas is disabled; generatePlatform may use this.
		this.aiplatform = aiplatform;

		log.debug("discover using: " + config);

		String domainName = config.getValue(WeblogicMetric.PROP_DOMAIN);
		String serverName = config.getValue(WeblogicMetric.PROP_SERVER);

		RuntimeResourceReport rrr = new RuntimeResourceReport(serverId);

		Properties props = config.toProperties();

		WeblogicDiscover discover = new WeblogicDiscover(this.version,props);

		try {
			MBeanServer mServer = discover.getMBeanServer();

			discover.init(mServer);

			NodeManagerQuery nodemgrQuery = new NodeManagerQuery();
			ServerQuery serverQuery = new ServerQuery();
			serverQuery.setDiscover(discover);

			ArrayList servers = new ArrayList();

			discover.find(mServer, serverQuery, servers);

			WeblogicQuery[] serviceQueries = discover.getServiceQueries();

			// ensure admin is first incase we need version
			serverQuery.sort(servers);

			String adminVersion = null;

			for (int i = 0; i < servers.size(); i++) {
				serverQuery = (ServerQuery) servers.get(i);
                if(!serverQuery.isRunning()) continue;

				if (serverQuery.isAdmin()) {
					adminVersion = serverQuery.getVersion();
				}
				else if (serverQuery.getVersion() == null) {
					// will be the case if a server was create but never started
					// safely assume it was created by the admin server in this
					// case and hence is the same version.
					serverQuery.setVersion(adminVersion);
				}

				AIPlatformValue aPlatform;
				AIServerExtValue aServer = generateServer(serverQuery);

				// Set the ID, so when this report is processed at the server,
				// there is no mistaking this server in the report for any other
				// server
				// in appdef (or worse, not matching anything and adding a new
				// server).
				if (serverQuery.getName().equals(serverName)
						&& serverQuery.getDiscover().getDomain().equals(domainName)) {
					aServer.setId(new Integer(serverId));
					aServer.setPlaceholder(true);
					// maintain existing installpath,
					// MBeanServer CurrentDirectory might be different
					if (installpath != null) {
						aServer.setInstallPath(installpath);
					}
					aiplatform.addAIServerValue(aServer);
					rrr.addAIPlatform(aiplatform);
				}
				else {
					aPlatform = generatePlatform(serverQuery);
					aPlatform.addAIServerValue(aServer);
					rrr.addAIPlatform(aPlatform);
				}

				if (!serverQuery.isRunning()) {
					continue;
				}

				mServer = discover.getMBeanServer(serverQuery.getUrl());

				ArrayList aServices = new ArrayList();
				ArrayList services = new ArrayList();

				for (int j = 0; j < serviceQueries.length; j++) {
					WeblogicQuery serviceQuery = serviceQueries[j];

					serviceQuery.setParent(serverQuery);
					serviceQuery.setVersion(serverQuery.getVersion());

					discover.find(mServer, serviceQuery, services);
				}

				// Dynamic services can only be discovered on WebLogic 9.1 or
				// higher
				if (DYNAMIC_SVC_SUPPORTED_WEBLOGIC_VERSION.compareTo(Double.valueOf(serverQuery.getVersion())) <= 0) {
					final JMXConnector jmxConnector = WeblogicUtil.getManagedServerConnection(props);
					Set serviceTypes = new HashSet();
					try {
						discoverDynamicServices(discover, jmxConnector.getMBeanServerConnection(), serverQuery, services, serviceTypes);
					}
					catch (IOException e) {
						throw new PluginException("Error discovering dynamic services", e);
					}
					finally {
						try {
							jmxConnector.close();
						}
						catch (IOException e) {
						}
					}
					final AIServiceTypeValue[] serviceTypeValues = new AIServiceTypeValue[serviceTypes.size()];
					int count = 0;
					for(Iterator iterator = serviceTypes.iterator();iterator.hasNext();) {
						serviceTypeValues[count] = ((ServiceType)iterator.next()).getAIServiceTypeValue();
						count++;
					}
					aServer.setAiServiceTypes(serviceTypeValues);
					pluginUpdater.updateServiceTypes(plugin.getProductPlugin(), serviceTypes);
				}

                for (int k = 0; k < services.size(); k++) {
                    boolean valid = true;
                    ServiceQuery service = (ServiceQuery) services.get(k);
                    if (service instanceof ApplicationQuery) {
                        valid = ((ApplicationQuery) service).isEAR();
                    }
                    if (valid) {
                        aServices.add(generateService(service));

                    } else {
                        log.debug("skipped service:"+service.getName());
                    }
                }

				AIServiceValue[] aiservices = (AIServiceValue[]) aServices.toArray(new AIServiceValue[0]);
				aServer.setAIServiceValues(aiservices);

				if (serverQuery.isAdmin()) {
					ArrayList mgrs = new ArrayList();
					nodemgrQuery.setAdminServer(serverQuery);
					discover.find(mServer, nodemgrQuery, mgrs);

					for (int n = 0; n < mgrs.size(); n++) {
						nodemgrQuery = (NodeManagerQuery) mgrs.get(n);
						aServer = generateServer(nodemgrQuery);
						aPlatform = generatePlatform(nodemgrQuery);
						aPlatform.addAIServerValue(aServer);
						rrr.addAIPlatform(aPlatform);
					}
				}
			}
		}
		catch (WeblogicDiscoverException e) {
			throw new PluginException(e.getMessage(), e);
		}

		return rrr;
	}

	private void discoverDynamicServices(WeblogicDiscover discover, MBeanServerConnection mServer, ServerQuery parent,
			ArrayList services, Set serviceTypes) throws PluginException, WeblogicDiscoverException {
		try {
			final Set objectNames = mServer.queryNames(new ObjectName(MBeanUtil.DYNAMIC_SERVICE_DOMAIN + ":*"), null);
			//Only WebLogic Admin servers have auto-inventory plugins - have to construct a ServerInfo for the WebLogic server
			String[] platformTypes = ((ServerTypeInfo)plugin.getTypeInfo()).getPlatformTypes();
			 ServerTypeInfo server =
	                new ServerTypeInfo(parent.getResourceType(), parent.getDescription(), parent.getVersion());
	        server.setValidPlatformTypes(platformTypes);
			for (Iterator iterator = objectNames.iterator(); iterator.hasNext();) {
				final ObjectName objectName = (ObjectName) iterator.next();
				final MBeanInfo serviceInfo = mServer.getMBeanInfo(objectName);
				if (serviceInfo instanceof ModelMBeanInfo) {
					ServiceType identityType = serviceTypeFactory.getServiceType(plugin.getProductPlugin().getName(),server,
							(ModelMBeanInfo) serviceInfo, objectName);
					//identityType could be null if MBean is not to be exported
					if(identityType != null) {
						ServiceType serviceType;
						if (!serviceTypes.contains(identityType)) {
							serviceType = serviceTypeFactory.create(plugin.getProductPlugin(),
									server, (ModelMBeanInfo) serviceInfo,
									objectName);
							if (serviceType != null) {
								serviceTypes.add(serviceType);
							}
						}else {
							serviceType = findServiceType(identityType.getInfo().getName(),serviceTypes);
						}
						final String shortServiceType = identityType.getServiceName();
						DynamicServiceQuery dynamicServiceQuery = new DynamicServiceQuery();
						dynamicServiceQuery.setParent(parent);
						dynamicServiceQuery.setType(shortServiceType);
						dynamicServiceQuery.setAttributeNames(serviceType.getCustomProperties().getOptionNames());
						dynamicServiceQuery.setName(objectName.getKeyProperty("name"));
						dynamicServiceQuery.getDynamicAttributes(mServer, objectName);
						services.add(dynamicServiceQuery);
					}
				}
			}
		} catch (Exception e) {
			 throw new PluginException(e.getMessage(), e);
		} 	
	}
	
	private ServiceType findServiceType(String serviceName, Set serviceTypes) {
		for(Iterator iterator = serviceTypes.iterator();iterator.hasNext();) {
			ServiceType serviceType = (ServiceType)iterator.next();
			if(serviceType.getInfo().getName().equals(serviceName)) {
				return serviceType;
			}
		}
		return null;
	}

	private AIPlatformValue generatePlatform(BaseServerQuery server) {

		AIPlatformValue aiplatform = new AIPlatformValue();

		String serverFqdn = server.getFqdn();
        serverFqdn = //support mapping via agent.properties
            this.props.getProperty(PROP_FQDN + "." + serverFqdn,
                                   serverFqdn);
		String fqdn = serverFqdn;

		// let it be known if the platform does not exist in cam
		// since ai will ka-boom with huge "No such entity!" stacktrace.
		// XXX ai should handle this condition better
		if (!fqdn.equals(this.aiplatform.getFqdn())) {
			log.info("Discovered server (" + server.getResourceFullName() + ") hosted on another platform (fqdn="
					+ fqdn + ")");
		}

		if (this.targetFqdn != null) {
			if (this.targetFqdn.equals("same")) {
				fqdn = this.aiplatform.getFqdn();
			}
			else {
				fqdn = this.targetFqdn;
			}
		}
		else {
			// out-of-the-box weblogic w/o changing ListenAddress
			if (serverFqdn.equals("localhost") || serverFqdn.equals("127.0.0.1")) {
				fqdn = this.aiplatform.getFqdn();
			}
			else {
				fqdn = serverFqdn;
			}
		}

		if (!fqdn.equals(serverFqdn)) {
			log.info("changing fqdn for " + server.getName() + ": " + serverFqdn + " => " + fqdn);
		}

		aiplatform.setFqdn(fqdn);

		return aiplatform;
	}

	private AIServerExtValue generateServer(BaseServerQuery server) throws PluginException {

		AIServerExtValue aiserver = new AIServerExtValue();

		aiserver.setInstallPath(server.getInstallPath());
		aiserver.setAutoinventoryIdentifier(server.getIdentifier());

		aiserver.setServicesAutomanaged(true);
		aiserver.setServerTypeName(server.getResourceName());
		String name = server.getResourceFullName();
		if (WeblogicProductPlugin.usePlatformName) {
			name = GenericPlugin.getPlatformName() + " " + name;
		}
		aiserver.setName(name);
		String notes = server.getDescription();
		if (notes != null) {
			aiserver.setDescription(notes);
		}
		if (!server.isAdmin()) {
			ConfigResponse productConfig = new ConfigResponse(server.getResourceConfig());
			String listeningPorts = server.getAttribute(BaseServerQuery.ATTR_LISTEN_PORT);
			if (listeningPorts!=null && !listeningPorts.equals("")) {
			    productConfig.setValue(Collector.LISTEN_PORTS, listeningPorts);
			}
			ConfigResponse metricConfig = new ConfigResponse();
			ConfigResponse cprops = new ConfigResponse(server.getCustomProperties());

			try {
				if (server.hasControl() && !server.isServer61()) {
					ConfigResponse controlConfig = new ConfigResponse(server.getControlConfig());
					aiserver.setControlConfig(controlConfig.encode());
				}

				aiserver.setProductConfig(productConfig.encode());
				aiserver.setMeasurementConfig(metricConfig.encode());
				aiserver.setCustomProperties(cprops.encode());
			}
			catch (EncodingException e) {
				throw new PluginException("Error generating config", e);
			}
		}

		log.debug("discovered server: " + aiserver.getName());

		return aiserver;
	}

	private AIServiceValue generateService(ServiceQuery service) throws PluginException {
		AIServiceValue aiservice = new AIServiceValue();

		ConfigResponse productConfig = new ConfigResponse(service.getResourceConfig());
		ConfigResponse metricConfig = new ConfigResponse();
		ConfigResponse cprops = new ConfigResponse(service.getCustomProperties());

		String notes = service.getDescription();
		if (notes != null) {
			aiservice.setDescription(notes);
		}

		aiservice.setServiceTypeName(service.getResourceName());

		String name = service.getResourceFullName();
		if (WeblogicProductPlugin.usePlatformName) {
			name = GenericPlugin.getPlatformName() + " " + name;
		}
		if (name.length() >= 200) {
			// make sure we dont exceed service name limit
			name = name.substring(0, 199);
		}
		aiservice.setName(name);

		try {
			if (service.hasControl() && !service.isServer61()) {
				ConfigResponse controlConfig = new ConfigResponse(service.getControlConfig());
				aiservice.setControlConfig(controlConfig.encode());
			}

			aiservice.setProductConfig(productConfig.encode());
			aiservice.setMeasurementConfig(metricConfig.encode());
			aiservice.setCustomProperties(cprops.encode());

			if (service.hasResponseTime()) {
				ConfigResponse rtConfig = new ConfigResponse(service.getResponseTimeConfig());
				aiservice.setResponseTimeConfig(rtConfig.encode());
			}
		}
		catch (EncodingException e) {
			throw new PluginException("Error generating config", e);
		}

		log.debug("discovered service: " + aiservice.getName());

		return aiservice;
	}
}
