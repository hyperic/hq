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

package org.hyperic.hq.product.jmx;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.Descriptor;
import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;

import junit.framework.TestCase;

import org.hyperic.hq.autoinventory.AICompare;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.MeasurementInfos;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceType;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.product.pluginxml.PluginData;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.StringConfigOption;

/**
 * Unit test of {@link ServiceTypeFactory}
 * @author jhickey
 * 
 */
public class ServiceTypeFactoryTest
    extends TestCase
{

    private class TestProductPlugin
        extends ProductPlugin
    {
        private String name;

        /**
         * 
         * @param name The name of the product plugin
         */
        public TestProductPlugin(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

    }

    private MBeanServer mBeanServer = MBeanServerFactory.createMBeanServer();

    private ObjectName objectName;

    private ProductPlugin productPlugin;

    private ServerTypeInfo serverTypeInfo;

    private ServiceTypeFactory serviceTypeFactory;

    private ModelMBeanAttributeInfo[] createAttributeInfos(String attributeName,
                                                           String metricAliasName,
                                                           String metricName,
                                                           String units,
                                                           String metricType,
                                                           String metricCategory)
    {
        ModelMBeanAttributeInfo attribute = new ModelMBeanAttributeInfo(attributeName,
                                                                        "java.lang.Boolean",
                                                                        "Sets monitoring enabled",
                                                                        true,
                                                                        true,
                                                                        false);
        ModelMBeanAttributeInfo metric = new ModelMBeanAttributeInfo(metricAliasName,
                                                                     "java.lang.Double",
                                                                     "",
                                                                     true,
                                                                     false,
                                                                     false);
        final Descriptor metricDescriptor = metric.getDescriptor();
        metricDescriptor.setField("displayName", metricName);
        metricDescriptor.setField("units", units);
        metricDescriptor.setField("metricType", metricType);
        metricDescriptor.setField("metricCategory", metricCategory);
        metric.setDescriptor(metricDescriptor);
        return new ModelMBeanAttributeInfo[] { attribute, metric };
    }

    private ModelMBeanAttributeInfo[] createAttributeInfos(String attributeName,
                                                           String metricAliasName,
                                                           String metricName,
                                                           String units,
                                                           String metricType,
                                                           String metricCategory,
                                                           boolean indicator, String collectionInterval, String defaultOn, String rate)
    {
        ModelMBeanAttributeInfo[] attributeInfos = createAttributeInfos(attributeName,
                                                                        metricAliasName,
                                                                        metricName,
                                                                        units,
                                                                        metricType,
                                                                        metricCategory);
        final Descriptor attributeDescriptor = attributeInfos[0].getDescriptor();
        attributeDescriptor.setField("attributeType", "Attribute");
        attributeInfos[0].setDescriptor(attributeDescriptor);

        final Descriptor metricDescriptor = attributeInfos[1].getDescriptor();
        metricDescriptor.setField("attributeType", "Metric");
        metricDescriptor.setField("displayName", metricName);
        metricDescriptor.setField("units", units);
        metricDescriptor.setField("indicator", Boolean.toString(indicator));
        metricDescriptor.setField("metricType", metricType);
        metricDescriptor.setField("metricCategory", metricCategory);
        metricDescriptor.setField("collectionInterval", collectionInterval);
        metricDescriptor.setField("defaultOn", defaultOn);
        metricDescriptor.setField("rate", rate);
        attributeInfos[1].setDescriptor(metricDescriptor);
        return attributeInfos;
    }

    private ServiceType createExpectedServiceType(String attributeName,
                                                  String metricAliasName,
                                                  String metricName,
                                                  String controlName,
                                                  String units,
                                                  String metricType,
                                                  String metricCategory,
                                                  boolean indicator, long interval, boolean defaultOn, String rate)
    {
        ServiceType expected = new ServiceType("Spring Configurable Bean Factory",
                                               "spring",
                                               new ServiceTypeInfo("Spring Application Spring Configurable Bean Factory",
                                                                   "Description",
                                                                   serverTypeInfo));
        Set controlActions = new HashSet();
        controlActions.add(controlName);
        controlActions.add("set" + attributeName);
        expected.setControlActions(controlActions);
        ConfigSchema expectedCustomProps = new ConfigSchema();
        expectedCustomProps.addOption(new StringConfigOption(attributeName, "Sets monitoring enabled"));
        expected.setCustomProperties(expectedCustomProps);
        ConfigResponse pluginClasses = new ConfigResponse();
        pluginClasses.setValue("measurement", "org.hyperic.hq.product.jmx.MxMeasurementPlugin");
        pluginClasses.setValue("control", "org.hyperic.hq.product.jmx.MxControlPlugin");
        expected.setPluginClasses(pluginClasses);
        ConfigResponse properties = new ConfigResponse();
        // In Java 6 (though officially unsupported) getKeyPropertyList does not
        // return key props in the order specified in the constructor
        // This is the only way to guarantee the object name is as expected
        // (same algorithm used to create it)
        final StringBuffer objectNameTemplate = new StringBuffer(objectName.getDomain()).append(':');
        Hashtable keyProperties = objectName.getKeyPropertyList();
        for (Iterator iterator = keyProperties.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry keyProperty = (Map.Entry) iterator.next();
            objectNameTemplate.append(keyProperty.getKey()).append('=');
            // for now, recognize only type and subtype - replace all others
            // with variable placeholders
            if ("type".equals(keyProperty.getKey()) || "subtype".equals(keyProperty.getKey())) {
                objectNameTemplate.append(keyProperty.getValue());
            } else {
                objectNameTemplate.append('%').append(keyProperty.getKey()).append('%');
            }
            objectNameTemplate.append(',');
        }
        objectNameTemplate.deleteCharAt(objectNameTemplate.length() - 1);
        properties.setValue("Spring Application Spring Configurable Bean Factory.OBJECT_NAME",
                            objectNameTemplate.toString());
        properties.setValue("Spring Application Spring Configurable Bean Factory.NAME",
                            "Spring Configurable Bean Factory");
        expected.setProperties(properties);

        MeasurementInfos measurements = new MeasurementInfos();
        MeasurementInfo expectedMeasurement = new MeasurementInfo();
        expectedMeasurement.setAlias(metricAliasName);
        expectedMeasurement.setName(metricName);
        expectedMeasurement.setCategory(metricCategory.toUpperCase());
        expectedMeasurement.setDefaultOn(defaultOn);
        expectedMeasurement.setIndicator(indicator);
        if ("GAUGE".equals(metricType.toUpperCase())) {
            expectedMeasurement.setCollectionType(MeasurementConstants.COLL_TYPE_DYNAMIC);
        } else if ("COUNTER".equals(metricType.toUpperCase())) {
            expectedMeasurement.setCollectionType(MeasurementConstants.COLL_TYPE_TRENDSUP);
            expectedMeasurement.setRate(rate);
        }
        expectedMeasurement.setInterval(interval);
        expectedMeasurement.setTemplate(objectNameTemplate.toString() + ":" + metricAliasName);
        expectedMeasurement.setUnits(units);
        measurements.addMeasurementInfo(expectedMeasurement);

        MeasurementInfo expectedAvailabilityMeasurement = new MeasurementInfo();
        expectedAvailabilityMeasurement.setAlias(Metric.ATTR_AVAIL);
        expectedAvailabilityMeasurement.setName(Metric.ATTR_AVAIL);
        expectedAvailabilityMeasurement.setCategory(MeasurementConstants.CAT_AVAILABILITY);
        expectedAvailabilityMeasurement.setUnits(MeasurementConstants.UNITS_PERCENTAGE);
        expectedAvailabilityMeasurement.setCollectionType(MeasurementConstants.COLL_TYPE_DYNAMIC);
        expectedAvailabilityMeasurement.setDefaultOn(true);
        expectedAvailabilityMeasurement.setIndicator(true);
        expectedAvailabilityMeasurement.setInterval(600000l);
        expectedAvailabilityMeasurement.setTemplate(objectNameTemplate.toString() + ":Availability");
        measurements.addMeasurementInfo(expectedAvailabilityMeasurement);

        expected.setMeasurements(measurements);
        return expected;
    }

    private ModelMBeanOperationInfo[] createOperationInfos(String controlName) {
        return new ModelMBeanOperationInfo[] { new ModelMBeanOperationInfo(controlName,
                                                                           "Resets metrics",
                                                                           new MBeanParameterInfo[0],
                                                                           "void",
                                                                           MBeanOperationInfo.UNKNOWN) };
    }

    private ModelMBean createServiceModelMBean(ModelMBeanAttributeInfo[] attributeInfos,
                                               ModelMBeanOperationInfo[] operationInfos,
                                               boolean export) throws RuntimeOperationsException, MBeanException
    {
        ModelMBeanInfo info = new ModelMBeanInfoSupport("ConfigurableBeanFactory",
                                                        "Description",
                                                        attributeInfos,
                                                        new ModelMBeanConstructorInfo[0],
                                                        operationInfos,
                                                        new ModelMBeanNotificationInfo[0]);
        final Descriptor descriptor = info.getMBeanDescriptor();
        descriptor.setField("typeName", "Spring Configurable Bean Factory");
        descriptor.setField("export", Boolean.toString(export));
        info.setMBeanDescriptor(descriptor);
        return new RequiredModelMBean(info);
    }

    private Set createServiceTypes(ModelMBeanAttributeInfo[] attributeInfos,
                                   ModelMBeanOperationInfo[] operationInfos,
                                   boolean export) throws Exception
    {

        final ModelMBean testBean = createServiceModelMBean(attributeInfos, operationInfos, export);
        mBeanServer.registerMBean(testBean, objectName);
        Set objectNames = new HashSet();
        objectNames.add(objectName);
        return serviceTypeFactory.create(productPlugin, serverTypeInfo, mBeanServer, objectNames);
    }

    public void setUp() throws Exception {
        super.setUp();
        this.objectName = new ObjectName("spring.application:application=sim,type=ConfigurableBeanFactory,name=beanfac");
        PluginData data = new PluginData();
        this.serverTypeInfo = new ServerTypeInfo("Spring Application", "A Spring standalone application", "");

        data.setProperty("template", "${OBJECT_NAME}:${alias}");
        data.setProperty("measurement-class", "org.hyperic.hq.product.jmx.MxMeasurementPlugin");
        data.setProperty("control-class", "org.hyperic.hq.product.jmx.MxControlPlugin");

        this.productPlugin = new TestProductPlugin("Spring");
        this.productPlugin.setData(data);
        this.serviceTypeFactory = new ServiceTypeFactory();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        mBeanServer.unregisterMBean(objectName);
    }

    /**
     * Verifies that a service type can be built dynamically from MBean metadata
     * 
     * @throws Exception
     */
    public void testCreate() throws Exception {
        ModelMBeanAttributeInfo[] attributeInfos = createAttributeInfos("MonitoringEnabled",
                                                                        "AverageExecutionTime(ms)",
                                                                        "Average Execution Time",
                                                                        "ms",
                                                                        "gauge",
                                                                        "performance");
        ModelMBeanOperationInfo[] operationInfos = createOperationInfos("ResetMetrics");
        Set serviceTypes = createServiceTypes(attributeInfos, operationInfos, true);
        assertEquals(1, serviceTypes.size());
        ServiceType expected = createExpectedServiceType("MonitoringEnabled",
                                                         "AverageExecutionTime(ms)",
                                                         "Average Execution Time",
                                                         "ResetMetrics",
                                                         "ms",
                                                         "gauge",
                                                         "performance",
                                                         true, 300000l,true,null);
        ServiceType actual = (ServiceType) serviceTypes.iterator().next();

        assertTrue(AICompare.aiServiceTypesEqual(expected.getAIServiceTypeValue(), actual.getAIServiceTypeValue()));
    }
    
    /**
     * Verifies that default rate of none is applied to a counter with no specified rate
     * 
     * @throws Exception
     */
    public void testCreateCounterDefaultRate() throws Exception {
        ModelMBeanAttributeInfo[] attributeInfos = createAttributeInfos("MonitoringEnabled",
                                                                        "AverageExecutionTime(ms)",
                                                                        "Average Execution Time",
                                                                        "ms",
                                                                        "counter",
                                                                        "performance");
        ModelMBeanOperationInfo[] operationInfos = createOperationInfos("ResetMetrics");
        Set serviceTypes = createServiceTypes(attributeInfos, operationInfos, true);
        assertEquals(1, serviceTypes.size());
        ServiceType expected = createExpectedServiceType("MonitoringEnabled",
                                                         "AverageExecutionTime(ms)",
                                                         "Average Execution Time",
                                                         "ResetMetrics",
                                                         "ms",
                                                         "counter",
                                                         "performance",
                                                         true, 600000l,true,"none");
        ServiceType actual = (ServiceType) serviceTypes.iterator().next();

        assertTrue(AICompare.aiServiceTypesEqual(expected.getAIServiceTypeValue(), actual.getAIServiceTypeValue()));
    }

    /**
     * Verifies that the default category of UTILIZATION is applied if an unsupported metricCategory is found
     * @throws Exception
     */
    public void testCreateInvalidCategory() throws Exception {
        ModelMBeanAttributeInfo[] attributeInfos = createAttributeInfos("MonitoringEnabled",
                                                                        "AverageExecutionTime",
                                                                        "Average Execution Time",
                                                                        "ms",
                                                                        "counter",
                                                                        "foo",
                                                                        false,"50000","true","1h");
        ModelMBeanOperationInfo[] operationInfos = createOperationInfos("ResetMetrics");
        Set serviceTypes = createServiceTypes(attributeInfos, operationInfos, true);
        assertEquals(1, serviceTypes.size());
        ServiceType expected = createExpectedServiceType("MonitoringEnabled",
                                                         "AverageExecutionTime",
                                                         "Average Execution Time",
                                                         "ResetMetrics",
                                                         "ms",
                                                         "counter",
                                                         "utilization",
                                                         false,50000l, true, "1h");
        ServiceType actual = (ServiceType) serviceTypes.iterator().next();

        assertTrue(AICompare.aiServiceTypesEqual(expected.getAIServiceTypeValue(), actual.getAIServiceTypeValue()));
    }

    /**
     * Verifies that the default unit of "none" is used if an unsupported units if found
     * @throws Exception
     */
    public void testCreateInvalidUnit() throws Exception {
        ModelMBeanAttributeInfo[] attributeInfos = createAttributeInfos("MonitoringEnabled",
                                                                        "AverageExecutionTime",
                                                                        "Average Execution Time",
                                                                        "messages",
                                                                        "counter",
                                                                        "utilization",
                                                                        false,"3000","false","2m");
        ModelMBeanOperationInfo[] operationInfos = createOperationInfos("ResetMetrics");
        Set serviceTypes = createServiceTypes(attributeInfos, operationInfos, true);
        assertEquals(1, serviceTypes.size());
        ServiceType expected = createExpectedServiceType("MonitoringEnabled",
                                                         "AverageExecutionTime",
                                                         "Average Execution Time",
                                                         "ResetMetrics",
                                                         "none",
                                                         "counter",
                                                         "utilization",
                                                         false,3000l, false, "2m");
        ServiceType actual = (ServiceType) serviceTypes.iterator().next();

        assertTrue(AICompare.aiServiceTypesEqual(expected.getAIServiceTypeValue(), actual.getAIServiceTypeValue()));
    }

    /**
     * Verifies that descriptor data exported by the old ManagedMetric annotation from instrumented Spring Framework 2.5.6 is used, as opposed to
     * the new one in open source Spring 3.0 M4.  Old annotation added attributes for "indicator" as well as "attributeType" which differentiated b/w
     * attributes and metrics.  These are no longer present.
     * @throws Exception
     */
    public void testCreateOldManagedMetricStyle() throws Exception {
        ModelMBeanAttributeInfo[] attributeInfos = createAttributeInfos("MonitoringEnabled",
                                                                        "AverageExecutionTime(s)",
                                                                        "Average Execution Time",
                                                                        "s",
                                                                        "counter",
                                                                        "utilization",
                                                                        false,"600", "true","8s");
        ModelMBeanOperationInfo[] operationInfos = createOperationInfos("ResetMetrics");
        Set serviceTypes = createServiceTypes(attributeInfos, operationInfos, true);
        assertEquals(1, serviceTypes.size());
        ServiceType expected = createExpectedServiceType("MonitoringEnabled",
                                                         "AverageExecutionTime(s)",
                                                         "Average Execution Time",
                                                         "ResetMetrics",
                                                         "sec",
                                                         "counter",
                                                         "utilization",
                                                         false, 600l,true, "8s");
        ServiceType actual = (ServiceType) serviceTypes.iterator().next();

        assertTrue(AICompare.aiServiceTypesEqual(expected.getAIServiceTypeValue(), actual.getAIServiceTypeValue()));
    }
    
    /**
     * Verifies that defaul value is applied if defaultOn is set to something other than true/false
     * @throws Exception
     */
    public void testCreateInvalidDefaultOn() throws Exception {
        ModelMBeanAttributeInfo[] attributeInfos = createAttributeInfos("MonitoringEnabled",
                                                                        "AverageExecutionTime(s)",
                                                                        "Average Execution Time",
                                                                        "s",
                                                                        "counter",
                                                                        "utilization",
                                                                        false,"600", "foo","8s");
        ModelMBeanOperationInfo[] operationInfos = createOperationInfos("ResetMetrics");
        Set serviceTypes = createServiceTypes(attributeInfos, operationInfos, true);
        assertEquals(1, serviceTypes.size());
        ServiceType expected = createExpectedServiceType("MonitoringEnabled",
                                                         "AverageExecutionTime(s)",
                                                         "Average Execution Time",
                                                         "ResetMetrics",
                                                         "sec",
                                                         "counter",
                                                         "utilization",
                                                         false, 600l,false, "8s");
        ServiceType actual = (ServiceType) serviceTypes.iterator().next();

        assertTrue(AICompare.aiServiceTypesEqual(expected.getAIServiceTypeValue(), actual.getAIServiceTypeValue()));
    }
    
    /**
     * Verifies default collection interval is used if non-numeric interval specified
     * @throws Exception
     */
    public void testCreateInvalidCollectionInterval() throws Exception {
        ModelMBeanAttributeInfo[] attributeInfos = createAttributeInfos("MonitoringEnabled",
                                                                        "AverageExecutionTime(s)",
                                                                        "Average Execution Time",
                                                                        "s",
                                                                        "counter",
                                                                        "utilization",
                                                                        false,"foo","false","9h");
        ModelMBeanOperationInfo[] operationInfos = createOperationInfos("ResetMetrics");
        Set serviceTypes = createServiceTypes(attributeInfos, operationInfos, true);
        assertEquals(1, serviceTypes.size());
        ServiceType expected = createExpectedServiceType("MonitoringEnabled",
                                                         "AverageExecutionTime(s)",
                                                         "Average Execution Time",
                                                         "ResetMetrics",
                                                         "sec",
                                                         "counter",
                                                         "utilization",
                                                         false, 600000l, false, "9h");
        ServiceType actual = (ServiceType) serviceTypes.iterator().next();

        assertTrue(AICompare.aiServiceTypesEqual(expected.getAIServiceTypeValue(), actual.getAIServiceTypeValue()));
    }

    /**
     * Verifies that resources with export=false in their ModelMBean descriptors
     * will not be auto-discovered
     * 
     * @throws Exception
     */
    public void testIgnoreResourceExportFalse() throws Exception {
        ModelMBeanAttributeInfo[] attributeInfos = createAttributeInfos("MonitoringEnabled",
                                                                        "AverageExecutionTime(ms)",
                                                                        "Average Execution Time",
                                                                        "ms",
                                                                        "gauge",
                                                                        "performance");
        ModelMBeanOperationInfo[] operationInfos = createOperationInfos("ResetMetrics");
        Set serviceTypes = createServiceTypes(attributeInfos, operationInfos, false);
        assertEquals(0, serviceTypes.size());

    }

    /**
     * Verifies that a service type can be updated dynamically from MBean
     * metadata
     * 
     * @throws Exception
     */
    public void testUpdateDynamicType() throws Exception {
        ModelMBeanAttributeInfo[] attributeInfos = createAttributeInfos("MonitoringEnabled",
                                                                        "AverageExecutionTime(ms)",
                                                                        "Average Execution Time",
                                                                        "ms",
                                                                        "gauge",
                                                                        "performance");
        ModelMBeanOperationInfo[] operationInfos = createOperationInfos("ResetMetrics");
        Set serviceTypes = createServiceTypes(attributeInfos, operationInfos, true);

        assertEquals(1, serviceTypes.size());
        ServiceType expected = createExpectedServiceType("MonitoringEnabled",
                                                         "AverageExecutionTime(ms)",
                                                         "Average Execution Time",
                                                         "ResetMetrics",
                                                         "ms",
                                                         "gauge",
                                                         "performance",
                                                         true,300000l, true, null);
        ServiceType actual = (ServiceType) serviceTypes.iterator().next();

        assertTrue(AICompare.aiServiceTypesEqual(expected.getAIServiceTypeValue(), actual.getAIServiceTypeValue()));

        mBeanServer.unregisterMBean(this.objectName);

        attributeInfos = createAttributeInfos("BeanDefinitionNames",
                                              "CacheSize",
                                              "Cache Size",
                                              "ms",
                                              "gauge",
                                              "performance");
        operationInfos = createOperationInfos("Refresh");
        serviceTypes = createServiceTypes(attributeInfos, operationInfos, true);
        assertEquals(1, serviceTypes.size());
        ServiceType expected2 = createExpectedServiceType("BeanDefinitionNames",
                                                          "CacheSize",
                                                          "Cache Size",
                                                          "Refresh",
                                                          "ms",
                                                          "gauge",
                                                          "performance",
                                                          true, 300000l, true, null);
        ServiceType actual2 = (ServiceType) serviceTypes.iterator().next();

        assertTrue(AICompare.aiServiceTypesEqual(expected2.getAIServiceTypeValue(), actual2.getAIServiceTypeValue()));
    }

}
