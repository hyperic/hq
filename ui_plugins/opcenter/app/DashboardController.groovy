import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.events.server.session.AlertManagerEJBImpl as AlertMan
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl as EscMan
import org.hyperic.hibernate.PageInfo
import org.hyperic.util.StringUtil
import org.json.JSONObject
import java.text.DateFormat
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.events.server.session.Alert
import org.hyperic.hq.events.EventConstants
import org.hyperic.hq.appdef.server.session.DownResSortField
import org.hyperic.hq.authz.shared.PermissionException
import org.hyperic.hq.events.AlertSeverity
import org.hyperic.hq.galerts.server.session.GalertLogSortField
import org.hyperic.hq.events.AlertFiredEvent
import org.hyperic.hq.escalation.EscalationEvent

class DashboardController extends BaseController
{
    private alertMan = AlertMan.one
    private escMan   = EscMan.one
    private PRIORITIES = ["", "!", "!!", "!!!"]
    private DateFormat DF = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                                           DateFormat.SHORT)

    private static int TYPEFILTER_ALL         = 0
    private static int TYPEFILTER_DOWN        = 1
    private static int TYPEFILTER_ALERTSALL   = 2
    private static int TYPEFILTER_ALERTSESC   = 3
    private static int TYPEFILTER_ALERTSNOESC = 4

    private static _resToPlatformMap = [:]

    private _summaryData = [:]

    protected void init() {
        setJSONMethods(['updateDashboard']) 
    }

    private getIconUrl(String img, String title, String fn) {
        def imgUrl = urlFor(asset:'images') + "/" + img
        def onClick = ""
        if (fn) {
            onClick = "onclick=\"${fn}\""
        }

        """<img src="${imgUrl}" title="${title}" ${onClick}">"""
    }

    /**
     * Method to get the platform Resource for the given Resource, pulling it
     * from the cache if found.
     */
    private getPlatform(resource) {
        def plat = _resToPlatformMap[resource]
        if (!plat) {
            plat = resource.platform
            _resToPlatformMap[resource] = plat
        }

        return plat
    }

    // TODO: Should try to migrate this to the OpCenterDAO
    private canView(resource) {
        def appdefRes

        if (resource.isPlatform()) {
            appdefRes = resource.toPlatform()
        } else if (resource.isServer()) {
            appdefRes = resource.toServer()
        } else if (resource.isService()) {
            appdefRes = resource.toService()
        } else {
            throw IllegalArgumentException("Unhandled type: " + resource)
        }

        try {
            appdefRes.checkPerms(operation:'view', user:user)
            return true
        } catch (PermissionException e) {
            return false
        }
    }

    private getUnfixedAlerts(params, typefilter) {
        def groupId = params.getOne('groupFilter')?.toInteger()

        def res = []
        def inEscLow = 0, inEscMed = 0, inEscHigh = 0;
        def unfixedLow = 0, unfixedMed = 0, unfixedHigh = 0;

        // Classic alerts

        def unfixed = OpCenterDAO.getUnfixedAlerts(groupId)
        log.info("Found " + unfixed.size() + " unfixed alerts")

        for (it in unfixed) {

            def esc   = it[1]
            def count = it[2]
            def aid   = it[3]

            if (typefilter == TYPEFILTER_ALERTSESC && !esc) {
                continue
            } else if (typefilter == TYPEFILTER_ALERTSNOESC && esc) {
                continue
            }

            def result = [:]

            def alert = alertMan.findAlertById(aid.toInteger())
            def definition = alert?.definition
            def resource = definition?.resource

            // Check if alert definition has been removed
            if (resource) {

                if (!canView(resource)) {
                    continue
                }

                result["Platform"] = getPlatform(resource)
                result["Resource"] = resource
                result["Alert"] = alert
                result["Priority"] = definition.priority
                result["StatusType"] = "Alert"
                result["Duration"]   = System.currentTimeMillis() - alert.getCtime()
                result["StatusInfo"] = new StringBuffer()
                result["StatusInfo"] << count + " occurrences. "
                result["LastCheck"] = alert.ctime
                result["Escalation"] = esc

                // States
                result["State"] = new StringBuffer()

                // Counts
                if (esc) {
                    if (definition.priority == EventConstants.PRIORITY_LOW) {
                        inEscLow++
                    } else if (definition.priority == EventConstants.PRIORITY_MEDIUM) {
                        inEscMed++
                    } else if (definition.priority == EventConstants.PRIORITY_HIGH) {
                        inEscHigh++
                    }
                }

                if (definition.priority == EventConstants.PRIORITY_LOW) {
                    unfixedLow++
                } else if (definition.priority == EventConstants.PRIORITY_MEDIUM) {
                    unfixedMed++
                } else if (definition.priority == EventConstants.PRIORITY_HIGH) {
                    unfixedHigh++
                }

                res << result
            }
        }

        // Group alerts
        def range = System.currentTimeMillis()
        def groupFilter = (groupId != -1) ? groupId : null
        def unfixedGroupAlerts =
            alertHelper.findGroupAlerts(AlertSeverity.LOW, range, range, false,
                                        true, groupFilter,
                                        PageInfo.getAll(GalertLogSortField.SEVERITY, false))
        log.info("Found " + unfixedGroupAlerts.size() + " unfixed group alerts")

        for (it in unfixedGroupAlerts) {

            if (typefilter == TYPEFILTER_DOWN || typefilter == TYPEFILTER_ALERTSNOESC) {
                // Don't show group alerts in down resources or if filtering
                // by alerts without escalations. (All group alerts require an escalation)
                continue
            }

            def result = [:]
            result["Group"] = it.alertDef.group
            result["GroupAlert"] = it
            result["Priority"] = it.alertDef.severityEnum
            result["StatusType"] = "Alert"
            result["Duration"]   = System.currentTimeMillis() - it.timestamp
            result["StatusInfo"] = new StringBuffer()
            result["StatusInfo"] << it.longReason + ". "
            result["Escalation"] = it.alertDef.escalation
            result["State"] = new StringBuffer()
            result["LastCheck"] = it.timestamp

            switch (it.alertDef.severityEnum) {
                case 1:
                    unfixedLow++
                    inEscLow++
                    break;
                case 2:
                    unfixedMed++
                    inEscMed++
                    break;
                case 3:
                    unfixedHigh++
                    inEscHigh++
                    break;
            }

            res << result
        }

        _summaryData["AlertsUnfixedLow"] = unfixedLow
        _summaryData["AlertsUnfixedMed"] = unfixedMed
        _summaryData["AlertsUnfixedHigh"] = unfixedHigh
        _summaryData["AlertsUnfixed"] = unfixedLow + unfixedMed + unfixedHigh
        _summaryData["AlertsInEscLow"] = inEscLow
        _summaryData["AlertsInEscMed"] = inEscMed
        _summaryData["AlertsInEscHigh"] = inEscHigh
        _summaryData["AlertsInEsc"] = inEscLow + inEscMed + inEscHigh

        res
    }

    private getDownResources(params) {
        def groupId = params.getOne('groupFilter')?.toInteger()

        def groupMemberIds = null
        if (groupId != null && groupId > 0) {
            def group = resourceHelper.findGroup(groupId)
            if (group) {
                groupMemberIds = group.resources*.id
            }
        }

        def res = []

        long start = System.currentTimeMillis()
        def downResources = resourceHelper.
                getDownResources(null, PageInfo.getAll(DownResSortField.DOWNTIME, true))
        log.info("Down metric query took " + (System.currentTimeMillis() - start) +
                 ". Found " + downResources.size() + " entries")

        def downPlatforms = 0
        def downRes = 0

        downResources.each {
            def resource = it.resource.resource

            if (groupMemberIds && !groupMemberIds.contains(resource.id)) {
                log.debug("Skipping resource " + resource.id +
                          ", not in group " + groupId)
            } else {
                downRes++
                if (resource.isPlatform()) {
                    downPlatforms++
                }

                def result = [:]
                result["Platform"] = getPlatform(resource)
                result["Resource"] = resource
                result["Priority"] = 3
                result["StatusType"] = "Resource Down"
                result["Duration"] = it?.duration
                result["StatusInfo"] = new StringBuffer()                
                res << result
            }
        }

        _summaryData.put("DownResources", downRes)
        _summaryData.put("DownPlatforms", downPlatforms)

        res
    }

    protected sortAndPage(res, PageInfo pInfo, params) {

        // Filter by platform, if given.
        def platformFilter = params.getOne('platformFilter')
        if (platformFilter && platformFilter.size() > 0) {
            res = res.grep { it.Platform?.name?.contains(platformFilter) }
        }

        if (res.size() == 0) {
            return res
        }

        def d = pInfo.sort.description
	    res = res.sort {a, b ->
            def col1 = a."${d}"
            def col2 = b."${d}"

            if (col1 instanceof Resource) {
            	return col1?.name <=> col2?.name
            } else if (col1 instanceof Alert) {
                return col1.definition.name <=> col2.definition.name
            } else {
                return col1 <=> col2
            }
        }

        if (!pInfo.ascending)
            res = res.reverse()

        def startIdx = pInfo.startRow
        def endIdx   = startIdx + pInfo.pageSize
        if (endIdx >= res.size)
            endIdx = -1

        return res[startIdx..endIdx]
    }

    private getSuiteData(PageInfo pInfo, params) {
        double pageSize = params.getOne('pageSize')?.toDouble()
        def typeFilter = params.getOne('typeFilter')?.toInteger()

        def res = []
        def start = System.currentTimeMillis()

        if (typeFilter == TYPEFILTER_ALL || typeFilter == TYPEFILTER_DOWN) {
            res.addAll(getDownResources(params))
        }

        if (typeFilter != TYPEFILTER_DOWN) {
            res.addAll(getUnfixedAlerts(params, typeFilter))
        }

        // We'll always have at least 1 page.
        def numPages = Math.max(1, (Integer)Math.ceil(res.size() / pageSize))

        _summaryData["FetchTime"] = System.currentTimeMillis() - start
        _summaryData["LastUpdated"] = System.currentTimeMillis()
        _summaryData["TotalRows"] = res.size()
        _summaryData["NumPages"] = numPages

        def sortedAndPaged = sortAndPage(res, pInfo, params)

        // After sort/page, add info that is more expensive to gather and is
        // not sortable.
        sortedAndPaged.each {

            def resource = it["Resource"]
            if (resource) {
                def log = OpCenterDAO.getLastLog(resource)
                if (log) {
                    it["StatusInfo"] << "Last event: " + log.detail
                }

                def availMetric = it.Resource.getAvailabilityMeasurement()
                def last = availMetric.getLastDataPoint()
                it["LastCheck"] = last.timestamp
            }

            def alert = null
            if (it["Alert"]) {
                alert = it["Alert"]
                def reason = alertMan.getLongReason(alert)
                it["StatusInfo"] << reason + ". "

                def lastCheck = 0
                for (condition in alert.definition.conditions) {
                    int condType = condition.type
                    // Search conditions that have measurement ids.
                    if (condType == 1 || condType == 2 || condType == 4) {
                        def condMetric = metricHelper.findMeasurementById(condition.measurementId)
                        def last = condMetric.getLastDataPoint()
                        if (last && last.timestamp > lastCheck) {
                            lastCheck = last.timestamp
                            it["StatusInfo"] << " Current value = " +
                                condMetric.template.renderWithUnits(last.value) + ". "
                        }
                    }
                }

                // Fall back to last alert evaluation if no metric check is
                // available.
                if (lastCheck == 0) {
                    lastCheck = alert.ctime
                }
                it["LastCheck"] = lastCheck

            } else if (it["GroupAlert"]) {
                alert = it["GroupAlert"]
            }

            def esc = it["Escalation"]
            if (alert && esc) {
                it["State"] << getIconUrl("notify.gif", "Alert In Escalation", null)

                // TODO: There must be a better way to get this..
                def actionLogs = alert.getActionLog().asList()
                def lastLog = actionLogs.get(actionLogs.size() - 1)
                it["LastEscalation"] = lastLog.timeStamp
                it["StatusInfo"] << "Last action: " + lastLog.detail + ". "

                def definition = alert?.definition
                def escState = escMan.findEscalationState(definition)
                if (escState) {
                    it["StatusInfo"] << "Next escalation at " +
                        DF.format(new Date(escState.nextActionTime)) + ". "

                    def acked = escState.getAcknowledgedBy()
                    if (acked) {
                        def ackedBy = DF.format(lastLog.timeStamp) +
                                      ": " + lastLog.detail + ". "
                        it["State"] << getIconUrl("ack.gif", ackedBy, null)
                    }
                }
            }                    
        }
    }

    public getDashboardSchema() {
        def selectCol      = new TableColumn('Select', '<input type="checkbox" id="dashboardTable_CheckAllBox" onclick="MyAlertCenter.toggleAll(this)" />', false)
        def platformCol    = new TableColumn('Platform', "Platform", true)
        def resourceCol    = new TableColumn('Resource', 'Resource', true)
        def alertCol       = new TableColumn('Alert', 'Alert Name', true)
        def priorityCol    = new TableColumn('Priority', 'Priority', true)
        def statusTypeCol  = new TableColumn("StatusType", "Status Type", true)
        def lastCheckCol   = new TableColumn('LastCheck', 'Last Check', false)
        def lastEscalation = new TableColumn('LastEscalation', 'Last Escalation', true)
        def durationCol    = new TableColumn('Duration', 'Duration', true)
        def stateCol       = new TableColumn('State', 'State', false)
        def statusInfoCol  = new TableColumn('StatusInfo', 'Status Information', false)

        def globalId = 0
        [
            getData: {pageInfo, params ->
                getSuiteData(pageInfo, params)
            },
            defaultSort: platformCol,
            defaultSortOrder: 1,
            styleClass: {
                if (!it.Priority) {
                    return "OpStyleGreen"
                } else if (it.Priority.equals(1)) {
                    return "OpStyleYellow"
                } else if (it.Priority.equals(2)) {
                    return "OpStyleOrange"
                } else if (it.Priority.equals(3)) {
                    if (it.StatusType == "Resource Down") {
                        return "OpStyleGray"
                    } else {
                        return "OpStyleRed"
                    }
                } else {
                    return ""
                }
            },
            rowId: {globalId++},
            columns: [
                [field: selectCol,
                 width:'3%',
                 label: {
                     if (it.Alert) {
                     	def esc = it.Alert.definition.escalation
                     	def pause = (esc == null ? "0" : (esc.pauseAllowed ? esc.maxPauseTime : "0"))
                     	// checkbox id is in the format: {portalName}|{appdefKey}|{alertId}|{maxPauseTime}
                     	def id = "dashboardTable|" + it.Alert.alertDefinition.appdefEntityId.appdefKey + "|" + it.Alert.id + "|" + pause
                        def member = (it.Alert.ackable ? "ackableAlert" : "fixableAlert")
                        return "<input type='checkbox' name='ealerts' id='" + id + "' class='" + member + "' value='-559038737:" + it.Alert.id +"' onclick='MyAlertCenter.toggleAlertButtons(this)' />"
                     } else if (it.GroupAlert) {
                     	def esc = it.GroupAlert.definition.escalation
                     	def pause = (esc == null ? "0" : (esc.pauseAllowed ? esc.maxPauseTime : "0"))
                     	// checkbox id is in the format: {portalName}|{appdefKey}|{alertId}|{maxPauseTime}
                     	def id = "dashboardTable|" + it.GroupAlert.alertDef.appdefID.appdefKey + "|" + it.GroupAlert.id + "|" + pause
                   	    def member = (it.GroupAlert.acknowledgeable ? "ackableAlert" : "fixableAlert")
             	        return it.fixed ? "" : "<input type='checkbox' name='ealerts' id='" + id + "' class='" + member + "' value='195934910:" + it.GroupAlert.id +"' onclick='MyAlertCenter.toggleAlertButtons(this)' />"
                     } else {
                         return ""
                     }
                 }],  
                [field:  platformCol,
                 width:  '15%',
                 nowrap: false,
                 label:  {
                     if (it.Platform) {
                        return "<a href=\"${it.Platform.urlFor(null)}\" target=\"_blank\">${it.Platform.name}</a>"
                     } else {
                        return ""
                     }
                 }],
                [field:  resourceCol,
                 width:  '15%',
                 nowrap: false,
                 label:  {
                     if (it.Resource) {
                         return "<a href=\"${it.Resource.urlFor(null)}\" target=\"_blank\">${it.Resource.name}</a>"
                     } else if (it.Group) {
                         return "<a href=\"${it.Group.urlFor(null)}\" target=\"_blank\">${it.Group.resource.name}</a>"
                     } else {
                         return ""
                     }
                 }],
                [field:  alertCol,
                 width:  '10%',
                 nowrap: false,
                 label:  {
                     if (it.Alert) {
                         return "<a href=\"${it.Alert.urlFor(null)}\" target=\"_blank\">${it.Alert.alertDefinition.name}</a>"
                     } else if (it.GroupAlert) {
                         return "<a href=\"${it.GroupAlert.urlFor(null)}\" target=\"_blank\">${it.GroupAlert.alertDef.name}</a>" 
                     } else {
                         return ""
                     }
                 }],
                [field:  priorityCol,
                 width:  '3%',
                 nowrap: false,
                 label:  { if (it.Priority) { PRIORITIES[it.Priority] } } ],
                [field:  statusTypeCol,
                 width:  '5%',
                 nowrap: false,
                 label:  { it.StatusType }],                
                [field:  lastEscalation,
                 width:  '6%',
                 nowrap: false,
                 label:  { it.LastEscalation ? DF.format(new Date(it.LastEscalation)) : ""}],
                [field:  lastCheckCol,
                 width:  '6%',
                 nowrap: false,
                 label:  { it.LastCheck ? DF.format(new Date(it.LastCheck)) : ""}],
                [field:  durationCol,
                 width:  '5%',
                 nowrap: false,
                 label:  { it.Duration ? StringUtil.formatDuration(it.Duration) : ""}],
                [field:  stateCol,
                 width:  '5%',
                 nowrap: false,
                 label:  { it.State }],
                [field:  statusInfoCol,
                 width:  '30%',
                 nowrap: false,
                 label:  { it.StatusInfo }]
            ],
        ]
    }

    def index(params) {
        def groups = resourceHelper.findViewableGroups()

    	render(locals:[ groups: groups,
                        DASHBOARD_SCHEMA : getDashboardSchema() ])
    }

    def updateDashboard(params) {
        log.info("Updating dashboard data " + params)
        def start = System.currentTimeMillis();
        JSONObject json = DojoUtil.processTableRequest(getDashboardSchema(), params)
        log.info("Gathered dashboard data in " + (System.currentTimeMillis() - start))
        log.info("Summary Data=" + _summaryData)
        JSONObject summary = new JSONObject(_summaryData)
        json.append("summaryinfo", summary)
        json
    }
}
