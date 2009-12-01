package org.hyperic.hq.plugin.weblogic;

import javax.management.AttributeNotFoundException;
import javax.management.ObjectName;

import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

/**
 * MeasurementPlugin that obtains metric values via a JMX getAttribute call
 * using the WebLogic managed server's RemoteMBeanServer, obtained through JNDI
 * lookup
 * @author Jennifer Hickey
 * 
 */
public class WeblogicServiceMeasurementPlugin extends MeasurementPlugin {

	private double doubleValue(Object obj) throws PluginException {
		try {
			return Double.valueOf(obj.toString()).doubleValue();
		}
		catch (NumberFormatException e) {
			throw new PluginException("Cannot convert '" + obj + "' to double");
		}
	}

	public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException,
			MetricUnreachableException {
		double doubleVal;
		String objectName = metric.getObjectName();
		String attribute = metric.getAttributeName();
		Object objectVal = null;
		try {
			ObjectName objName = new ObjectName(objectName);
			objectVal = WeblogicUtil.getManagedServerConnection(metric).getMBeanServerConnection().getAttribute(
					objName, attribute);
		}
		catch (AttributeNotFoundException e) {
			// XXX not all MBeans have a reasonable attribute to
			// determine availability, so just assume if we get this far
			// the MBean exists and is alive.
			if (metric.isAvail()) {
				objectVal = new Double(Metric.AVAIL_UP);
			}
			else {
				throw new PluginException(e);
			}
		}
		catch (RuntimeException e) {
			// Temporary fix until availability strings can be mapped
			// in hq-plugin.xml. Resin wraps AttributeNotFoundException
			if (metric.isAvail()) {
				Throwable cause = e.getCause();
				while (cause != null) {
					if (cause instanceof AttributeNotFoundException) {
						objectVal = new Double(Metric.AVAIL_UP);
						break;
					}
					cause = cause.getCause();
				}
				if (objectVal == null) {
					objectVal = new Double(Metric.AVAIL_DOWN);
				}
			}
			else {
				throw e;
			}
		}
		catch (Exception e) {
			if (metric.isAvail()) {
				objectVal = new Double(Metric.AVAIL_DOWN);
			}
			else {
				throw new PluginException("Unable to obtain the value of metric: " + metric.getObjectName()
						+ ".  Cause: " + e.getMessage());
			}
		}

		String stringVal = objectVal.toString();

		// check for value mappings in plugin.xml:
		// <property name"StateVal.Stopped" value="0.0"/>
		// <property name="StateVal.Started" value="1.0"/>
		// <property name"State.3" value="1.0"/>
		String mappedVal = getTypeProperty(metric.getAttributeName() + "." + stringVal);

		if (mappedVal != null) {
			doubleVal = doubleValue(mappedVal);
		}
		else if (objectVal instanceof Number) {
			doubleVal = ((Number) objectVal).doubleValue();
		}
		else if (objectVal instanceof Boolean) {
			doubleVal = ((Boolean) objectVal).booleanValue() ? Metric.AVAIL_UP : Metric.AVAIL_DOWN;
		}
		else {
			doubleVal = doubleValue(stringVal);
		}

		if (doubleVal == -1) {
			return new MetricValue(Double.NaN);
		}

		return new MetricValue(doubleVal);
	}

	public String translate(String template, ConfigResponse config) {
		return super.translate(template, config);
	}
}
