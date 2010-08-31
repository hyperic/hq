/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.product;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AIServiceTypeValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;

/**
 * Representation of a service type, equivalent to the data specified by the
 * <service> XML tag
 * 
 * @author jhickey
 * 
 */
public class ServiceType {
	private Set controlActions = new HashSet();

	private ConfigSchema customProperties = new ConfigSchema();

	private final ServiceTypeInfo info;

	private Log log = LogFactory.getLog(ServiceType.class.getName());

	private MeasurementInfos measurements = new MeasurementInfos();

	private ConfigResponse pluginClasses = new ConfigResponse();

	private final String productName;

	private ConfigResponse properties = new ConfigResponse();

	private final AIServiceTypeValue resource;

	private final String serviceName;

	/**
	 * 
	 * @param serviceName
	 *            The unique service type name (unique with respect to server
	 *            type)
	 * @param productName
	 *            The name of the product containing this service
	 * @param info
	 *            The {@link ServiceTypeInfo} describing this service type
	 */
	public ServiceType(String serviceName, String productName,
			ServiceTypeInfo info) {
		this.info = info;
		this.productName = productName;
		this.serviceName = serviceName;
		this.resource = new AIServiceTypeValue();
		this.resource.setDescription(info.getDescription());
		this.resource.setName(info.getName());
		this.resource.setServiceName(serviceName);
		this.resource.setProductName(productName);
	}

	/**
	 * Adds a control action to the collection of control actions
	 * 
	 * @param controlAction
	 *            The name of the control action to add
	 */
	public void addControlAction(String controlAction) {
		this.controlActions.add(controlAction);
		resource.setControlActions((String[]) controlActions
				.toArray(new String[controlActions.size()]));
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj.getClass().equals(this.getClass()))) {
			return false;
		}
		return getInfo().getName().equals(
				((ServiceType) obj).getInfo().getName());
	}

	/**
	 * 
	 * @return The {@link AIServiceTypeValue} representing this ServiceType
	 */
	public AIServiceTypeValue getAIServiceTypeValue() {
		return this.resource;
	}

	/**
	 * 
	 * @return The set of control action names for this {@link ServiceType}
	 */
	public Set getControlActions() {
		return controlActions;
	}

	/**
	 * 
	 * @return The custom properties for this {@link ServiceType}
	 */
	public ConfigSchema getCustomProperties() {
		return customProperties;
	}

	/**
	 * 
	 * @return The {@link ServiceTypeInfo} describing this service type
	 */
	public ServiceTypeInfo getInfo() {
		return info;
	}

	/**
	 * 
	 * @return The measurements for this service type
	 */
	public MeasurementInfos getMeasurements() {
		return measurements;
	}

	/**
	 * 
	 * @return The plugins for this service type. Key of properties map is
	 *         plugin type (i.e. "control"), value is fully qualified class name
	 *         of plugin
	 */
	public ConfigResponse getPluginClasses() {
		return pluginClasses;
	}

	/**
	 * 
	 * @return The name of the product containing this service
	 */
	public String getProductName() {
		return productName;
	}

	/**
	 * 
	 * @return The properties of this service type (the ones used internally).
	 *         This is equivalent to the direct <property> tag in the service
	 *         XML (as opposed to the <properties> tag which defines custom
	 *         properties).
	 */
	public ConfigResponse getProperties() {
		return properties;
	}

	/**
	 * 
	 * @return The unique service type name (unique with respect to server type)
	 */
	public String getServiceName() {
		return serviceName;
	}

	public int hashCode() {
		return getInfo().getName().hashCode();
	}

	/**
	 * 
	 * @param controlActions
	 *            The set of control action names for this {@link ServiceType}
	 */
	public void setControlActions(Set controlActions) {
		this.controlActions = controlActions;
		resource.setControlActions((String[]) controlActions
				.toArray(new String[controlActions.size()]));
	}

	/**
	 * 
	 * @param customProperties
	 *            The custom properties for this {@link ServiceType}
	 */
	public void setCustomProperties(ConfigSchema customProperties) {
		this.customProperties = customProperties;
		try {
			resource.setCustomProperties(customProperties.encode());
		} catch (EncodingException e) {
			log
					.warn("Unable to set custom properties on internal resource object.  Cause: "
							+ e.getMessage());
		}
	}

	/**
	 * 
	 * @param measurements
	 *            The measurements for this service type
	 */
	public void setMeasurements(MeasurementInfos measurements) {
		this.measurements = measurements;
		try {
			resource.setMeasurements(measurements.encode());
		} catch (EncodingException e) {
			log
					.warn("Unable to set measurements on internal resource object.  Cause: "
							+ e.getMessage());
		}
	}

	/**
	 * 
	 * @param pluginClasses
	 *            The plugins for this service type. Key of properties map is
	 *            plugin type (i.e. "control"), value is fully qualified class
	 *            name of plugin
	 */
	public void setPluginClasses(ConfigResponse pluginClasses) {
		this.pluginClasses = pluginClasses;
		try {
			resource.setPluginClasses(pluginClasses.encode());
		} catch (EncodingException e) {
			log
					.warn("Unable to set plugin classes on internal resource object.  Cause: "
							+ e.getMessage());
		}
	}

	/**
	 * 
	 * @param properties
	 *            The properties of this service type (the ones used
	 *            internally). This is equivalent to the direct <property> tag
	 *            in the service XML (as opposed to the <properties> tag which
	 *            defines custom properties).
	 */
	public void setProperties(ConfigResponse properties) {
		this.properties = properties;
		try {
			resource.setProperties(properties.encode());
		} catch (EncodingException e) {
			log
					.warn("Unable to set properties on internal resource object.  Cause: "
							+ e.getMessage());
		}
	}

	public String toString() {
		final StringBuilder serviceType = new StringBuilder("ServiceType[name=");
		serviceType.append(getInfo().getName());
		serviceType.append(']');
		return serviceType.toString();
	}
}
