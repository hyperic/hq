
import org.hyperic.hq.hqapi1.ErrorCode

import org.hyperic.hq.auth.shared.SessionManager
import org.hyperic.hq.authz.shared.PermissionException
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID
import org.hyperic.hq.bizapp.server.session.EventsBossEJBImpl as EventsBoss
import org.hyperic.hq.events.AlertSeverity
import org.hyperic.hq.events.EventConstants
import org.hyperic.hq.events.shared.AlertConditionValue
import org.hyperic.hq.events.shared.AlertDefinitionValue
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl as AMan
import org.hyperic.hq.measurement.shared.ResourceLogEvent
import org.hyperic.hq.product.LogTrackPlugin
import ApiController

public class AlertdefinitionController extends ApiController {
    private eventBoss   = EventsBoss.one
    private aMan        = AMan.one

    private EVENT_LEVEL_TO_NUM = [
        ANY: -1,
        ERR : LogTrackPlugin.LOGLEVEL_ERROR,
        WRN : LogTrackPlugin.LOGLEVEL_WARN,
        INF : LogTrackPlugin.LOGLEVEL_INFO,
        DBG : LogTrackPlugin.LOGLEVEL_DEBUG,
    ]
    
    /**
     * Seems as though the measurementId column for alert conditions can
     * equal 0 (or something else not found in the DB?)
     *
     * We safely avoid any problems by returning 'Unknown' for templates
     * we can't find.
     */
    private getTemplate(int mid, typeBased) {
        if (typeBased) {
            try {
                return metricHelper.findTemplateById(mid)
            } catch (Exception e) {
                log.warn("Lookup of template id=${mid} failed", e)
            }
        }
        else {
            try {
                return metricHelper.findMeasurementById(mid).template
            } catch (Exception e) {
                log.warn("Lookup of metric id=${mid} failed", e)
            }
        }
        return null
    }

    private Closure getAlertDefinitionXML(d, excludeIds) {
        { out ->
            def attrs = [name: d.name,
                         description: d.description,
                         priority: d.priority,
                         active: d.active,
                         frequency: d.frequencyType,
                         count: d.count,
                         range: d.range,
                         willRecover: d.willRecover,
                         notifyFiltered: d.notifyFiltered,
                         controlFiltered: d.controlFiltered]

            if (!excludeIds) {
                attrs['id'] = d.id
            }

            // parent is nullable.
            if (d.parent != null) {
                attrs['parent'] = d.parent.id
            }

            AlertDefinition(attrs) {

                if (d.resource) {
                    if (d.parent != null && d.parent.id == 0) {
                        ResourcePrototype(id: d.resource.id,
                                          name: d.resource.name)
                    } else {
                        Resource(id : d.resource.id,
                                 name : d.resource.name)
                    }
                }
                if (d.escalation) {
                    def e = d.escalation
                    Escalation(id :           e.id,
                               name :         e.name,
                               description :  e.description,
                               pauseAllowed : e.pauseAllowed,
                               maxPauseTime : e.maxPauseTime,
                               notifyAll :    e.notifyAll,
                               repeat :       e.repeat)
                }
                for (c in d.conditions) {
                    // Attributes common to all conditions
                    def conditionAttrs = [required: c.required,
                                          type: c.type]

                    if (c.type == EventConstants.TYPE_THRESHOLD) {
                        def metric = getTemplate(c.measurementId, d.typeBased)
                        if (!metric) {
                            log.warn("Unable to find metric " + c.measurementId +
                                     "for definition " + d.name)
                            continue
                        } else {
                            conditionAttrs["thresholdMetric"] = metric.name
                            conditionAttrs["thresholdComparator"] = c.comparator
                            conditionAttrs["thresholdValue"] = c.threshold
                        }
                    } else if (c.type == EventConstants.TYPE_BASELINE) {
                        def metric = getTemplate(c.measurementId, d.typeBased)
                        if (!metric) {
                            log.warn("Unable to find metric " + c.measurementId +
                                     "for definition " + d.name)
                            continue
                        } else {
                            conditionAttrs["baselineMetric"] = metric.name
                            conditionAttrs["baselineComparator"] = c.comparator
                            conditionAttrs["baselinePercentage"] = c.threshold
                            conditionAttrs["baselineType"] = c.optionStatus
                        }
                    } else if (c.type == EventConstants.TYPE_CHANGE) {
                        def metric = getTemplate(c.measurementId, d.typeBased)
                        if (!metric) {
                            log.warn("Unable to find metric " + c.measurementId +
                                     "for definition " + d.name)
                            continue
                        } else {
                            conditionAttrs["metricChange"] = metric.name
                        }
                    } else if (c.type == EventConstants.TYPE_CUST_PROP) {
                        conditionAttrs["property"] = c.name
                    } else if (c.type == EventConstants.TYPE_LOG) {
                        int level = c.name.toInteger()
                        conditionAttrs["logLevel"] = ResourceLogEvent.getLevelString(level)
                        conditionAttrs["logMatches"] = c.optionStatus
                    } else if (c.type == EventConstants.TYPE_ALERT) {
                        def alert = alertHelper.getById(c.measurementId)
                        if (alert == null) {
                            // TODO: This is not handled correctly in HQ.  NPE
                            //       is thrown rather than null returned.
                            log.warn("Unable to find recover condition " +
                                     c.measurementId + " for " + c.name)
                            continue
                        } else {
                            conditionAttrs["recover"] = alert.name
                        }
                    } else if (c.type == EventConstants.TYPE_CFG_CHG) {
                        conditionAttrs["configMatch"] = c.optionStatus
                    } else if (c.type == EventConstants.TYPE_CONTROL) {
                        conditionAttrs["controlAction"] = c.name
                        conditionAttrs["controlStatus"] = c.optionStatus
                    } else {
                        log.warn("Unhandled condition type " + c.type +
                                 " for condition " + c.name)
                    }
                    // Write it out
                    AlertCondition(conditionAttrs)
                }
            }
        }
    }

    def listDefinitions(params) {

        def alertNameFilter = params.getOne('alertNameFilter')
        def resourceNameFilter = params.getOne('resourceNameFilter')
        def groupName = params.getOne('groupName')
        def escalationId = params.getOne('escalationId')?.toInteger()

        def excludeTypeBased = params.getOne('excludeTypeBased')?.toBoolean()
        if (excludeTypeBased == null) {
            excludeTypeBased = false;
        }

        def parentId = params.getOne('parentId')?.toInteger()

        def failureXml
        def definitions = []

        if (parentId) {
            def typeAlert = alertHelper.getById(parentId)
            if (!typeAlert) {
                failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                           "Unable to find parent alert definition " +
                                           "with id " + parentId)
            } else if (!typeAlert.parent || typeAlert.parent.id > 0) {
                failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                           "Alert definition with id " +
                                           parentId + " is not a type based " +
                                           "definition")
            } else {
                definitions = typeAlert.children
            }
        } else if (escalationId != null) {
            def escalation = escalationHelper.getEscalation(escalationId, null)
            if (!escalation) {
                failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                           "Escalation with id = " + escalationId +
                                           " not found")
            } else {
                // TODO: Add to alert helper
                definitions = aMan.getUsing(escalation)
                if (excludeTypeBased) {
                    definitions = definitions.findAll { it.parent == null }
                }
            }
        } else {
            definitions = alertHelper.findDefinitions(AlertSeverity.LOW, null,
                                                      excludeTypeBased)
        }

        // Filter
        try {
            if (alertNameFilter) {
                definitions = definitions.findAll { it.name ==~ alertNameFilter }
            }
            if (resourceNameFilter) {
                definitions = definitions.findAll { it.resource.name ==~ resourceNameFilter }
            }
        } catch (java.util.regex.PatternSyntaxException e) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                       "Invalid syntax: " + e.getMessage())
        }

        if (groupName) {
            def group = getGroup(null, groupName)
            if (!group) {
                failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                           "Unable to find group with name " + groupName)
            } else {
                def resources = group.resources
                definitions = definitions.findAll { resources.contains(it.resource) }
            }
        }

        renderXml() {
            out << AlertDefinitionsResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                    for (definition in definitions.sort {a, b -> a.id <=> b.id}) {
                        out << getAlertDefinitionXML(definition, false)
                    }
                }
            }
        }
    }

    def listTypeDefinitions(params) {
        def excludeIds = params.getOne('excludeIds')?.toBoolean()
        def definitions = alertHelper.findTypeBasedDefinitions()

        def noIds = false
        if (excludeIds) {
            noIds = true
        }

        renderXml() {
            out << AlertDefinitionsResponse() {
                out << getSuccessXML()
                // Order by id's so recoveries come after problem alerts
                for (definition in definitions.sort {a, b -> a.id <=> b.id}) {
                    out << getAlertDefinitionXML(definition, noIds)
                }
            }
        }
    }

    def delete(params) {
        def id   = params.getOne("id")?.toInteger()

        def alertdefinition = alertHelper.getById(id)
        def failureXml = null

        if (!alertdefinition) {
            failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                       "Alert definition with id " + id +
                                       " not found")
        } else if (alertdefinition.parent && alertdefinition.parent.id > 0) {
            failureXml = getFailureXML(ErrorCode.NOT_SUPPORTED,
                                       "Unable to delete alert definition based on " +
                                       "type alert definition " + 
                                       alertdefinition.parent.id)
        } else {
            try {
                alertdefinition.delete(user)
            } catch (PermissionException e) {
                failureXml = getFailureXML(ErrorCode.PERMISSION_DENIED)
            } catch (Exception e) {
                failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
            }
        }

        renderXml() {
            StatusResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                }
            }
        }
    }

    private checkRequiredAttributes(name, xml, attrs) {
        for (attr in attrs) {
            if (xml."@${attr}" == null) {
                return getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                     "Required attribute '" + attr +
                                     "' not given for " + name)
            }
        }
        return null
    }

    def sync(params) {
        def syncRequest = new XmlParser().parseText(getPostData())
        def definitions = []

        for (xmlDef in syncRequest['AlertDefinition']) {
            def failureXml = null
            def resource = null // Can be a resource or a prototype in the case of type alerts
            boolean typeBased
            def existing = null
            Integer id = xmlDef.'@id'?.toInteger()
            if (id) {
                existing = alertHelper.getById(id)
                if (!existing) {
                    failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                               "Definition with id " + id +
                                               " not found")
                } else {
                    typeBased = (existing.parent != null && existing.parent.id == 0)
                    resource = existing.resource
                }
            } else {
                if (xmlDef['Resource'].size() ==1 &&
                    xmlDef['ResourcePrototype'].size() == 1) {
                    failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                               "Only one of Resource or " +
                                               "ResourcePrototype required for " +
                                               xmlDef.'@name')
                } else if (xmlDef['Resource'].size() == 1) {
                    typeBased = false
                    def rid = xmlDef['Resource'][0].'@id'?.toInteger()
                    resource = getResource(rid)
                    if (!resource) {
                        failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                                   "Cannot find resource with " +
                                                   "id " + id)
                    }
                } else if (xmlDef['ResourcePrototype'].size() == 1) {
                    typeBased = true
                    def name = xmlDef['ResourcePrototype'][0].'@name'
                    resource = resourceHelper.findResourcePrototype(name)
                    if (!resource) {
                        failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                                   "Cannot find resource type " +
                                                   name + " for definition " +
                                                   xmlDef.'@name')
                    }

                    // For type based alerts - attempt to look up by name
                    // if no id was given.
                    try {
                        def allTypeAlerts = alertHelper.findTypeBasedDefinitions()
                        def existingTypeAlerts = allTypeAlerts.grep {
                            it.name == xmlDef.'@name' &&
                            it.resource.name == name
                        }

                        if (existingTypeAlerts.size() > 1) {
                            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                                       "Found multiple (" +
                                                       existingTypeAlerts.size() +
                                                       ") matches for alert " +
                                                       "definition " +
                                                       xmlDef.'@name')
                        } else if (existingTypeAlerts.size() == 1) {
                            existing = existingTypeAlerts[0]
                            log.debug("Found existing type alert=" + existing)
                        }
                    } catch (PermissionException e) {
                        failureXml = getFailureXML(ErrorCode.PERMISSION_DENIED)
                    }
                } else {
                    failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                               "A single Resource or " +
                                               "ResourcePrototype is required for " +
                                               xmlDef.'@name')
                }
            }

            // Required attributes, basically everything but description
            ['controlFiltered', 'notifyFiltered', 'willRecover', 'range', 'count',
             'frequency', 'active', 'priority',
             'name'].each { attr ->
                if (xmlDef."@${attr}" == null) {
                    failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                              "Required attribute " + attr +
                                              " not found for definition " +
                                              xmlDef.'@name')
                }
            }

            // At least one condition is always required
            if (!xmlDef['AlertCondition'] || xmlDef['AlertCondition'].size() < 1) {
                failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                           "At least 1 AlertCondition is " +
                                           "required for definition " +
                                           xmlDef.'@name')
            }

            // Configure any escalations
            def escalation = null
            if (xmlDef['Escalation'].size() == 1) {

                def xmlEscalation = xmlDef['Escalation'][0]
                def escName = xmlEscalation.'@name'
                if (escName) {
                    escalation = escalationHelper.getEscalation(null, escName)
                }

                if (!escalation) {
                    failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                               "Unable to find escalation with " +
                                               "name '" + escName + "'")
                }
            }

            // Alert priority must be 1-3
            int priority = xmlDef.'@priority'.toInteger()
            if (priority < 1 || priority > 3) {
                failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                           "AlertDefinition priority must be " +
                                           "between 1 (low) and 3 (high) " +
                                           "found=" + priority)
            }

            // Alert frequency must be 0-4
            int frequency = xmlDef.'@frequency'.toInteger()
            if (frequency < 0 || frequency > 4) {
                failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                           "AlertDefinition frequency must be " +
                                           "between 0 and 4 " +
                                           "found=" + frequency)
            }

            // Error with AlertDefinition attributes
            if (failureXml) {
                renderXml() {
                    AlertDefinitionsResponse() {
                        out << failureXml
                    }
                }
                return
            }

            def aeid;
            if (typeBased) {
                aeid = new AppdefEntityTypeID(resource.appdefType,
                                              resource.instanceId)
            } else {
                aeid = resource.entityId
            }

            AlertDefinitionValue adv = new AlertDefinitionValue();
            adv.id          = existing?.id
            adv.name        = xmlDef.'@name'
            adv.description = xmlDef.'@description'
            adv.appdefType  = aeid.type
            adv.appdefId    = aeid.id
            adv.priority    = xmlDef.'@priority'?.toInteger()
            adv.active      = xmlDef.'@active'.toBoolean()
            adv.willRecover = xmlDef.'@willRecover'.toBoolean()
            adv.notifyFiltered = xmlDef.'@notifyFiltered'?.toBoolean()
            adv.controlFiltered = xmlDef.'@controlFiltered'?.toBoolean()
            adv.frequencyType  = xmlDef.'@frequency'.toInteger()
            adv.count = xmlDef.'@count'.toLong()
            adv.range = xmlDef.'@range'.toLong()
            adv.escalationId = escalation?.id
            if (existing) {
                // If the alert is pre-existing, set the parent id.
                adv.parentId = existing.parent?.id
            }

            def templs
            if (typeBased) {
                def args = [:]
                args.all = 'templates'
                args.resourceType = resource.name
                templs = metricHelper.find(args)
            } else {
                // TODO: This gets all metrics, should warn if that metric is disabled?
                templs = resource.metrics
            }

            def isRecovery = false

            for (xmlCond in xmlDef['AlertCondition']) {
                AlertConditionValue acv = new AlertConditionValue()
                def acError

                acError = checkRequiredAttributes(adv.name, xmlCond,
                                                  ['required','type'])
                if (acError != null) {
                    failureXml = acError
                    break
                }

                acv.required = xmlCond.'@required'.toBoolean()
                acv.type = xmlCond.'@type'.toInteger()

                switch (acv.type) {
                    case EventConstants.TYPE_THRESHOLD:
                        acError = checkRequiredAttributes(adv.name, xmlCond,
                                                          ['thresholdMetric',
                                                           'thresholdComparator',
                                                           'thresholdValue'])
                        if (acError != null) {
                            failureXml = acError
                            break
                        }

                        acv.name = xmlCond.'@thresholdMetric'
                        def template = templs.find {
                            acv.name == (typeBased ? it.name : it.template.name)
                        }
                        if (!template) {
                            failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                                       "Unable to find metric " +
                                                       acv.name + " for " +
                                                       resource.name)
                            break
                        }

                        acv.measurementId = template.id
                        acv.comparator    = xmlCond.'@thresholdComparator'
                        acv.threshold     = Double.valueOf(xmlCond.'@thresholdValue')
                        break
                    case EventConstants.TYPE_BASELINE:
                        acError = checkRequiredAttributes(adv.name, xmlCond,
                                                          ['baselineMetric',
                                                           'baselineComparator',
                                                           'baselinePercentage',
                                                           'baselineType'])
                        if (acError != null) {
                            failureXml = acError
                            break
                        }

                        acv.name = xmlCond.'@baselineMetric'
                        def template = templs.find {
                            acv.name == (typeBased ? it.name : it.template.name)
                        }
                        if (!template) {
                            failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                                       "Unable to find metric " +
                                                       acv.name + " for " +
                                                       resource.name)
                            break
                        }

                        def baselineType = xmlCond.'@baselineType'
                        if (!baselineType.equals("min") &&
                            !baselineType.equals("max")&&
                            !baselineType.equals("mean")) {
                            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                                       "Invalid baseline type '" +
                                                       baselineType + "'")
                            break
                        }


                        acv.measurementId = template.id
                        acv.comparator    = xmlCond.'@baselineComparator'
                        acv.threshold     = Double.valueOf(xmlCond.'@baselinePercentage')
                        acv.option        = baselineType
                        break
                    case EventConstants.TYPE_CONTROL:
                        acError = checkRequiredAttributes(adv.name, xmlCond,
                                                          ['controlAction',
                                                           'controlStatus'])
                        if (acError != null) {
                            failureXml = acError
                            break
                        }

                        def controlStatus = xmlCond.'@controlStatus'
                        if (!controlStatus.equals("Completed") &&
                            !controlStatus.equals("In Progress") &&
                            !controlStatus.equals("Failed")) {
                            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                                       "Invalid control condition " +
                                                       "status " + controlStatus)
                            break
                        }

                        // TODO: Check resource for given control action
                        acv.name   = xmlCond.'@controlAction'
                        acv.option = controlStatus
                        break
                    case EventConstants.TYPE_CHANGE:
                        acError = checkRequiredAttributes(adv.name, xmlCond,
                                                          ['metricChange'])
                        if (acError != null) {
                            failureXml = acError
                            break
                        }

                        acv.name = xmlCond.'@metricChange'
                        def template = templs.find {
                            acv.name == (typeBased ? it.name : it.template.name)
                        }
                        if (!template) {
                            failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                                       "Unable to find metric " +
                                                       acv.name + " for " +
                                                       resource.name)
                            break
                        }
                        acv.measurementId = template.id
                        break
                    case EventConstants.TYPE_ALERT:
                        acError = checkRequiredAttributes(adv.name, xmlCond,
                                                          ['recover'])
                        if (acError != null) {
                            failureXml = acError
                            break
                        }

                        isRecovery = true

                        // If a resource alert, look up alert by name
                        if (resource) {
                            log.debug("Looking up alerts for resource=" + resource.id)
                            def resourceDefs = resource.getAlertDefinitions(user)
                            def recovery = resourceDefs.find { it.name == xmlCond.'@recover' }
                            if (recovery) {
                                log.info("Found recovery definition " + recovery.id)
                                acv.measurementId = recovery.id
                                break
                            }
                        }

                        if (!acv.measurementId) {
                            failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                                       "Unable to find recovery " +
                                                       "with name '" +
                                                       xmlCond.'@recover' + "'")
                        }

                        break
                    case EventConstants.TYPE_CUST_PROP:
                        acError = checkRequiredAttributes(adv.name, xmlCond,
                                                          ['property'])
                        if (acError != null) {
                            failureXml = acError
                            break
                        }
                        acv.name = xmlCond.'@property'
                        break
                    case EventConstants.TYPE_LOG:
                        acError = checkRequiredAttributes(adv.name, xmlCond,
                                                          ['logLevel',
                                                           'logMatches'])
                        if (acError != null) {
                            failureXml = acError
                            break
                        }


                        def level = EVENT_LEVEL_TO_NUM[xmlCond.'@logLevel']
                        if (level == null) {
                            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                                       "Unknown log level " +
                                                       xmlCond.'@logLevel')
                            break
                        }

                        acv.name = level.toString()
                        acv.option = xmlCond.'@logMatches'
                        break
                    case EventConstants.TYPE_CFG_CHG:

                        def configMatch = xmlCond.'@configMatch'
                        if (configMatch) {
                            acv.option = configMatch
                        }
                        break
                    default:
                        failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                                   "Unhandled AlertCondition " +
                                                   "type " + acv.type + " for " +
                                                   adv.name)
                }

                // Error with AlertCondition
                if (failureXml) {
                    renderXml() {
                        AlertDefinitionsResponse() {
                            out << failureXml
                        }
                    }
                    return
                }
                adv.addCondition(acv)
            }

            // TODO: Migrate this to AlertHelper
            try {
                def sessionId = SessionManager.instance.put(user)
                if (adv.id == null) {
                    def newDef
                    if (typeBased) {
                        newDef =
                            eventBoss.createResourceTypeAlertDefinition(sessionId,
                                                                        aeid, adv)
                    } else {
                        newDef = eventBoss.createAlertDefinition(sessionId,
                                                                     adv)
                    }
                    adv.id = newDef.id
                } else {
                    eventBoss.updateAlertDefinition(sessionId, adv)
                }
            } catch (Exception e) {
                log.error("Error updating alert definition", e)
                failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR,
                                           e.getMessage())
            }

            // Error with save/update
            if (failureXml) {
                renderXml() {
                    AlertDefinitionsResponse() {
                        out << failureXml
                    }
                }
                return
            }

            def pojo = alertHelper.getById(adv.id)

            // Deal with Escalations
            if (escalation) {
                // TODO: Backend should handle escalations on recovery alerts
                if (isRecovery) {
                    log.warn("Skipping escalation for definition '" + pojo.name +
                             "'.  Escalations not allowed for recovery alerts.")
                } else {
                    pojo.setEscalation(user, escalation)
                }
            } else {
                pojo.unsetEscalation(user)
            }

            // Keep synced defintions for sync return XML
            definitions << pojo
        }

        renderXml() {
            out << AlertDefinitionsResponse() {
                out << getSuccessXML()
                for (alertdef in definitions) {
                    out << getAlertDefinitionXML(alertdef, false)
                }
            }
        }
    }
}