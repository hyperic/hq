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

package org.hyperic.hq.alerts;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.TemplateManager;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.types.AlertCondition;
import org.hyperic.hq.types.AlertDefinition;
import org.hyperic.hq.types.ResourcePrototype;
/**
 * Unit test of {@link AlertDefinitionXmlParser}
 * @author jhickey
 *
 */
public class AlertDefinitionXmlParserTest
    extends TestCase
{
    private AlertDefinitionXmlParser parser;

    private ResourceManager resourceManager;

    private AuthzSubjectManager authSubjectManager;

    private TemplateManager templateManager;

    private AlertDefinitionManager alertDefinitionManager;

    private static final AuthzSubject SUBJECT = new AuthzSubject(true,
                                                                 "auth",
                                                                 "dept",
                                                                 "something@something.com",
                                                                 true,
                                                                 "Jen",
                                                                 "Hickey",
                                                                 "jen",
                                                                 "phone",
                                                                 "sms",
                                                                 false);

    private void replay() {
        EasyMock.replay(resourceManager, authSubjectManager, templateManager, alertDefinitionManager);
    }

    public void setUp() throws Exception {
        super.setUp();
        this.resourceManager = (ResourceManager) EasyMock.createMock(ResourceManager.class);
        this.authSubjectManager = (AuthzSubjectManager) EasyMock.createMock(AuthzSubjectManager.class);
        this.templateManager = (TemplateManager) EasyMock.createMock(TemplateManager.class);
        this.alertDefinitionManager = (AlertDefinitionManager) EasyMock.createMock(AlertDefinitionManager.class);
        this.parser = new AlertDefinitionXmlParser(resourceManager,
                                                   templateManager,
                                                   authSubjectManager,
                                                   alertDefinitionManager);
    }

    public void testAppdefTypeNotFound() throws PermissionException {
        Integer serverId = Integer.valueOf(234);
        AlertDefinition definition = new AlertDefinition();
        definition.setName("Prop Changed");
        definition.setActive(false);
        definition.setControlFiltered(true);
        definition.setDescription("Something changed");
        definition.setFrequency(0);
        definition.setWillRecover(false);
        definition.setNotifyFiltered(false);
        definition.setPriority(1);
        ResourcePrototype resource = new ResourcePrototype();
        resource.setName("SpringSource tc Server 6.0");
        definition.setResourcePrototype(resource);

        ResourceType type = new ResourceType();
        type.setId(-99);
        Resource expectedResource = new Resource(type, null, "SpringSource tc Server 6.0", null, serverId, false);
        EasyMock.expect(resourceManager.findResourcePrototypeByName("SpringSource tc Server 6.0"))
                .andReturn(expectedResource);

        AlertCondition metricChange = new AlertCondition();
        metricChange.setType(EventConstants.TYPE_CHANGE);
        metricChange.setMetricChange("Failure Count");
        definition.getAlertCondition().add(metricChange);

        replay();
        try {
            AlertDefinitionValue value = parser.parse(definition);
            fail("An Exception should be thrown if resource type not found");
        } catch (AlertDefinitionXmlParserException e) {
            verify();
        }
    }

    public void testConditionTypeNotFound() throws PermissionException {
        Integer serverId = Integer.valueOf(234);
        AlertDefinition definition = new AlertDefinition();
        definition.setName("Prop Changed");
        definition.setActive(false);
        definition.setControlFiltered(true);
        definition.setDescription("Something changed");
        definition.setFrequency(0);
        definition.setWillRecover(false);
        definition.setNotifyFiltered(false);
        definition.setPriority(1);
        ResourcePrototype resource = new ResourcePrototype();
        resource.setName("SpringSource tc Server 6.0");
        definition.setResourcePrototype(resource);

        ResourceType type = new ResourceType();
        type.setId(AuthzConstants.authzServerProto);
        Resource expectedResource = new Resource(type, null, "SpringSource tc Server 6.0", null, serverId, false);
        EasyMock.expect(resourceManager.findResourcePrototypeByName("SpringSource tc Server 6.0"))
                .andReturn(expectedResource);

        EasyMock.expect(authSubjectManager.getOverlordPojo()).andReturn(SUBJECT);
        List templates = new ArrayList();

        Boolean expectedDefaultOn = null;
        EasyMock.expect(templateManager.findTemplatesByMonitorableType(EasyMock.eq(SUBJECT),
                                                                       EasyMock.isA(PageInfo.class),
                                                                       EasyMock.eq("SpringSource tc Server 6.0"),
                                                                       EasyMock.eq(expectedDefaultOn)))
                .andReturn(templates);

        AlertCondition metricChange = new AlertCondition();
        metricChange.setType(-99);
        metricChange.setMetricChange("Failure Count");
        definition.getAlertCondition().add(metricChange);

        replay();
        try {
            AlertDefinitionValue value = parser.parse(definition);
            fail("An Exception should be thrown if condition type not found");
        } catch (AlertDefinitionXmlParserException e) {
            verify();
        }
    }

    public void testInvalidFrequency() {
        Integer serverId = Integer.valueOf(234);
        AlertDefinition definition = new AlertDefinition();
        definition.setName("Prop Changed");
        definition.setActive(false);
        definition.setControlFiltered(true);
        definition.setDescription("Something changed");
        definition.setFrequency(6);
        definition.setWillRecover(false);
        definition.setNotifyFiltered(false);
        definition.setPriority(1);
        ResourcePrototype resource = new ResourcePrototype();
        resource.setName("SpringSource tc Server 6.0");
        definition.setResourcePrototype(resource);

        ResourceType type = new ResourceType();
        type.setId(AuthzConstants.authzServerProto);
        Resource expectedResource = new Resource(type, null, "SpringSource tc Server 6.0", null, serverId, false);
        EasyMock.expect(resourceManager.findResourcePrototypeByName("SpringSource tc Server 6.0"))
                .andReturn(expectedResource);

        replay();
        try {
            AlertDefinitionValue value = parser.parse(definition);
            fail("An Exception should be thrown if invalid frequency found");
        } catch (AlertDefinitionXmlParserException e) {
            verify();
        }
    }

    public void testInvalidPriority() {
        Integer serverId = Integer.valueOf(234);
        AlertDefinition definition = new AlertDefinition();
        definition.setName("Prop Changed");
        definition.setActive(false);
        definition.setControlFiltered(true);
        definition.setDescription("Something changed");
        definition.setFrequency(0);
        definition.setWillRecover(false);
        definition.setNotifyFiltered(false);
        definition.setPriority(99);
        ResourcePrototype resource = new ResourcePrototype();
        resource.setName("SpringSource tc Server 6.0");
        definition.setResourcePrototype(resource);

        ResourceType type = new ResourceType();
        type.setId(AuthzConstants.authzServerProto);
        Resource expectedResource = new Resource(type, null, "SpringSource tc Server 6.0", null, serverId, false);
        EasyMock.expect(resourceManager.findResourcePrototypeByName("SpringSource tc Server 6.0"))
                .andReturn(expectedResource);

        replay();
        try {
            AlertDefinitionValue value = parser.parse(definition);
            fail("An Exception should be thrown if invalid priority found");
        } catch (AlertDefinitionXmlParserException e) {
            verify();
        }
    }

    public void testMeasurementNotFound() throws PermissionException {
        Integer serverId = Integer.valueOf(234);
        AlertDefinition definition = new AlertDefinition();
        definition.setName("Prop Changed");
        definition.setActive(false);
        definition.setControlFiltered(true);
        definition.setDescription("Something changed");
        definition.setFrequency(0);
        definition.setWillRecover(false);
        definition.setNotifyFiltered(false);
        definition.setPriority(1);
        ResourcePrototype resource = new ResourcePrototype();
        resource.setName("SpringSource tc Server 6.0");
        definition.setResourcePrototype(resource);

        AlertCondition metricChange = new AlertCondition();
        metricChange.setType(EventConstants.TYPE_CHANGE);
        metricChange.setMetricChange("Failure Count");
        definition.getAlertCondition().add(metricChange);

        ResourceType type = new ResourceType();
        type.setId(AuthzConstants.authzServerProto);
        Resource expectedResource = new Resource(type, null, "SpringSource tc Server 6.0", null, serverId, false);
        EasyMock.expect(resourceManager.findResourcePrototypeByName("SpringSource tc Server 6.0"))
                .andReturn(expectedResource);
        EasyMock.expect(authSubjectManager.getOverlordPojo()).andReturn(SUBJECT);
        List templates = new ArrayList();

        Boolean expectedDefaultOn = null;
        EasyMock.expect(templateManager.findTemplatesByMonitorableType(EasyMock.eq(SUBJECT),
                                                                       EasyMock.isA(PageInfo.class),
                                                                       EasyMock.eq("SpringSource tc Server 6.0"),
                                                                       EasyMock.eq(expectedDefaultOn)))
                .andReturn(templates);

        replay();
        try {
            AlertDefinitionValue value = parser.parse(definition);
            fail("An Exception should be thrown if measurement not found");
        } catch (AlertDefinitionXmlParserException e) {
            verify();
        }
    }

    public void testNoConditions() throws PermissionException {
        Integer serverId = Integer.valueOf(234);
        AlertDefinition definition = new AlertDefinition();
        definition.setName("Prop Changed");
        definition.setActive(false);
        definition.setControlFiltered(true);
        definition.setDescription("Something changed");
        definition.setFrequency(0);
        definition.setWillRecover(false);
        definition.setNotifyFiltered(false);
        definition.setPriority(1);
        ResourcePrototype resource = new ResourcePrototype();
        resource.setName("SpringSource tc Server 6.0");
        definition.setResourcePrototype(resource);

        ResourceType type = new ResourceType();
        type.setId(AuthzConstants.authzServerProto);
        Resource expectedResource = new Resource(type, null, "SpringSource tc Server 6.0", null, serverId, false);
        EasyMock.expect(resourceManager.findResourcePrototypeByName("SpringSource tc Server 6.0"))
                .andReturn(expectedResource);

        replay();
        try {
            AlertDefinitionValue value = parser.parse(definition);
            fail("An Exception should be thrown if no conditions found");
        } catch (AlertDefinitionXmlParserException e) {
            verify();
        }
    }

    public void testNoDefName() {
        AlertDefinition definition = new AlertDefinition();
        definition.setActive(false);
        definition.setControlFiltered(true);
        definition.setDescription("Something changed");
        definition.setFrequency(0);
        definition.setWillRecover(false);
        definition.setNotifyFiltered(false);
        definition.setPriority(1);
        ResourcePrototype resource = new ResourcePrototype();
        resource.setName("SpringSource tc Server 6.0");
        definition.setResourcePrototype(resource);

        replay();
        try {
            AlertDefinitionValue value = parser.parse(definition);
            fail("An Exception should be thrown if definition name not specified");
        } catch (AlertDefinitionXmlParserException e) {
            verify();
        }
    }

    public void testNonExistentResourceType() {
        AlertDefinition definition = new AlertDefinition();
        definition.setName("Prop Changed");
        definition.setActive(false);
        definition.setControlFiltered(true);
        definition.setDescription("Something changed");
        definition.setFrequency(0);
        definition.setWillRecover(false);
        definition.setNotifyFiltered(false);
        definition.setPriority(1);
        ResourcePrototype resource = new ResourcePrototype();
        resource.setName("SpringSource tc Server 6.0");
        definition.setResourcePrototype(resource);

        EasyMock.expect(resourceManager.findResourcePrototypeByName("SpringSource tc Server 6.0")).andReturn(null);

        replay();
        try {
            AlertDefinitionValue value = parser.parse(definition);
            fail("An Exception should be thrown if resource type not found");
        } catch (AlertDefinitionXmlParserException e) {
            verify();
        }
    }
    
    public void testParseOtherConditions() throws PermissionException {
        Integer serverId = Integer.valueOf(234);
        AlertDefinition definition = new AlertDefinition();
        definition.setName("Prop Changed");
        definition.setActive(false);
        definition.setControlFiltered(true);
        definition.setDescription("Something changed");
        definition.setFrequency(0);
        definition.setWillRecover(false);
        definition.setNotifyFiltered(false);
        definition.setPriority(1);
        ResourcePrototype resource = new ResourcePrototype();
        resource.setName("SpringSource tc Server 6.0");
        definition.setResourcePrototype(resource);

        AlertCondition propCondition = new AlertCondition();
        propCondition.setRequired(false);
        propCondition.setType(EventConstants.TYPE_CUST_PROP);
        propCondition.setProperty("MyProp");
        definition.getAlertCondition().add(propCondition);

        AlertCondition controlCondition = new AlertCondition();
        controlCondition.setRequired(true);
        controlCondition.setControlAction("stop");
        controlCondition.setControlStatus("Failed");
        controlCondition.setType(EventConstants.TYPE_CONTROL);
        definition.getAlertCondition().add(controlCondition);

        AlertCondition logCondition = new AlertCondition();
        logCondition.setRequired(true);
        logCondition.setLogLevel("DBG");
        logCondition.setLogMatches("something");
        logCondition.setType(EventConstants.TYPE_LOG);
        definition.getAlertCondition().add(logCondition);

        AlertCondition cfgCondition = new AlertCondition();
        cfgCondition.setType(EventConstants.TYPE_CFG_CHG);
        cfgCondition.setConfigMatch("configProp");
        definition.getAlertCondition().add(cfgCondition);

        AlertCondition metricChange = new AlertCondition();
        metricChange.setType(EventConstants.TYPE_CHANGE);
        metricChange.setMetricChange("Failure Count");
        definition.getAlertCondition().add(metricChange);

        AlertCondition baselineCondition = new AlertCondition();
        baselineCondition.setBaselineMetric("Availability");
        baselineCondition.setBaselineComparator("=");
        baselineCondition.setBaselineType("mean");
        baselineCondition.setBaselinePercentage(50d);
        baselineCondition.setType(EventConstants.TYPE_BASELINE);
        definition.getAlertCondition().add(baselineCondition);

        ResourceType type = new ResourceType();
        type.setId(AuthzConstants.authzServiceProto);
        Resource expectedResource = new Resource(type, null, "SpringSource tc Server 6.0", null, serverId, false);
        EasyMock.expect(resourceManager.findResourcePrototypeByName("SpringSource tc Server 6.0"))
                .andReturn(expectedResource);
        EasyMock.expect(authSubjectManager.getOverlordPojo()).andReturn(SUBJECT);
        List templates = new ArrayList();
        MeasurementTemplate template = new MeasurementTemplate();
        template.setName("Failure Count");
        template.setId(678);
        templates.add(template);

        MeasurementTemplate template2 = new MeasurementTemplate();
        template2.setName("Availability");
        template2.setId(123);
        templates.add(template2);

        Boolean expectedDefaultOn = null;
        EasyMock.expect(templateManager.findTemplatesByMonitorableType(EasyMock.eq(SUBJECT),
                                                                       EasyMock.isA(PageInfo.class),
                                                                       EasyMock.eq("SpringSource tc Server 6.0"),
                                                                       EasyMock.eq(expectedDefaultOn)))
                .andReturn(templates);

        replay();
        AlertDefinitionValue value = parser.parse(definition);
        verify();
        verifyParsedDefinition(value, definition, serverId, AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        AlertConditionValue[] conditions = value.getConditions();

        assertEquals(6, conditions.length);
        AlertConditionValue actualPropCondition = conditions[0];

        assertEquals(EventConstants.TYPE_CUST_PROP, actualPropCondition.getType());
        assertFalse(actualPropCondition.getRequired());
        assertEquals("MyProp", actualPropCondition.getName());

        AlertConditionValue actualControlCondition = conditions[1];
        assertEquals(EventConstants.TYPE_CONTROL, actualControlCondition.getType());
        assertEquals("stop", actualControlCondition.getName());
        assertEquals("Failed", actualControlCondition.getOption());
        assertTrue(actualControlCondition.getRequired());

        AlertConditionValue actualLogCondition = conditions[2];
        assertTrue(actualLogCondition.getRequired());
        assertEquals(Integer.toString(LogTrackPlugin.LOGLEVEL_DEBUG), actualLogCondition.getName());
        assertEquals("something", actualLogCondition.getOption());

        AlertConditionValue actualConfigCondition = conditions[3];
        assertEquals("configProp", actualConfigCondition.getOption());

        AlertConditionValue actualChangeCondition = conditions[4];
        assertEquals(678, actualChangeCondition.getMeasurementId());
        assertEquals("Failure Count", actualChangeCondition.getName());

        AlertConditionValue actualBaselineCondition = conditions[5];
        assertEquals(123, actualBaselineCondition.getMeasurementId());
        assertEquals("mean", actualBaselineCondition.getOption());
        assertEquals("=", actualBaselineCondition.getComparator());
        assertEquals(50d, actualBaselineCondition.getThreshold());
    }
    
    public void testParseRecoveryAlert() throws PermissionException {
        Integer serverId = Integer.valueOf(234);
        AlertDefinition definition = new AlertDefinition();
        definition.setName("Server Up");
        definition.setActive(true);
        definition.setDescription("Too much memory");
        definition.setFrequency(0);
        definition.setPriority(3);
        ResourcePrototype resource = new ResourcePrototype();
        resource.setName("SpringSource tc Server 6.0");
        definition.setResourcePrototype(resource);
        AlertCondition condition = new AlertCondition();
        condition.setRequired(true);
        condition.setType(EventConstants.TYPE_ALERT);
        condition.setRecover("Server Down");
        definition.getAlertCondition().add(condition);

        ResourceType type = new ResourceType();
        type.setId(AuthzConstants.authzPlatformProto);
        Resource expectedResource = new Resource(type, null, "SpringSource tc Server 6.0", null, serverId, false);
        EasyMock.expect(resourceManager.findResourcePrototypeByName("SpringSource tc Server 6.0"))
                .andReturn(expectedResource);
        EasyMock.expect(authSubjectManager.getOverlordPojo()).andReturn(SUBJECT);
        List templates = new ArrayList();
        Boolean expectedDefaultOn = null;
        EasyMock.expect(templateManager.findTemplatesByMonitorableType(EasyMock.eq(SUBJECT),
                                                                       EasyMock.isA(PageInfo.class),
                                                                       EasyMock.eq("SpringSource tc Server 6.0"),
                                                                       EasyMock.eq(expectedDefaultOn)))
                .andReturn(templates);
        List alertDefs = new ArrayList();
        org.hyperic.hq.events.server.session.AlertDefinition resourceDef = new org.hyperic.hq.events.server.session.AlertDefinition();
        resourceDef.setId(777);
        resourceDef.setName("Server Down");
        alertDefs.add(resourceDef);
        EasyMock.expect(alertDefinitionManager.findAlertDefinitions(SUBJECT, expectedResource)).andReturn(alertDefs);

        replay();
        AlertDefinitionValue value = parser.parse(definition);
        verify();
        verifyParsedDefinition(value, definition, serverId, AppdefEntityConstants.APPDEF_TYPE_PLATFORM);
        AlertConditionValue[] conditions = value.getConditions();
        assertEquals(1, conditions.length);
        AlertConditionValue actualCondition = conditions[0];
        assertEquals(777, actualCondition.getMeasurementId());
        assertEquals(EventConstants.TYPE_ALERT, actualCondition.getType());
        assertTrue(actualCondition.getRequired());
    }

    public void testParseThresholdCondition() throws PermissionException {
        Integer serverId = Integer.valueOf(234);
        AlertDefinition definition = new AlertDefinition();
        definition.setName("Heap Memory Usage High");
        definition.setActive(true);
        definition.setControlFiltered(true);
        definition.setDescription("Too much memory");
        definition.setCount(2);
        definition.setFrequency(2);
        definition.setPriority(3);
        definition.setWillRecover(false);
        definition.setRange(3000);
        definition.setNotifyFiltered(true);
        ResourcePrototype resource = new ResourcePrototype();
        resource.setName("SpringSource tc Server 6.0");
        definition.setResourcePrototype(resource);
        AlertCondition condition = new AlertCondition();
        condition.setRequired(true);
        condition.setType(EventConstants.TYPE_THRESHOLD);
        condition.setThresholdComparator("<");
        condition.setThresholdMetric("Heap Memory Free");
        condition.setThresholdValue(100d);
        definition.getAlertCondition().add(condition);

        ResourceType type = new ResourceType();
        type.setId(AuthzConstants.authzServerProto);
        Resource expectedResource = new Resource(type, null, "SpringSource tc Server 6.0", null, serverId, false);
        EasyMock.expect(resourceManager.findResourcePrototypeByName("SpringSource tc Server 6.0"))
                .andReturn(expectedResource);
        EasyMock.expect(authSubjectManager.getOverlordPojo()).andReturn(SUBJECT);
        List templates = new ArrayList();
        MeasurementTemplate template = new MeasurementTemplate();
        template.setName("Heap Memory Free");
        template.setId(678);
        templates.add(template);
        Boolean expectedDefaultOn = null;
        EasyMock.expect(templateManager.findTemplatesByMonitorableType(EasyMock.eq(SUBJECT),
                                                                       EasyMock.isA(PageInfo.class),
                                                                       EasyMock.eq("SpringSource tc Server 6.0"),
                                                                       EasyMock.eq(expectedDefaultOn)))
                .andReturn(templates);

        replay();
        AlertDefinitionValue value = parser.parse(definition);
        verify();
        verifyParsedDefinition(value, definition, serverId, AppdefEntityConstants.APPDEF_TYPE_SERVER);
        AlertConditionValue[] conditions = value.getConditions();
        assertEquals(1, conditions.length);
        AlertConditionValue actualCondition = conditions[0];
        assertEquals("<", actualCondition.getComparator());
        assertEquals(100d, actualCondition.getThreshold());
        assertEquals(EventConstants.TYPE_THRESHOLD, actualCondition.getType());
        assertTrue(actualCondition.getRequired());
        assertEquals("Heap Memory Free", actualCondition.getName());
        assertEquals(678, actualCondition.getMeasurementId());
    }

    private void verify() {
        EasyMock.verify(resourceManager, authSubjectManager, templateManager, alertDefinitionManager);
    }

    private void verifyParsedDefinition(AlertDefinitionValue actual,
                                        AlertDefinition expected,
                                        Integer appdefId,
                                        int appdefType)
    {
        assertEquals(expected.isActive(), actual.getActive());
        assertEquals(appdefId, actual.getAppdefId());
        assertEquals(appdefType, actual.getAppdefType());
        assertEquals(expected.isControlFiltered(), actual.getControlFiltered());
        assertEquals(expected.getCount(), actual.getCount());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getFrequency(), actual.getFrequencyType());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.isNotifyFiltered(), actual.getNotifyFiltered());
        assertEquals(Integer.valueOf(0), actual.getParentId());
        assertEquals(expected.getPriority(), actual.getPriority());
        assertEquals(expected.getRange(), actual.getRange());
        assertEquals(expected.isWillRecover(), actual.getWillRecover());
    }

}
