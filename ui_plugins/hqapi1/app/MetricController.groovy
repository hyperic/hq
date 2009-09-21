import org.hyperic.hq.hqu.rendit.BaseController

import org.hyperic.hq.hqapi1.ErrorCode;

class MetricController extends ApiController {

    private Closure getMetricXML(m) {
        { doc -> 
            Metric(id             : m.id,
                   interval       : m.interval,
                   enabled        : m.enabled,
                   name           : m.template.name,
                   defalutOn      : m.template.defaultOn,
                   indicator      : m.template.designate,
                   collectionType : m.template.collectionType) {
            MetricTemplate(id              : m.template.id,
                           name            : m.template.name,
                           alias           : m.template.alias,
                           units           : m.template.units,
                           plugin          : m.template.plugin,
                           indicator       : m.template.designate,
                           defaultOn       : m.template.defaultOn,
                           collectionType  : m.template.collectionType,
                           defaultInterval : m.template.defaultInterval,
                           category        : m.template.category.name)
            }
        }
    }

    private Closure getMetricTemplateXML(t) {
        { doc -> 
            MetricTemplate(id              : t.id,
                           name            : t.name,
                           alias           : t.alias,
                           units           : t.units,
                           plugin          : t.plugin,
                           indicator       : t.designate,
                           defaultOn       : t.defaultOn,
                           collectionType  : t.collectionType,
                           defaultInterval : t.defaultInterval,
                           category        : t.category.name)
        }
    }

    private Closure getMetricDataXML(r) {
        { doc ->
            MetricData(resourceId: r.resource.id,
                       resourceName: r.resource.name,
                       metricId: r.metric.id,
                       metricName: r.metric.template.name) {
                for (dp in r.data) {
                    DataPoint(timestamp : dp.timestamp,
                              value     : dp.value)
                }
            }
        }
    }

    private validInterval(long interval) {
        return interval > 0 && interval%60000 == 0
    }

    def getTemplates(params) {
        def prototype = params.getOne("prototype")

        def failureXml = null
        def templates
        if (!prototype) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                       "No prototype given")
        } else {
            // Make sure the prototype exists.
            def proto = resourceHelper.find(prototype: prototype)
            if (!proto) {
                failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                           "Unable to find type " + prototype)
            } else {
                templates = metricHelper.find(all:'templates',
                                              resourceType: prototype)
            }
        }

        renderXml() {
            MetricTemplatesResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                    for (t in templates) {
                        out << getMetricTemplateXML(t)
                    }
                }
            }
        }
    }

    def getMetricTemplate(params) {
        def id = params.getOne('id')?.toInteger()

        def template
        def failureXml = null
        if (!id) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                       "Metric template id not given")
        } else {
            template = metricHelper.findTemplateById(id)
            if (!template) {
                failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                           "Unable to find template id=" + id)
            }
        }

        renderXml() {
            MetricTemplateResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                    out << getMetricTemplateXML(template)
                }
            }
        }
    }

    def getMetrics(params) {
        def failureXml
        def metrics
        def resourceId = params.getOne("resourceId")?.toInteger()
        def enabled = params.getOne("enabled")?.toBoolean()

        if (!resourceId) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                       "Resource id not given")
        } else {
            def res = getResource(resourceId)
            if (!res) {
                failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                           "Unable to find resource id=" + resourceId)
            } else {
                try {
                    if (enabled != null && enabled) {
                        metrics = res.enabledMetrics
                    } else {
                        metrics = res.metrics
                    }                    
                } catch (Exception e) {
                    log.error("UnexpectedError: " + e.getMessage(), e)
                    failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
                }
            }
        }

        renderXml() {
            MetricsResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                    for (m in metrics) {
                        out << getMetricXML(m)
                    }
                }
            }
        }
    }

    def getMetric(params) {
        def failureXml
        def metric
        def metricId = params.getOne("id")?.toInteger()

        if (!metricId) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                       "Metric id not given")
        } else {
            try {
                metric = metricHelper.findMeasurementById(metricId);
                if (!metric) {
                    failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                               "Unable to find metric id=" + metricId)
                }
            } catch (Exception e) {
                log.error("UnexpectedError: " + e.getMessage(), e)
                failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
            }
        }
        
        renderXml() {
            MetricResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                    out << getMetricXML(metric)
                }
            }
        }
    }

    // TODO: Need collection based method for enable/disable
    def syncMetrics(params) {

        def syncRequest = new XmlParser().parseText(getPostData())
        def xmlMetric = syncRequest['Metric']
        def failureXml = null

        xmlMetric.each { metric ->
            def id = metric.'@id'?.toInteger()
            def m = metricHelper.findMeasurementById(id)
            if(!m) {
                failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                           "Unable to find metric with id " + id)
                return
            }

            def enabled   = metric.'@enabled'.toBoolean();
            def interval  = metric.'@interval'?.toLong();

            try {
                if (enabled != null && enabled != m.enabled) {
                    if (enabled) {
                        if (interval == null) {
                            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                                       "No interval given for metric id " + id)
                            return
                        }

                        if (interval == 0) {
                            interval = m.template.defaultInterval
                        }

                        if (!validInterval(interval)) {
                            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                                       "Invalid interval " + interval +
                                                       " for metric " + id)
                            return
                        }

                        m.enableMeasurement(user, interval)
                    } else {
                        m.disableMeasurement(user)
                    }
                } else {
                    // Enabled flag was not changed, check for interval update.
                    if (interval != null && interval != m.interval) {
                        if (!validInterval(interval)) {
                            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                                       "Invalid interval " + interval +
                                                       " for metric " + id)
                            return
                        }
                        m.updateMeasurementInterval(user, interval)
                    }
                }
            } catch (Exception e) {
                log.error("UnexpectedError: " + e.getMessage(), e)
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

    def syncTemplates(params) {

        def syncRequest = new XmlParser().parseText(getPostData())
        def xmlTemplate = syncRequest['MetricTemplate']
        def failureXml = null

        xmlTemplate.each { template ->
            def id = template.'@id'?.toInteger()
            def t = metricHelper.findTemplateById(id)
            if(!t) {
                failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                           "Unable to find template with id " + id)
                return
            }

            def indicator = template.'@indicator'?.toBoolean();
            def defaultOn = template.'@defaultOn'?.toBoolean();
            def interval  = template.'@defaultInterval'?.toLong();

            try {
                if (indicator != null && indicator != t.designate) {
                    // Availability templates cannot be changed.
                    if (t.availability && !indicator) {
                        failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                                   "Indicator flag for availability " +
                                                   "template " + t.id +
                                                   " cannot be false")
                        return
                    }

                    t.setDefaultIndicator(user, indicator)
                }

                if (defaultOn != null && defaultOn != t.defaultOn) {
                    t.setDefaultOn(user, defaultOn)
                }

                if (interval != null && interval != t.defaultInterval) {

                    if (!validInterval(interval)) {
                        failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                                   "Invalid interval " + interval +
                                                   " for template " + t.id)
                        return
                    }

                    t.setDefaultInterval(user, interval)
                }
            } catch (Exception e) {
                log.error("UnexpectedError: " + e.getMessage(), e)
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

    def getData(params) {
        def metricId = params.getOne("metricId")?.toInteger()
        def start = params.getOne("start")?.toLong()
        def end = params.getOne("end")?.toLong()

        def failureXml = null
        if (metricId == null || start == null || end == null) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                       "Missing argument")
        }

        if (end < start) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                       "End time cannot be < start time")
        }

        def metric = metricHelper.findMeasurementById(metricId)
        if (!metric) {
            failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                       "Unable to find metric id=" + metricId)
        }

        def data;
        if (!failureXml) {
            try {
                data = metric.getData(start, end)
            } catch (Exception e) {
                log.error("UnexpectedError: " + e.getMessage(), e);
                failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
            }
        }

        renderXml() {
            MetricDataResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    def result = [resource: metric.resource, metric: metric,
                                  data: data]
                    out << getSuccessXML()
                    out << getMetricDataXML(result)
                }
            }
        }
    }

    def getGroupData(params) {
        def groupId = params.getOne("groupId")?.toInteger()
        def templateId = params.getOne("templateId")?.toInteger()
        def start = params.getOne("start")?.toLong()
        def end = params.getOne("end")?.toLong()

        if ((!groupId || !templateId || !start || !end) ||
            (end < start)) {
            renderXml() {
                MetricsDataResponse() {
                    out << getFailureXML(ErrorCode.INVALID_PARAMETERS)
                }
            }
            return
        }

        // Ensure passed group id and template id exist.
        def group = resourceHelper.findGroup(groupId)
        def template = metricHelper.findTemplateById(templateId)
        if (!group || !template) {
            renderXml() {
                MetricsDataResponse() {
                    out << getFailureXML(ErrorCode.OBJECT_NOT_FOUND)
                }
            }
            return
        }

        // Make sure group is compatible
        def prototype = group.resourcePrototype
        if (!prototype) {
            renderXml() {
                MetricsDataResponse() {
                    out << getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                         "Group " + group.name + " is not a " +
                                         "compatible group")
                }
            }
            return
        }

        // Ensure compatible group type has the template given
        def templates = metricHelper.find(all:'templates',
                                          resourceType: prototype.name)
        def foundTemplate = templates.find { it.id == templateId }
        if (!foundTemplate) {
            renderXml() {
                MetricsDataResponse() {
                    out << getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                         "Unable to find metric template " +
                                         template.name + " for group " +
                                         group.name + " with compatible type " +
                                         prototype.name)
                }
            }
            return
        }
        
        def results = []
        def members = group.resources
        members.each { resource ->
            def m = resource.metrics.find { it.template.id == templateId }
            def data = []
            if (m) {
                data = m.getData(start, end)
            }
            results << [resource: resource, metric: m, data: data]
        }

        renderXml() {
            MetricsDataResponse() {
                out << getSuccessXML()
                for (result in results) {
                    out << getMetricDataXML(result)
                }
            }
        }
    }

    def getResourceData(params) {
        
        def ids = params["ids"]
        def templateId = params.getOne("templateId")?.toInteger()
        def start = params.getOne("start")?.toLong()
        def end = params.getOne("end")?.toLong()

        if ((!ids || !ids.length == 0 || !templateId || !start || !end) ||
            (end < start)) {
            renderXml() {
                MetricsDataResponse() {
                    out << getFailureXML(ErrorCode.INVALID_PARAMETERS)
                }
            }
            return
        }

        // Make sure the passed in template exists.
        def template = metricHelper.findTemplateById(templateId)
        if (!template) {
            renderXml() {
                MetricsDataResponse() {
                    out << getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                         "Template with id " + templateId +
                                         " not found")
                }
            }
            return
        }

        // Validate the resources exist.
        def results = []
        for (String id : ids) {
            def resource = getResource(id.toInteger())
            if (!resource) {
                renderXml() {
                    MetricsDataResponse() {
                        out << getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                             "Resource with id " + id + 
                                             " not found")
                    }
                }
                return
            }
            def m = resource.metrics.find { it.template.id == templateId }
            if (!m) {
                renderXml() {
                    MetricsDataResponse() {
                        out << getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                             "Unable to find metric " +
                                             template.name + " for resource " +
                                             resource.name)
                    }
                }
                return
            }

            def data = m.getData(start, end)
            results << [resource: resource, metric: m, data: data]
        }

        renderXml() {
            MetricsDataResponse() {
                out << getSuccessXML()
                for (result in results) {
                    out << getMetricDataXML(result)
                }
            }
        }
    }
}
