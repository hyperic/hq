import org.hyperic.hq.authz.shared.PermissionException

import org.hyperic.hq.hqu.rendit.BaseController

import java.text.DateFormat
import org.hyperic.hq.common.YesOrNo
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.AlertPermissionManager;
import org.hyperic.hq.events.AlertSeverity
import org.hyperic.hq.events.EventConstants
import org.hyperic.hq.events.server.session.AlertDefSortField
import org.hyperic.hq.events.server.session.AlertSortField
import org.hyperic.hq.galerts.server.session.GalertDefSortField
import org.hyperic.hq.galerts.server.session.GalertLogSortField
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.hqu.rendit.util.HQUtil

class AlertController 
	extends BaseController
{
    private final DateFormat df = 
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    private final SEVERITY_MAP = [(AlertSeverity.LOW)    : 'Low',
                                  (AlertSeverity.MEDIUM) : 'Med',
                                  (AlertSeverity.HIGH)   : 'High']
    private getSeverityImg(s) {
        def imgUrl = urlFor(asset:'images') + 
            "/${SEVERITY_MAP[s]}-severity.gif"
        """<img src="${imgUrl}" width="16" height="16" border="0" 
                class="severityIcon">  ${SEVERITY_MAP[s]}"""
    }
            
    private getPriority(params) {
        def minPriority = params.getOne('minPriority', '1')
        def severity = AlertSeverity.findByCode(minPriority.toInteger())
    }        

    boolean logRequests() {
        false
    }    

    private getNow() {
        System.currentTimeMillis()
    }
    
    private final ALERT_TABLE_SCHEMA = [
        getData: {pageInfo, params -> 
            def alertTime = params.getOne('alertTime', "86400000").toLong()
            def show      = params.getOne('show', "all")
            def group     = params.getOne('group', "0")
            alertHelper.findAlerts(getPriority(params), alertTime, now,
                                   show == "inescalation", show == "notfixed",
                                   group != "0" ? group.toInteger() : null,
                                   pageInfo)
        },
        defaultSort: AlertSortField.DATE,
        defaultSortOrder: 0,  // descending
        styleClass: {it.fixed ? null : "alertHighlight"},
        columns: [
            [field:AlertSortField.ACTION_TYPE, width:'3%',
             label:{
				 def canTakeAction
				
				 try {
					Bootstrap.getBean(AlertPermissionManager.class).canFixAcknowledgeAlerts(user, it.alertDefinition.appdefEntityId)
					
					canTakeAction = true
				 } catch (PermissionException e) {
				    canTakeAction = false
				 }
				
			 	 def esc = it.definition.escalation
             	 def pause = (esc == null ? "0" : (esc.pauseAllowed ? esc.maxPauseTime : "0"))
             	 // checkbox id is in the format: {portalName}|{appdefKey}|{alertId}|{maxPauseTime}
             	 def id = "Alerts|" + it.alertDefinition.appdefEntityId.appdefKey + "|" + it.id + "|" + pause
				 def member = (it.ackable ? "ackableAlert" : "fixableAlert")
             	 def box = ((it.fixed || !canTakeAction) ? "" : "<input type='checkbox' name='ealerts' id='" + id + "' class='" + member + "' value='-559038737:" + it.id +"' onclick='MyAlertCenter.toggleAlertButtons(this)' />")}],
            [field:AlertSortField.DATE, width:'8%',
             label:{df.format(it.timestamp)}],
            [field:AlertSortField.DEFINITION, width:'15%',
             label:{linkTo(it.alertDefinition.name, [resource:it]) }],
            [field:AlertSortField.RESOURCE, width:'28%',
             label:{linkTo(it.alertDefinition.resource.name,
                           [resource:it.alertDefinition.resource])}],
            [field:AlertSortField.PLATFORM, width:'28%',
             label:{linkTo(it.alertDefinition.resource.platform.name,
                           [resource:it.alertDefinition.resource.platform])}],
            [field:AlertSortField.FIXED, width:'5%',
             label:{YesOrNo.valueFor(it.fixed).value.capitalize()}],
            [field:AlertSortField.ACKED_BY, width:'7%',
             label:{
				 def canTakeAction
				
				 try {
				     Bootstrap.getBean(AlertPermissionManager.class).canFixAcknowledgeAlerts(user, it.alertDefinition.appdefEntityId)
					
					canTakeAction = true
				 } catch (PermissionException e) {
					canTakeAction = false
				 }
			
			     def esc = it.definition.escalation
             	 def pause = (esc == null ? "0" : (esc.pauseAllowed ? esc.maxPauseTime : "0"))
             	 // checkbox id is in the format: {portalName}|{appdefKey}|{alertId}|{maxPauseTime}
             	 def id = "Alerts|" + it.alertDefinition.appdefEntityId.appdefKey + "|" + it.id + "|" + pause
                 def by = it.acknowledgedBy
                 by == null ? ((it.ackable && canTakeAction) ? "<a href='javascript:MyAlertCenter.acknowledgeAlert(\"" + id + "\")'><img src='/images/icon_ack.gif'></a>" : "") : by.fullName
            }],
            [field:AlertSortField.SEVERITY, width:'6%',
             label:{
                def s = it.alertDefinition.severity
                def imgUrl = urlFor(asset:'images') + 
                    "/${SEVERITY_MAP[s]}-severity.gif"
                """<img src="${imgUrl}" width="16" height="16" border="0" 
                        class="severityIcon"> ${SEVERITY_MAP[s]}"""
                 }
            ],
        ]
    ]
    
    private final GALERT_TABLE_SCHEMA = [
        getData: {pageInfo, params -> 
            def alertTime = params.getOne('alertTime', "86400000").toLong()
            def show      = params.getOne('show', "all")
            def group     = params.getOne('group', "0")
            alertHelper.findGroupAlerts(getPriority(params), alertTime,
                                        now, show == "inescalation",
                                        show == "notfixed",
                                        group != "0" ? group.toInteger() : null,
                                        pageInfo)
        },
        defaultSort: GalertLogSortField.DATE,
        defaultSortOrder: 0,  // descending
        styleClass: {it.fixed ? null : "alertHighlight"},
        columns: [
            [field:GalertLogSortField.ACTION_TYPE, width:'3%',
             label:{
				 def canTakeAction
				
				 try {
				     Bootstrap.getBean(AlertPermissionManager.class).canFixAcknowledgeAlerts(user, it.alertDef.appdefID)
					
					canTakeAction = true
				 } catch (PermissionException e) {
					canTakeAction = false
				 }
			
			     def esc = it.definition.escalation
             	 def pause = (esc == null ? "0" : (esc.pauseAllowed ? esc.maxPauseTime : "0"))
             	 // checkbox id is in the format: {portalName}|{appdefKey}|{alertId}|{maxPauseTime}
             	 def id = "GroupAlerts|" + it.alertDef.appdefID.appdefKey + "|" + it.id + "|" + pause
             	 def member = (it.acknowledgeable ? "ackableAlert" : "fixableAlert")
             	 def box = ((it.fixed || !canTakeAction) ? "" : "<input type='checkbox' name='ealerts' id='" + id + "' class='" + member + "' value='195934910:" + it.id +"' onclick='MyAlertCenter.toggleAlertButtons(this)' />")}],
            [field:GalertLogSortField.DATE, width:'14%',
             label:{df.format(it.timestamp)}],
            [field:GalertLogSortField.DEFINITION, width:'25%',
             label:{linkTo(it.alertDef.name, [resource:it]) }],
            [field:GalertLogSortField.GROUP, width:'35%',
             label:{linkTo(it.alertDef.group.name,
                    [resource:it.alertDef.group])}],
            [field:GalertLogSortField.FIXED, width:'5%',
             label:{YesOrNo.valueFor(it.fixed).value.capitalize()}],
            [field:GalertLogSortField.ACKED_BY, width:'10%',
             label:{
				 def canTakeAction
				
				 try {
				     Bootstrap.getBean(AlertPermissionManager.class).canFixAcknowledgeAlerts(user, it.alertDef.appdefID)
					
					canTakeAction = true
				 } catch (PermissionException e) {
					canTakeAction = false
				 }
				
				 def esc = it.definition.escalation
             	 def pause = (esc == null ? "0" : (esc.pauseAllowed ? esc.maxPauseTime : "0"))
             	 // checkbox id is in the format: {portalName}|{appdefKey}|{alertId}|{maxPauseTime}
             	 def id = "GroupAlerts|" + it.alertDef.appdefID.appdefKey + "|" + it.id + "|" + pause
                 def by = it.acknowledgedBy
                 by == null ? ((it.acknowledgeable && canTakeAction) ? "<a href='javascript:MyAlertCenter.acknowledgeAlert(\"" + id + "\")'><img src='/images/icon_ack.gif'></a>" : "") : by.fullName }],
            [field:GalertLogSortField.SEVERITY, width:'8%',
             label:{
                 def s = it.alertDef.severity
                 def imgUrl = urlFor(asset:'images') + 
                 "/${SEVERITY_MAP[s]}-severity.gif"
                 """<img src="${imgUrl}" width="16" height="16" border="0" 
                         class="severityIcon"> ${SEVERITY_MAP[s]}"""
              }
             ],
         ]
    ]
    
    private final DEF_TABLE_SCHEMA = [
        getData: {pageInfo, params -> 
            def excludeTypes = params.getOne('excludeTypes', 'true').toBoolean()
            alertHelper.findDefinitions(AlertSeverity.LOW, 
                                        getOnlyShowDisabled(params),
                                        excludeTypes, pageInfo) 
        },
        defaultSort: AlertDefSortField.CTIME,
        defaultSortOrder: 0,  // descending
        columns: [
            [field:AlertDefSortField.NAME, width:'13%',
             label:{linkTo(it.name, [resource:it]) }],
            [field:AlertDefSortField.CTIME, width:'13%',
             label:{df.format(it.ctime)}],
            [field:AlertDefSortField.MTIME, width:'13%',
             label:{df.format(it.mtime)}],
            [field:AlertDefSortField.ACTIVE, width:'5%',
             label:{
             	def markUp = "<span style='whitespace:nowrap:'>"
             	 
            	if (it.active && !it.enabled) {
	             	def imgUrl = urlFor(asset:'images') + "/flag_yellow.gif"
	             	
        			markUp += YesOrNo.valueFor(it.active).value.capitalize() + "&nbsp;<img align='absmiddle' src='${imgUrl}' width='16' height='16' border='0' class='severityIcon' title='$localeBundle.ActiveButDisabled'/>"
             	} else {
             		markUp += YesOrNo.valueFor(it.active).value.capitalize()
             	} 
             	
             	return markUp + "</span>"
			}],
            [field:AlertDefSortField.LAST_FIRED, width:'13%',
             label:{
                if (it.lastFired)
                    return linkTo(df.format(it.lastFired),
                                  [resource:it, resourceContext:'listAlerts'])
                else
                    return ''
            }],
            [field:AlertDefSortField.RESOURCE, width:'23%',
             label:{linkTo(it.resource.name,
                           [resource:it.resource])}],
            [field:AlertDefSortField.ESCALATION, width:'13%',
             label:{
                if (it.escalation == null)
                    return ""
                else
                    return linkTo(it.escalation.name, [resource:it.escalation])
            }],
            [field:AlertDefSortField.PRIORITY, width:'8%',
             label:{getSeverityImg(it.severity)}],
        ]
    ]
    
    private final TYPE_DEF_TABLE_SCHEMA = [
        getData: {pageInfo, params -> 
            alertHelper.findTypeBasedDefinitions(getOnlyShowDisabled(params),
                                                 pageInfo)
        },
        defaultSort: AlertDefSortField.NAME,
        defaultSortOrder: 0,  // descending
        columns: [
            [field:AlertDefSortField.NAME, width:'20%',
             label:{linkTo(it.name, [resource:it]) }],
            [field:AlertDefSortField.CTIME, width:'13%',
             label:{df.format(it.ctime)}], 
            [field:AlertDefSortField.MTIME, width:'13%',
             label:{df.format(it.mtime)}], 
            [field:AlertDefSortField.ACTIVE, width:'7%',
             label:{
            	def markUp = "<span style='whitespace:nowrap:'>"
                	 
                markUp += YesOrNo.valueFor(it.active).value.capitalize()

                return markUp + "</span>"
             }],
            [field:[getValue: {localeBundle.ResourceType },
                    description:'resourceType', sortable:false], width:'19%',
             label:{it.resourceType.name}],
             [field:AlertDefSortField.ESCALATION, width:'18%',
              label:{
                 if (it.escalation == null)
                     return ""
                 else
                     return linkTo(it.escalation.name, [resource:it.escalation])
             }],
            [field:AlertDefSortField.PRIORITY, width:'10%',
             label:{getSeverityImg(it.severity)}], 
        ]
    ]
            
    private final GALERT_DEF_TABLE_SCHEMA = [
        getData: {pageInfo, params -> 
            alertHelper.findGroupDefinitions(AlertSeverity.LOW, 
                                             getOnlyShowDisabled(params),
                                             pageInfo)
        },
        defaultSort: GalertDefSortField.NAME,
        defaultSortOrder: 0,  // descending
        columns: [
            [field:GalertDefSortField.NAME, width:'17%',
             label:{linkTo(it.name, [resource:it]) }],
            [field:GalertDefSortField.CTIME, width:'13%',
             label:{df.format(it.ctime)}],
            [field:GalertDefSortField.MTIME, width:'13%',
             label:{df.format(it.mtime)}],
            [field:GalertDefSortField.ACTIVE, width:'7%',
             label:{YesOrNo.valueFor(it.enabled).value.capitalize()}],
            [field:GalertDefSortField.LAST_FIRED, width:'13%',
             label:{
                 if (it.lastFired)
                     return linkTo(df.format(it.lastFired),
                                   [resource:it, resourceContext:'listAlerts'])
                 else
                     return ''
             }],
            [field:GalertDefSortField.GROUP, width:'16%',
             label:{linkTo(it.group.name, [resource:it.group])}],
            [field:GalertDefSortField.ESCALATION, width:'14%',
             label:{linkTo(it.escalation.name, [resource:it.escalation])}],
            [field:GalertDefSortField.SEVERITY, width:'7%',
             label:{getSeverityImg(it.severity)}], 
        ]
    ]

    private getLastDays() {
        def res = []

        for (i in 1..7) {
            def val
            if (i == 1) {
                val = "$localeBundle.Day"
            } else if (i == 7) {
                val = "$localeBundle.Week"
            } else {
                val = "$i $localeBundle.Days"
            }
            res << [code:i * 24 * 60 * 60 * 1000, value:val]    
        }
        res << [code:System.currentTimeMillis(), value:localeBundle.AllTime]
        res
    }

    private getGroups() {
        def res = [[code:0, value:localeBundle.AllGroups]]
        resourceHelper.findViewableGroups().each { group ->
            res << [code: group.id, value: group.name]
        }
        res
    }

    def AlertController() {
        setTemplate('standard')
    }
    
    def index(params) {
    	render(locals:[alertSchema     : ALERT_TABLE_SCHEMA, 
    	               galertSchema    : GALERT_TABLE_SCHEMA,
    	               defSchema       : DEF_TABLE_SCHEMA,
    	               typeDefSchema   : TYPE_DEF_TABLE_SCHEMA,
    	               galertDefSchema : GALERT_DEF_TABLE_SCHEMA,
    	               severities      : AlertSeverity.all,
    	               lastDays        : lastDays,
    	               superUser       : user.isSuperUser(), 
    	               isEE            : HQUtil.isEnterpriseEdition(),
                       groups          : groups])
    }
    
    private getOnlyShowDisabled(params) { 
        def disabledOnly = params.getOne('onlyShowDisabled', 'false').toBoolean()
    
        if (disabledOnly == false) {
            return null
        } else {
            return !disabledOnly
        }
    }
    
    def data(params) {
        def json = DojoUtil.processTableRequest(ALERT_TABLE_SCHEMA, params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
    
    def groupData(params) {
        def json = DojoUtil.processTableRequest(GALERT_TABLE_SCHEMA, params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
    
    def defData(params) {
        def json = DojoUtil.processTableRequest(DEF_TABLE_SCHEMA, params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }

    def typeDefData(params) {
        def json = DojoUtil.processTableRequest(TYPE_DEF_TABLE_SCHEMA, params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
    
    def galertDefData(params) {
        def json = DojoUtil.processTableRequest(GALERT_DEF_TABLE_SCHEMA, params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
    
}
