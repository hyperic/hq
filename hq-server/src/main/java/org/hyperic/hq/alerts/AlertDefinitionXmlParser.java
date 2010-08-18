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

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerImpl;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceManagerImpl;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.server.session.AlertDefinitionManagerImpl;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MeasurementTemplateSortField;
import org.hyperic.hq.measurement.server.session.TemplateManagerImpl;
import org.hyperic.hq.measurement.shared.TemplateManager;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.types.AlertCondition;
import org.hyperic.hq.types.AlertDefinition;
import org.hyperic.hq.types.AlertDefinitionsResponse;
import org.hyperic.hq.types.XmlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Parser responsible for transforming AlertDefinitionsResponse XML to a Set of
 * {@link AlertDefinitionValue}s
 * @author jhickey
 * 
 */
@Component
public class AlertDefinitionXmlParser {

    private static final Map EVENT_LEVEL_TO_NUM = new HashMap();

    static {
        EVENT_LEVEL_TO_NUM.put("ANY", Integer.valueOf(-1));
        EVENT_LEVEL_TO_NUM.put("ERR", Integer.valueOf(LogTrackPlugin.LOGLEVEL_ERROR));
        EVENT_LEVEL_TO_NUM.put("WRN", Integer.valueOf(LogTrackPlugin.LOGLEVEL_WARN));
        EVENT_LEVEL_TO_NUM.put("INF", Integer.valueOf(LogTrackPlugin.LOGLEVEL_INFO));
        EVENT_LEVEL_TO_NUM.put("DBG", Integer.valueOf(LogTrackPlugin.LOGLEVEL_DEBUG));
    }

    private final ResourceManager resourceManager;
    private final TemplateManager templateManager;
    private final AuthzSubjectManager authzSubjectManager;
    private final AlertDefinitionManager alertDefinitionManager;

    @Autowired
    public AlertDefinitionXmlParser(ResourceManager resourceManager,
                                    TemplateManager templateManager,
                                    AuthzSubjectManager authzSubjectManager,
                                    AlertDefinitionManager alertDefinitionManager)
    {
        this.resourceManager = resourceManager;
        this.templateManager = templateManager;
        this.authzSubjectManager = authzSubjectManager;
        this.alertDefinitionManager = alertDefinitionManager;
    }

    private MeasurementTemplate find(List templates, String metricName, Resource resource) {
        for (Iterator templateIterator = templates.iterator(); templateIterator.hasNext();) {
            MeasurementTemplate template = (MeasurementTemplate) templateIterator.next();
            if (template.getName().equals(metricName)) {
                return template;
            }
        }
        throw new AlertDefinitionXmlParserException("Unable to find metric " + metricName + " for resource " +
                                                    resource.getName());
    }

    private int getAppdefType(Resource resource) {
        Integer typeId = resource.getResourceType().getId();
        if (AuthzConstants.authzPlatformProto.equals(typeId)) {
            return AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
        } else if (AuthzConstants.authzServerProto.equals(typeId)) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVER;
        } else if (AuthzConstants.authzServiceProto.equals(typeId)) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        } else {
            throw new AlertDefinitionXmlParserException("Resource [" + resource + "] is not an appdef " +
                                                        "resource type");
        }
    }

    private List getTemplates(AuthzSubject subject, Resource resource) {
        try {
            return templateManager.findTemplatesByMonitorableType(subject,
                                                                  PageInfo.getAll(MeasurementTemplateSortField.TEMPLATE_NAME,
                                                                                  true),
                                                                  resource.getName(),
                                                                  null);
        } catch (PermissionException e) {
            throw new AlertDefinitionXmlParserException("Error obtaining measurement templates.  Cause: " +
                                                        e.getMessage());
        }
    }

    private AlertConditionValue parse(AlertCondition condition,
                                      AlertDefinition definition,
                                      List templates,
                                      Resource resource,
                                      AuthzSubject subject)
    {
        AlertConditionValue alertConditionValue = new AlertConditionValue();
        alertConditionValue.setRequired(condition.isRequired());
        alertConditionValue.setType(condition.getType());
        switch (alertConditionValue.getType()) {
        case EventConstants.TYPE_THRESHOLD:
            parseThresholdCondition(condition, definition, templates, resource, alertConditionValue);
            break;
        case EventConstants.TYPE_BASELINE:
            parseBaselineCondition(condition, definition, templates, resource, alertConditionValue);
            break;
        case EventConstants.TYPE_CONTROL:
            parseControlCondition(condition, definition, alertConditionValue);
            break;
        case EventConstants.TYPE_CHANGE:
            parseMetricChangeCondition(condition, definition, templates, resource, alertConditionValue);
            break;
        case EventConstants.TYPE_ALERT:
            parseAlertCondition(condition, definition, resource, subject, alertConditionValue);
            break;
        case EventConstants.TYPE_CUST_PROP:
            parsePropertyChangeCondition(condition, definition, alertConditionValue);
            break;
        case EventConstants.TYPE_LOG:
            parseLogCondition(condition, definition, alertConditionValue);
            break;
        case EventConstants.TYPE_CFG_CHG:
            parseConfigChangeCondition(condition, alertConditionValue);
            break;
        default:
            throw new AlertDefinitionXmlParserException("Unhandled AlertCondition " + "type " +
                                                        alertConditionValue.getType() + " for " + definition.getName());
        }
        return alertConditionValue;
    }

    AlertDefinitionValue parse(AlertDefinition definition) {
        if (definition.getName() == null) {
            throw new AlertDefinitionXmlParserException("Required attribute name not found for definition");
        }
        String name = definition.getResourcePrototype().getName();
        Resource resource = resourceManager.findResourcePrototypeByName(name);
        if (resource == null) {
            throw new AlertDefinitionXmlParserException("Cannot find resource type " + name + " for definition " +
                                                        definition.getName());
        }
        // Alert priority must be 1-3
        int priority = definition.getPriority();
        if (priority < 1 || priority > 3) {
            throw new AlertDefinitionXmlParserException("AlertDefinition priority must be " +
                                                        "between 1 (low) and 3 (high) " + "found=" + priority);
        }

        // Alert frequency must be 0-4
        int frequency = definition.getFrequency();
        if (frequency != 0 && frequency != 2 && frequency != 4) {
            throw new AlertDefinitionXmlParserException("AlertDefinition frequency must be " + "either 0, 2, or 4 " +
                                                        "found=" + frequency);
        }

        if (definition.getAlertCondition() == null || definition.getAlertCondition().size() < 1) {
            // At least one condition is always required
            throw new AlertDefinitionXmlParserException("At least 1 AlertCondition is " + "required for definition " +
                                                        definition.getName());
        }

        AppdefEntityTypeID aeid = new AppdefEntityTypeID(getAppdefType(resource), resource.getInstanceId());
        AlertDefinitionValue alertDefValue = new AlertDefinitionValue();
        alertDefValue.setName(definition.getName());
        alertDefValue.setDescription(definition.getDescription());
        alertDefValue.setAppdefType(aeid.getType());
        alertDefValue.setAppdefId(aeid.getId());
        alertDefValue.setParentId(EventConstants.TYPE_ALERT_DEF_ID);
        alertDefValue.setPriority(definition.getPriority());
        alertDefValue.setActive(definition.isActive());
        alertDefValue.setWillRecover(definition.isWillRecover());
        alertDefValue.setNotifyFiltered(definition.isNotifyFiltered());
        alertDefValue.setControlFiltered(definition.isControlFiltered());
        alertDefValue.setFrequencyType(definition.getFrequency());
        alertDefValue.setCount(definition.getCount());
        alertDefValue.setRange(definition.getRange());

        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        List templates = getTemplates(overlord, resource);

        for (Iterator conditions = definition.getAlertCondition().iterator(); conditions.hasNext();) {
            AlertCondition condition = (AlertCondition) conditions.next();
            AlertConditionValue alertConditionValue = parse(condition, definition, templates, resource, overlord);
            alertDefValue.addCondition(alertConditionValue);
        }
        return alertDefValue;
    }

    /**
     * 
     * @param alertDefinitionsXml InputStream of XML that can be deserialized to
     *        an {@link AlertDefinitionsResponse}
     * @return A Set of {@link AlertDefinitionValue}s parsed from the XML
     */
    public Set<AlertDefinitionValue> parse(InputStream alertDefinitionsXml) {
        List<AlertDefinition> alertDefinitions;
        try {
            AlertDefinitionsResponse response = (AlertDefinitionsResponse) XmlUtil.deserialize(AlertDefinitionsResponse.class,
                                                                                               alertDefinitionsXml);
            alertDefinitions = response.getAlertDefinition();
        } catch (JAXBException e) {
            throw new AlertDefinitionXmlParserException("Error parsing alert definition XML.  Cause: " + e.getMessage());
        }
        return parse(alertDefinitions);
    }

    Set<AlertDefinitionValue> parse(List<AlertDefinition> alertDefinitions) {
        final Set<AlertDefinitionValue> alertDefinitionValues = new HashSet<AlertDefinitionValue>();
        for (AlertDefinition definition : alertDefinitions) {
            alertDefinitionValues.add(parse(definition));
        }
        return alertDefinitionValues;
    }

    private void parseAlertCondition(AlertCondition condition,
                                     AlertDefinition definition,
                                     Resource resource,
                                     AuthzSubject subject,
                                     AlertConditionValue alertConditionValue)
    {
        if (condition.getRecover() == null) {
            throw new AlertDefinitionXmlParserException("Required attribute recover not found for condition of alert definition " +
                                                        definition.getName());
        }
        List resourceDefs;
        try {
            resourceDefs = alertDefinitionManager.findAlertDefinitions(subject, resource);
        } catch (PermissionException e) {
            throw new AlertDefinitionXmlParserException("Error obtaining resource type alerts.  Cause: " +
                                                        e.getMessage());
        }
        boolean foundDefinition = false;
        for (Iterator resourceAlerts = resourceDefs.iterator(); resourceAlerts.hasNext();) {
            org.hyperic.hq.events.server.session.AlertDefinition resourceDef = (org.hyperic.hq.events.server.session.AlertDefinition) resourceAlerts.next();
            if (condition.getRecover().equals(resourceDef.getName())) {
                alertConditionValue.setMeasurementId(resourceDef.getId().intValue());
                foundDefinition = true;
                break;
            }
        }
        if (!foundDefinition) {
            throw new AlertDefinitionXmlParserException("Unable to find recovery " + "with name '" +
                                                        condition.getRecover() + "'");
        }
    }

    private void parseBaselineCondition(AlertCondition condition,
                                        AlertDefinition definition,
                                        List templates,
                                        Resource resource,
                                        AlertConditionValue alertConditionValue)
    {
        if (condition.getBaselineMetric() == null) {
            throw new AlertDefinitionXmlParserException("Required attribute baselineMetric not found for condition of alert definition " +
                                                        definition.getName());
        }
        if (condition.getBaselineComparator() == null) {
            throw new AlertDefinitionXmlParserException("Required attribute baselineComparator not found for condition of alert definition " +
                                                        definition.getName());
        }
        alertConditionValue.setName(condition.getBaselineMetric());
        MeasurementTemplate template = find(templates, alertConditionValue.getName(), resource);
        String baselineType = condition.getBaselineType();
        if (!"min".equals(baselineType) && !"max".equals(baselineType) && !"mean".equals(baselineType)) {
            throw new AlertDefinitionXmlParserException("Invalid baseline type '" + baselineType + "'");
        }

        alertConditionValue.setMeasurementId(template.getId().intValue());
        alertConditionValue.setComparator(condition.getBaselineComparator());
        alertConditionValue.setThreshold(condition.getBaselinePercentage().doubleValue());
        alertConditionValue.setOption(baselineType);
    }

    private void parseConfigChangeCondition(AlertCondition condition, AlertConditionValue alertConditionValue) {
        String configMatch = condition.getConfigMatch();
        if (configMatch != null) {
            alertConditionValue.setOption(configMatch);
        }
    }

    private void parseControlCondition(AlertCondition condition,
                                       AlertDefinition definition,
                                       AlertConditionValue alertConditionValue)
    {
        if (condition.getControlAction() == null) {
            throw new AlertDefinitionXmlParserException("Required attribute controlAction not found for condition of alert definition " +
                                                        definition.getName());
        }
        String controlStatus = condition.getControlStatus();
        if (!"Completed".equals(controlStatus) && !"In Progress".equals(controlStatus) &&
            !"Failed".equals(controlStatus))
        {
            throw new AlertDefinitionXmlParserException("Invalid control condition " + "status " + controlStatus);
        }
        alertConditionValue.setName(condition.getControlAction());
        alertConditionValue.setOption(controlStatus);
    }

    private void parseLogCondition(AlertCondition condition,
                                   AlertDefinition definition,
                                   AlertConditionValue alertConditionValue)
    {
        Integer level = (Integer) EVENT_LEVEL_TO_NUM.get(condition.getLogLevel());
        if (level == null) {
            throw new AlertDefinitionXmlParserException("Unknown log level " + condition.getLogLevel());
        }
        alertConditionValue.setName(level.toString());
        alertConditionValue.setOption(condition.getLogMatches());
    }

    private void parseMetricChangeCondition(AlertCondition condition,
                                            AlertDefinition definition,
                                            List templates,
                                            Resource resource,
                                            AlertConditionValue alertConditionValue)
    {
        if (condition.getMetricChange() == null) {
            throw new AlertDefinitionXmlParserException("Required attribute metricChange not found for condition of alert definition " +
                                                        definition.getName());
        }
        alertConditionValue.setName(condition.getMetricChange());
        MeasurementTemplate template = find(templates, alertConditionValue.getName(), resource);
        alertConditionValue.setMeasurementId(template.getId().intValue());
    }

    private void parsePropertyChangeCondition(AlertCondition condition,
                                              AlertDefinition definition,
                                              AlertConditionValue alertConditionValue)
    {
        if (condition.getProperty() == null) {
            throw new AlertDefinitionXmlParserException("Required attribute property not found for condition of alert definition " +
                                                        definition.getName());
        }
        alertConditionValue.setName(condition.getProperty());
    }

    private void parseThresholdCondition(AlertCondition condition,
                                         AlertDefinition definition,
                                         List templates,
                                         Resource resource,
                                         AlertConditionValue alertConditionValue)
    {
        if (condition.getThresholdMetric() == null) {
            throw new AlertDefinitionXmlParserException("Required attribute thresholdMetric not found for condition of alert definition " +
                                                        definition.getName());
        }
        if (condition.getThresholdComparator() == null) {
            throw new AlertDefinitionXmlParserException("Required attribute thresholdComparator not found for condition of alert definition " +
                                                        definition.getName());
        }
        alertConditionValue.setName(condition.getThresholdMetric());
        MeasurementTemplate template = find(templates, alertConditionValue.getName(), resource);
        alertConditionValue.setMeasurementId(template.getId().intValue());
        alertConditionValue.setComparator(condition.getThresholdComparator());
        alertConditionValue.setThreshold(condition.getThresholdValue().doubleValue());
    }
}
