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

package org.hyperic.hq.appdef.shared;

import java.io.Serializable;

/**
 * Value object for service type
 * 
 * @author jhickey
 * 
 */
public class AIServiceTypeValue implements Serializable {

	private static final long serialVersionUID = -8146629460134699907L;

	private String[] controlActions;

	private byte[] customProperties;

	private String description;

	private byte[] measurements;

	private String name;

	private byte[] pluginClasses;

	private String productName;

	private byte[] properties;

	private int serverId;

	private String serviceName;

	/**
	 * 
	 * @return The set of control action names
	 */
	public String[] getControlActions() {
		return controlActions;
	}

	/**
	 * 
	 * @return The custom properties
	 */
	public byte[] getCustomProperties() {
		return customProperties;
	}

	/**
	 * 
	 * @return The service type description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * 
	 * @return The measurements for this service type
	 */
	public byte[] getMeasurements() {
		return measurements;
	}

	/**
	 * 
	 * @return The name of this service type
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @return The plugins for this service type.
	 */
	public byte[] getPluginClasses() {
		return pluginClasses;
	}

	/**
	 * 
	 * @return The name of the product containing this service type
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
	public byte[] getProperties() {
		return properties;
	}

	/**
	 * 
	 * @return The ID of the server this service type belongs to
	 */
	public int getServerId() {
		return serverId;
	}

	/**
	 * 
	 * @return The unique service type name (unique with respect to server type)
	 */
	public String getServiceName() {
		return serviceName;
	}

	/**
	 * 
	 * @param controlActions
	 *            The set of control action names
	 */
	public void setControlActions(String[] controlActions) {
		this.controlActions = controlActions;
	}

	/**
	 * 
	 * @param customProperties
	 *            The custom properties
	 */
	public void setCustomProperties(byte[] customProperties) {
		this.customProperties = customProperties;
	}

	/**
	 * 
	 * @param description
	 *            The service type description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * 
	 * @param measurements
	 *            The measurements for this service type
	 */
	public void setMeasurements(byte[] measurements) {
		this.measurements = measurements;
	}

	/**
	 * 
	 * @param name
	 *            The name of this service type
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @param pluginClasses
	 *            The plugins for this service type.
	 */
	public void setPluginClasses(byte[] pluginClasses) {
		this.pluginClasses = pluginClasses;
	}

	/**
	 * 
	 * @param productName
	 *            The name of the product containing this service type
	 */
	public void setProductName(String productName) {
		this.productName = productName;
	}

	/**
	 * 
	 * @param properties
	 *            The properties of this service type (the ones used
	 *            internally). This is equivalent to the direct <property> tag
	 *            in the service XML (as opposed to the <properties> tag which
	 *            defines custom properties).
	 */
	public void setProperties(byte[] properties) {
		this.properties = properties;
	}

	/**
	 * 
	 * @param serverId
	 *            The ID of the server this service type belongs to
	 */
	public void setServerId(int serverId) {
		this.serverId = serverId;
	}

	/**
	 * 
	 * @param serviceName
	 *            The unique service type name (unique with respect to server
	 *            type)
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String toString() {
		StringBuffer str = new StringBuffer("AIServiceTypeValue[");
		str.append("name=");
		str.append(getName());
		str.append(",serverId=");
		str.append(getServerId());
		str.append(']');
		return (str.toString());
	}
}
